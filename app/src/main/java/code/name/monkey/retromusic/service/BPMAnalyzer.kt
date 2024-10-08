package code.name.monkey.retromusic.service

import android.content.Context
import android.net.Uri
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import code.name.monkey.retromusic.db.SongAnalysisDao
import code.name.monkey.retromusic.db.SongAnalysisEntity
import code.name.monkey.retromusic.util.logD
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.Executors
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.coroutines.cancellation.CancellationException

class BPMAnalyzer private constructor(private val context: Context, private val callback: AnalysisProcessCallback) : KoinComponent {

    companion object {
        @Volatile
        private var INSTANCE: BPMAnalyzer? = null

        fun getInstance(context: Context, callback: AnalysisProcessCallback): BPMAnalyzer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BPMAnalyzer(context.applicationContext, callback).also { INSTANCE = it }
            }
        }
    }

    private val songAnalysisDao: SongAnalysisDao by inject<SongAnalysisDao>()

    private var parentJob = Job().apply { complete() }
    private val semaphore = Semaphore(3)

    fun analyzeBPM(songId: Long, uri: Uri, parentScope: CoroutineScope): Job {
        val processJob = Job(parentScope.coroutineContext[Job])
        val processScope = CoroutineScope(Dispatchers.IO + processJob)

        processScope.launch {
            semaphore.withPermit {
                withContext(Dispatchers.Main) {
                    callback.onSingleProcessStart(songId)
                }

                val complexOnsetTimes = mutableListOf<Double>()
                val percussionOnsetTimes = mutableListOf<Double>()
                val audioDispatcher: AudioDispatcher = AudioDispatcherFactory.fromPipe(
                    context, uri, 0.0, 320.0, 44100, 1024, 512
                )

                fun fixBPM(bpm: Double): Double {
                    val minBPM = 60.0
                    val maxBPM = 200.0
                    var fixedBPM = bpm
                    if (bpm > 0.0 && bpm < minBPM) {
                        while (fixedBPM < minBPM) fixedBPM *= 2.0
                    } else if (bpm > maxBPM) {
                        while (fixedBPM > maxBPM) fixedBPM /= 2.0
                    }
                    return fixedBPM
                }

                val complexHandler = OnsetHandler { time, salience ->
                    complexOnsetTimes.add(time)
                }
                val percussionHandler = OnsetHandler { time, salience ->
                    percussionOnsetTimes.add(time)
                }

                val complexOnsetDetector = ComplexOnsetDetector(1024)
                audioDispatcher.addAudioProcessor(complexOnsetDetector)
                complexOnsetDetector.setHandler(complexHandler)

                audioDispatcher.addAudioProcessor(
                    PercussionOnsetDetector(44100.0f, 1024, percussionHandler, 95.0, 10.0)
                )

                val completion = CompletableDeferred<Unit>()

                audioDispatcher.addAudioProcessor(object : AudioProcessor {
                    override fun process(audioEvent: AudioEvent): Boolean {
                        return true
                    }

                    override fun processingFinished() {
                        complexOnsetTimes.sort()
                        percussionOnsetTimes.sort()

                        val bpmValues = mutableListOf<Double>()

                        for (i in 0 until complexOnsetTimes.size - 1) {
                            val bpm = fixBPM(60.0 / (complexOnsetTimes[i + 1] - complexOnsetTimes[i]))
                            bpmValues.add(bpm)
                            logD("Complex Onset BPM: $bpm")
                        }

                        for (i in 0 until percussionOnsetTimes.size - 1) {
                            val bpm = fixBPM(60.0 / (percussionOnsetTimes[i + 1] - percussionOnsetTimes[i]))
                            bpmValues.add(bpm)
                            logD("Percussion Onset BPM: $bpm")
                        }

                        val medianBPM = if (bpmValues.size % 2 == 0) {
                            val middleIndex = bpmValues.size / 2
                            (bpmValues[middleIndex - 1] + bpmValues[middleIndex]) / 2.0
                        } else {
                            bpmValues[bpmValues.size / 2]
                        }

                        logD("Audio processing finished. medianBPM: $medianBPM")

                        processScope.launch(Dispatchers.IO) {
                            val songAnalysis = SongAnalysisEntity(songId = songId, bpm = medianBPM)
                            songAnalysisDao.addOrUpdateBpm(songAnalysis)
                        }

                        processJob.complete()
                        completion.complete(Unit)
                    }
                })

                logD("Starting dispatcher")
                withContext(Dispatchers.IO) {
                    audioDispatcher.run()
                }

                completion.await()

                withContext(Dispatchers.Main) {
                    callback.onSingleProcessFinish(songId)
                }
            }
        }

        return processJob
    }

    suspend fun analyzeAll(songIds: List<Long>, uris: List<Uri>) {
        parentJob = Job()
        val parentScope = CoroutineScope(Dispatchers.IO + parentJob)

        val jobs = mutableListOf<Job>()
        for (i in songIds.indices) {
            val isAnalyzed = runBlocking {
                songAnalysisDao.getBpmBySongId(songIds[i])
            }
            if (isAnalyzed == null) {
                jobs.add(analyzeBPM(songIds[i], uris[i], parentScope))
            }
        }

        jobs.joinAll()

        withContext(Dispatchers.Main) {
            callback.onAllProcessFinish()
            parentJob.complete()
        }
    }

    suspend fun deleteAllBPMs() {
        CoroutineScope(Dispatchers.IO).launch {
            songAnalysisDao.deleteAll()
        }.join()
    }

    fun isRunning(): Boolean {
        return parentJob.isActive
    }

    fun stopAllAnalysis() {
        parentJob.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                CoroutineScope(Dispatchers.Main).launch {
                    callback.onAllProcessFinish()
                    parentJob.complete()
                }
            }
        }
        parentJob.cancel()
        CoroutineScope(Dispatchers.IO).launch {
            parentJob.join()
        }
    }
}

interface AnalysisProcessCallback {
    fun onSingleProcessStart(id: Long)
    fun onSingleProcessFinish(id: Long)
    fun onAllProcessFinish()
}