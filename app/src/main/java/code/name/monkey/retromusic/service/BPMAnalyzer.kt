package code.name.monkey.retromusic.service

import android.content.Context
import android.net.Uri
import android.widget.Toast
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import code.name.monkey.retromusic.db.SongAnalysisDao
import code.name.monkey.retromusic.db.SongAnalysisEntity
import code.name.monkey.retromusic.extensions.uri
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.util.logD
import kotlinx.coroutines.CompletableDeferred
import java.text.DecimalFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.Semaphore

class BPMAnalyzer private constructor(private val context: Context) : KoinComponent {

    companion object {
        @Volatile
        private var INSTANCE: BPMAnalyzer? = null

        fun getInstance(context: Context): BPMAnalyzer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BPMAnalyzer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val songAnalysisDao: SongAnalysisDao by inject<SongAnalysisDao>()

    private val analysisDispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()

    fun analyzeBPM(songId: Long, uri: Uri, callback: AnalysisProcessCallback) = runBlocking {
        withContext(Dispatchers.Main) {
            callback.onProcessStart(songId)
        }

        val complexOnsetTimes = mutableListOf<Double>()
        val percussionOnsetTimes = mutableListOf<Double>()
        val dispatcher: AudioDispatcher = AudioDispatcherFactory.fromPipe(
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
        dispatcher.addAudioProcessor(complexOnsetDetector)
        complexOnsetDetector.setHandler(complexHandler)

        dispatcher.addAudioProcessor(
            PercussionOnsetDetector(44100.0f, 1024, percussionHandler, 95.0, 10.0)
        )

        val completion = CompletableDeferred<Unit>()

        dispatcher.addAudioProcessor(object : AudioProcessor {
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
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Analysis finished.\nMedianBPM: ${DecimalFormat("#.0").format(medianBPM)}", Toast.LENGTH_LONG).show()
                }

                CoroutineScope(Dispatchers.IO).launch {
                    val songAnalysis = SongAnalysisEntity(songId = songId, bpm = medianBPM)
                    songAnalysisDao.addOrUpdateBpm(songAnalysis)
                }

                completion.complete(Unit)
            }
        })

        logD("Starting dispatcher")
        CoroutineScope(Dispatchers.IO).launch {
            dispatcher.run()
        }

        completion.await()

        withContext(Dispatchers.Main) {
            callback.onProcessFinish(songId)
        }
    }

    fun analyzeAll(songIds: List<Long>, uris: List<Uri>, callback: AnalysisProcessCallback) {
        if (songIds.size != uris.size) {
            return
        }

        val dispatcher = Executors.newFixedThreadPool(3).asCoroutineDispatcher()
        val scope = CoroutineScope(dispatcher)

            for (i in songIds.indices) {
                scope.launch {
                    withContext(dispatcher){
                        analyzeBPM(songIds[i], uris[i], callback)
                    }
                }
            }
        dispatcher.close()
    }
}

interface AnalysisProcessCallback {
    fun onProcessStart(id: Long)
    fun onProcessFinish(id: Long)
}