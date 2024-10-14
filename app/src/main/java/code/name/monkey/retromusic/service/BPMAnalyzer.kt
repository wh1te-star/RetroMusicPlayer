package code.name.monkey.retromusic.service

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.transition.Visibility
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.filters.LowPassFS
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.onsets.ComplexOnsetDetector
import be.tarsos.dsp.onsets.OnsetHandler
import be.tarsos.dsp.onsets.PercussionOnsetDetector
import be.tarsos.dsp.writer.WriterProcessor
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.db.SongAnalysisDao
import code.name.monkey.retromusic.db.SongAnalysisEntity
import code.name.monkey.retromusic.util.logD
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.RandomAccessFile
import java.text.DecimalFormat
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

object BPMAnalyzer : KoinComponent {
    private const val maxThreads = 10
    private const val reliableRange = 15.0
    private const val possibleMinBPM = 60.0
    private const val possibleMaxBPM = 240.0

    private val songAnalysisDao: SongAnalysisDao by inject<SongAnalysisDao>()

    private var parentJob = Job().apply { complete() }
    private val semaphore = Semaphore(maxThreads)

    private var callback: AnalysisProcessCallback? = null
    private val songJobs: MutableMap<Long, Job> = mutableMapOf()

    fun setCallback(callback: AnalysisProcessCallback) {
        this.callback = callback
    }

    fun getAllBPMValues(): List<SongAnalysisEntity>? {
        return runBlocking {
            songAnalysisDao.getAll()
        }
    }

    fun getAnalyzedValue(songId: Long, columnName: String): Double? {
        return runBlocking {
            songAnalysisDao.getColumn(songId, columnName)
        }
    }

    fun analyzeBPM(context: Context, songId: Long, uri: Uri, parentScope: CoroutineScope, force: Boolean = false): Job {
        val processJob = Job(parentScope.coroutineContext[Job])
        val processScope = CoroutineScope(Dispatchers.IO + processJob)

        songJobs[songId] = processJob

        processScope.launch {
            semaphore.withPermit {
                withContext(Dispatchers.Main) {
                    callback?.onSingleProcessStart(songId)
                }

                val complexOnsetTimes = mutableListOf<Double>()
                val percussionOnsetTimes = mutableListOf<Double>()
                val audioDispatcher: AudioDispatcher = AudioDispatcherFactory.fromPipe(
                    context, uri, 0.0, -1.0, 44100, 1024, 512
                )

                fun fixBPM(bpm: Double, manualBPM: Double?): Double? {
                    var fixedBPM = bpm
                    if (manualBPM == null) {
                        if (0.0 < fixedBPM && fixedBPM < possibleMinBPM) {
                            while (fixedBPM < possibleMinBPM) fixedBPM *= 2.0
                        } else if (fixedBPM > possibleMaxBPM) {
                            while (fixedBPM > possibleMaxBPM) fixedBPM /= 2.0
                        }
                    }else{
                        var manualFixedBPM = fixedBPM
                        val reliableRangeMin = manualBPM - reliableRange / 2
                        val reliableRangeMax = manualBPM + reliableRange / 2
                        if (0.0 < manualFixedBPM && manualFixedBPM < reliableRangeMin) {
                            while (manualFixedBPM < reliableRangeMin) manualFixedBPM *= 2.0
                        } else if (manualFixedBPM > reliableRangeMax) {
                            while (manualFixedBPM > reliableRangeMax) manualFixedBPM /= 2.0
                        }
                        if (manualFixedBPM in reliableRangeMin..reliableRangeMax) {
                            fixedBPM = manualFixedBPM
                        } else {
                            return null
                        }
                    }
                    return fixedBPM
                }

                val lowPassFilter = LowPassFS(300.0f, 44100f)
                audioDispatcher.addAudioProcessor(lowPassFilter)

                val complexHandler = OnsetHandler { time, salience ->
                    if (salience > 0.5) {
                        complexOnsetTimes.add(time)
                    }
                }
                val percussionHandler = OnsetHandler { time, salience ->
                    if (salience > 0.5) {
                        percussionOnsetTimes.add(time)
                    }
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

                        val manualBPM = getAnalyzedValue(songId, "manualBPM")

                        val bpmValues = mutableListOf<Double>()

                        for (i in 0 until complexOnsetTimes.size - 1) {
                            val bpm = fixBPM(60.0 / (complexOnsetTimes[i + 1] - complexOnsetTimes[i]), manualBPM)
                            if (bpm != null) bpmValues.add(bpm)
                            logD("Complex Onset BPM: $bpm")
                        }

                        for (i in 0 until percussionOnsetTimes.size - 1) {
                            val bpm = fixBPM(60.0 / (percussionOnsetTimes[i + 1] - percussionOnsetTimes[i]), manualBPM)
                            if (bpm != null) bpmValues.add(bpm)
                            logD("Percussion Onset BPM: $bpm")
                        }

                        val modeBPM = bpmValues.map { (it * 10).roundToInt() / 10.0 }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 0.0

                        logD("Audio processing finished. modeBPM: $modeBPM")

                        processScope.launch(Dispatchers.IO) {
                            songAnalysisDao.updateColumn(songId, "bpm", modeBPM)
                        }

                        processJob.complete()
                        songJobs.remove(songId)
                        completion.complete(Unit)
                    }
                })

                logD("Starting dispatcher")
                withContext(Dispatchers.IO) {
                    audioDispatcher.run()
                }

                completion.await()

                withContext(Dispatchers.Main) {
                    callback?.onSingleProcessFinish(songId)
                }
            }
        }

        return processJob
    }

    suspend fun analyzeAll(context: Context, songIds: List<Long>, uris: List<Uri>) {
        parentJob = Job()
        val parentScope = CoroutineScope(Dispatchers.IO + parentJob)

        val jobs = mutableListOf<Job>()
        for (i in songIds.indices) {
            val isAnalyzed = runBlocking {
                songAnalysisDao.getBPM(songIds[i])
            }
            if (isAnalyzed == null) {
                jobs.add(analyzeBPM(context, songIds[i], uris[i], parentScope))
            }
        }

        jobs.joinAll()

        withContext(Dispatchers.Main) {
            callback?.onAllProcessFinish()
            parentJob.complete()
        }
    }

    suspend fun deleteAllBPMs() {
        CoroutineScope(Dispatchers.IO).launch {
            songAnalysisDao.deleteAll()
        }.join()
    }

    fun isRunning(songId: Long = 0L): Boolean {
        return if (songId == 0L) {
            parentJob.isActive
        } else {
            songJobs[songId]?.isActive == true
        }
    }

    fun stopAnalysis(songId: Long = 0L) {
        if (songId == 0L) {
            parentJob.cancel()
            songJobs.forEach {
                it.value.cancel()
                callback?.onSingleProcessFinish(it.key)
            }
            songJobs.clear()
            CoroutineScope(Dispatchers.IO).launch {
                parentJob.join()
                withContext(Dispatchers.Main) {
                    callback?.onAllProcessFinish()
                    parentJob.complete()
                }
            }
        } else {
            songJobs[songId]?.let { job ->
                job.cancel()
                CoroutineScope(Dispatchers.IO).launch {
                    job.join()
                    withContext(Dispatchers.Main) {
                        callback?.onSingleProcessFinish(songId)
                    }
                    songJobs.remove(songId)
                }
            }
        }
    }

    fun manualBPMTap(context: Context, songId: Long, uri: Uri) {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_manual_bpm, null)

        var robinIndex = 0
        var sumBPM = 0.0
        var previousTapTime = Long.MAX_VALUE
        val movingAverageSize = 100
        var validSize = 1
        val previousBPMs = MutableList(movingAverageSize) { 0.0 }

        val temporalBPMTextView: TextView = dialogView.findViewById(R.id.temporalBPMValue)
        val tapButton: FloatingActionButton = dialogView.findViewById(R.id.bpmTapButton)
        val bpmSignal: BPMSignal = dialogView.findViewById(R.id.bpmSignal)

        bpmSignal.setOnClickListener {
            bpmSignal.switchBeats()
        }

        tapButton.setOnClickListener {
            val currentTapTime = System.currentTimeMillis()
            val timeDifference = currentTapTime - previousTapTime
            if (timeDifference > 0) {
                bpmSignal.startTimer(timeDifference)

                val currentBPM = 60.0 * 1000.0 / timeDifference
                sumBPM += currentBPM
                sumBPM -= previousBPMs[robinIndex]
                val averageBPM = sumBPM / validSize

                val formattedBPM = DecimalFormat("0.0").format(averageBPM)
                val bpmText = "$formattedBPM BPM"
                val spannableString = SpannableString(bpmText)
                val start = bpmText.indexOf("BPM")
                spannableString.setSpan(RelativeSizeSpan(0.3f), start, bpmText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                temporalBPMTextView.text = spannableString

                previousTapTime = currentTapTime
                previousBPMs[robinIndex] = currentBPM
                robinIndex = (robinIndex + 1) % movingAverageSize
                if (validSize < movingAverageSize) {
                    validSize++
                }
            } else {
                previousTapTime = currentTapTime
            }
        }

        val dialogBuilder = AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("OK") { dialog, _ ->
                val averageBPM = sumBPM / validSize
                runBlocking {
                    songAnalysisDao.updateColumn(songId, "manualBPM", averageBPM)
                }
                analyzeBPM(context, songId, uri, CoroutineScope(Dispatchers.IO))
                dialog.dismiss()
            }
            .setNegativeButton(R.string.action_cancel) { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    fun exportFrequencyFilteredAudio(context: Context, inputUri: Uri, outputFilePath: String) {
        val outputFile = File(outputFilePath)
        outputFile.parentFile?.let { parentDir ->
            if (!parentDir.exists()) {
                parentDir.mkdirs()
            }
        }
        val audioDispatcher: AudioDispatcher = AudioDispatcherFactory.fromPipe(
            context, inputUri, 0.0, -1.0, 44100, 1024, 512
        )
        val lowPassFilter = LowPassFS(300.0f, 44100f)
        val randomAccessFile = RandomAccessFile(outputFile, "rw")
        val audioFormat = TarsosDSPAudioFormat(44100f, 16, 2, true, false)
        val writerProcessor = WriterProcessor(audioFormat, randomAccessFile)
        audioDispatcher.addAudioProcessor(lowPassFilter)
        audioDispatcher.addAudioProcessor(writerProcessor)
        audioDispatcher.run()
    }
}

interface AnalysisProcessCallback {
    fun onSingleProcessStart(id: Long)
    fun onSingleProcessFinish(id: Long)
    fun onAllProcessFinish()
}