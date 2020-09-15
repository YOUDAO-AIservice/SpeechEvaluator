package com.youdao.speechEvaluatorDemo.media

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Message
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.util.*
import kotlin.experimental.or

class ExtAudioRecorder(private val configuration: AuditRecorderConfiguration) {

    // 不压缩将使用这个进行录音
    private var audioRecorder: AudioRecord? = null

    // 压缩将使用这进行录音
    private var mediaRecorder: MediaRecorder? = null

    // 当前的振幅 (只有在未压缩的模式下)
    private var cAmplitude = 0

    // 录音状态
    private var state: State? = null

    // 文件 (只有在未压缩的模式下)
    private var randomAccessWriter: RandomAccessFile? = null

    private var bufferSize = 0

    // 录音 通知周期(只有在未压缩的模式下)
    private var framePeriod = 0

    // 输出的字节(只有在未压缩的模式下)
    private var buffer: ByteArray? = null


    private var samples: Short = 0
    private var channels: Short = 0

    // 写入头文件的字节数(只有在未压缩的模式下)
    // after stop() is called, this size is written to the header/data chunk in
    // the wave file
    private var payloadSize = 0

    //录音的开始时间
    private var startTime: Long = 0

    private var filePath: String? = null

    private val updateListener: AudioRecord.OnRecordPositionUpdateListener =
        object : AudioRecord.OnRecordPositionUpdateListener {
            override fun onMarkerReached(recorder: AudioRecord?) {
                // NOT USED
            }

            override fun onPeriodicNotification(recorder: AudioRecord?) {
                buffer?.let {
                    audioRecorder?.read(it, 0, it.size)
                    try {
                        randomAccessWriter?.write(it)
                        payloadSize += it.size
//                        if (samples.toInt() == 16) {
//                            for (i in 0 until it.size / 2) { // 16bit sample size
//                                val curSample: Short = getShort(it[i * 2], it[i * 2 + 1])
//                                if (curSample > cAmplitude) { // Check amplitude
//                                    cAmplitude = curSample.toInt()
//                                }
//                            }
//                        } else { // 8bit sample size
//                            for (i in it.indices) {
//                                if (it[i] > cAmplitude) { // Check amplitude
//                                    cAmplitude = it[i].toInt()
//                                }
//                            }
//                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Log.e(
                            ExtAudioRecorder::class.java.name,
                            "Error occurred in updateListener, recording is aborted"
                        )
                    }
                }
            }

        }

    init {
        if (configuration.uncompressed) {
            init(
                configuration.uncompressed,
                configuration.source,
                configuration.rate,
                configuration.channelConfig,
                configuration.format
            )
        } else {
            var rateIndex = 0
            do {
                init(
                    configuration.uncompressed,
                    configuration.source,
                    SAMPLE_RATES[rateIndex],
                    configuration.channelConfig,
                    configuration.format
                )
            } while ((++rateIndex < SAMPLE_RATES.size) and (state != State.INITIALIZING))

        }
    }

    /**
     * 设置输出的文件路径
     *
     * @param argPath 文件路径
     */
    fun setOutputFile(argPath: String) {
        try {
            if (state == State.INITIALIZING) {
                filePath = argPath
                if (!configuration.uncompressed) {
                    mediaRecorder?.setOutputFile(filePath)
                }
            }
        } catch (e: java.lang.Exception) {
            if (e.message != null) {
                Log.e(ExtAudioRecorder::class.java.name, e.message)
            } else {
                Log.e(
                    ExtAudioRecorder::class.java.name,
                    "Unknown error occurred while setting output path"
                )
            }
            state = State.ERROR
            fireFailEvent(-1, e)
        }
    }

    /**
     * 准备录音的录音机, 如果 state 不是 [State.INITIALIZING] 或文件路径为null
     * 将设置 state 为 [State.ERROR]。如果发生异常不会抛出，而是设置 state 为
     * [State.ERROR]
     */
    fun prepare() {
        try {
            if (state == State.INITIALIZING) {
                if (configuration.uncompressed) {
                    if ((audioRecorder!!.state == AudioRecord.STATE_INITIALIZED) and (filePath != null)) {
                        // 写文件头
                        randomAccessWriter = RandomAccessFile(filePath, "rw")
                        //设置文件长度为0，为了防止这个file以存在
                        randomAccessWriter?.let {
                            it.setLength(0)
                            it.writeBytes("RIFF")
                            //不知道文件最后的大小，所以设置0
                            it.writeInt(0)
                            it.writeBytes("WAVE")
                            it.writeBytes("fmt ")
                            // Sub-chunk
                            // size,
                            // 16
                            // for
                            // PCM
                            it.writeInt(Integer.reverseBytes(16))
                            // AudioFormat, 1 为 PCM
                            it.writeShort(
                                java.lang.Short.reverseBytes(1.toShort()).toInt()
                            )
                            // 数字为声道, 1 为 mono, 2 为 stereo
                            it.writeShort(
                                java.lang.Short.reverseBytes(channels).toInt()
                            )
                            // 采样率
                            it.writeInt(Integer.reverseBytes(configuration.rate))
                            // 采样率, SampleRate*NumberOfChannels*BitsPerSample/8
                            it.writeInt(Integer.reverseBytes(configuration.rate * samples * channels / 8))
                            it.writeShort(
                                java.lang.Short.reverseBytes((channels * samples / 8).toShort())
                                    .toInt()
                            )
                            // Block
                            // align,
                            // NumberOfChannels*BitsPerSample/8
                            it.writeShort(
                                java.lang.Short.reverseBytes(samples).toInt()
                            ) // Bits per sample
                            it.writeBytes("data")
                            it.writeInt(0) // Data chunk size not
                            // known yet, write 0
                        }
                        buffer = ByteArray(framePeriod * samples / 8 * channels)
                        state = State.READY
                    } else {
                        Log.e(
                            ExtAudioRecorder::class.java.name,
                            "prepare() method called on uninitialized recorder"
                        )
                        state = State.ERROR
                        fireFailEvent(-1, null)
                    }
                } else {
                    mediaRecorder?.prepare()
                    state = State.READY
                }
            } else {
                Log.e(ExtAudioRecorder::class.java.name, "prepare() method called on illegal state")
                release()
                state = State.ERROR
                fireFailEvent(-1, null)
            }
        } catch (e: java.lang.Exception) {
            if (e.message != null) {
                Log.e(ExtAudioRecorder::class.java.name, e.message)
            } else {
                Log.e(ExtAudioRecorder::class.java.name, "Unknown error occured in prepare()")
            }
            state = State.ERROR
            fireFailEvent(-1, e)
        }
    }

    /**
     * 开始录音，并设置 state 为 [State.RECORDING]。在调用这个方法前必须调用 [ExtAudioRecorder.prepare] 方法
     */
    fun start() {
        if (state == State.READY) {
            if (configuration.uncompressed) {
                payloadSize = 0
                audioRecorder?.let {
                    it.startRecording()
                    buffer?.let { buffer ->
                        it.read(buffer, 0, buffer.size)
                    }
                }
            } else {
                mediaRecorder?.start()
            }
            state = State.RECORDING
            startTime = Date().time
            startGetMaxAmplitudeThread()
        } else {
            Log.e(ExtAudioRecorder::class.java.name, "start() called on illegal state")
            state = State.ERROR
            fireFailEvent(-1, null)
        }
    }

    /**
     * 停止录音，并设置 state 为 [State.STOPPED]。如果要继续使用，则需要调用 [.reset] 方法
     *
     * @return 录音的时间
     */
    fun stop(): Int {
        return if (state == State.RECORDING) {
            if (configuration.uncompressed) {
                audioRecorder?.stop()
                try {
                    randomAccessWriter?.let {
                        it.seek(4) // Write size to RIFF header
                        it.writeInt(Integer.reverseBytes(36 + payloadSize))
                        it.seek(40) // Write size to Subchunk2Size
                        // field
                        it.writeInt(Integer.reverseBytes(payloadSize))
                        it.close()
                    }
                } catch (e: IOException) {
                    Log.e(
                        ExtAudioRecorder::class.java.name,
                        "I/O exception occurred while closing output file"
                    )
                    state = State.ERROR
                }
            } else {
                try {
                    mediaRecorder?.stop()
                } catch (e: java.lang.Exception) {
                }
            }
            state = State.STOPPED
            val file = File(filePath)
            if (file.exists() && file.isFile) {
                if (file.length() == 0L) {
                    file.delete()
                    0
                } else {
                    (Date().time - startTime).toInt() / 1000
                }
            } else {
                0
            }
        } else {
            Log.e(ExtAudioRecorder::class.java.name, "stop() called on illegal state")
            state = State.ERROR
            fireFailEvent(-1, null)
            0
        }
    }

    /**
     * 重置录音，并设置 state 为 [State.INITIALIZING]，如果当前状态为 [State.RECORDING]，将会停止录音。
     * 这个方法不会抛出异常，但是会设置状态为 [State.ERROR]
     */
    fun reset() {
        try {
            if (state != State.ERROR) {
                release()
                filePath = null // Reset file path
                cAmplitude = 0 // Reset amplitude
                if (configuration.uncompressed) {
                    audioRecorder = AudioRecord(
                        configuration.source, configuration.rate,
                        channels + 1, configuration.format, bufferSize
                    )
                    audioRecorder?.let {
                        it.setRecordPositionUpdateListener(updateListener)
                        it.positionNotificationPeriod = framePeriod
                    }
                } else {
                    mediaRecorder = MediaRecorder()
                    mediaRecorder?.let {
                        it.setAudioSource(MediaRecorder.AudioSource.MIC)
                        it.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                        it.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    }
                }
                state = State.INITIALIZING
            }
        } catch (e: java.lang.Exception) {
            Log.e(ExtAudioRecorder::class.java.name, e.message)
            state = State.ERROR
            fireFailEvent(-1, e)
        }
    }

    /**
     * 释放与这个类相关的资源，和移除不必要的文件，在必要的时候
     */
    fun release() {
        if (state == State.RECORDING) {
            stop()
        } else {
            if ((state == State.READY) and configuration.uncompressed) {
                try {
                    randomAccessWriter?.close() // 删除准备文件
                } catch (e: IOException) {
                    Log.e(
                        ExtAudioRecorder::class.java.name,
                        "I/O exception occurred while closing output file"
                    )
                }
                File(filePath).delete()
            }
        }
        if (configuration.uncompressed) {
            audioRecorder?.release()
        } else {
            mediaRecorder?.release()
        }
    }

    private fun startGetMaxAmplitudeThread() {
        if (configuration.handler != null) {
            Thread(Runnable {
                while (true) {
                    if (state == State.RECORDING) {
                        val var1 = Message()
                        var1.what = getMaxAmplitude() * 13 / 32767
                        configuration?.handler?.sendMessage(var1)
                        SystemClock.sleep(100L)
                        continue
                    }
                    return@Runnable
                }
            }).start()
        }
    }

    /**
     * Returns the largest amplitude sampled since the last call to this method.
     *
     * @return returns the largest amplitude since the last call, or 0 when not
     * in recording state.
     */
    private fun getMaxAmplitude(): Int {
        return if (state == State.RECORDING) {
            if (configuration.uncompressed) {
                val result = cAmplitude
                cAmplitude = 0
                result
            } else {
                try {
                    mediaRecorder?.let { return it.maxAmplitude }
                    return 0
                } catch (e: IllegalStateException) {
                    0
                }
            }
        } else {
            0
        }
    }

    /** Converts a byte[2] to a short, in LITTLE_ENDIAN format  */
    private fun getShort(argB1: Byte, argB2: Byte): Short { // 似乎没用到？
        return (argB1 or ((argB2.toInt() shl 8).toByte())) as Short
    }

    enum class State {
        /**
         * 录音初始化
         */
        INITIALIZING,

        /**
         * 已准备好录音
         */
        READY,

        /**
         * 录音中
         */
        RECORDING,

        /**
         * 录音生了错误
         */
        ERROR,

        /**
         * 停止录音
         */
        STOPPED,
    }

    interface RecorderListener {
        fun recordFailed(type: Int)
    }

    private fun init(
        unCompressed: Boolean,
        audioSource: Int,
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int
    ) {
        try {
            if (unCompressed) { // recording_uncompressed
                samples = if (audioFormat == AudioFormat.ENCODING_PCM_16BIT) {
                    16
                } else {
                    8
                }

                channels = if (channelConfig == AudioFormat.CHANNEL_CONFIGURATION_MONO) {
                    1
                } else {
                    2
                }
                framePeriod = sampleRate * configuration.timerInterval / 1000
                bufferSize = framePeriod * 2 * samples * channels / 8
                if (bufferSize < AudioRecord.getMinBufferSize(
                        sampleRate,
                        channelConfig,
                        audioFormat
                    )
                ) {
                    // Check to make sure
                    // buffer size is not
                    // smaller than the
                    // smallest allowed one
                    bufferSize =
                        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                    // Set frame period and timer interval accordingly
                    framePeriod = bufferSize / (2 * samples * channels / 8)
                    Log.w(
                        ExtAudioRecorder::class.java.name,
                        "Increasing buffer size to $bufferSize"
                    )
                }
                audioRecorder =
                    AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize)
                audioRecorder?.let {
                    if (it.state != AudioRecord.STATE_INITIALIZED) throw Exception(
                        "AudioRecord initialization failed"
                    )
                    it.setRecordPositionUpdateListener(updateListener)
                    it.positionNotificationPeriod = framePeriod
                }
            } else {
                // RECORDING_COMPRESSED
                mediaRecorder = MediaRecorder()
                mediaRecorder?.let {
                    it.setAudioSource(MediaRecorder.AudioSource.MIC)
                    it.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    it.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                }
            }
            cAmplitude = 0
            filePath = null
            state = State.INITIALIZING
        } catch (e: Exception) {
            fireFailEvent(0, e)
            if (e.message != null) {
                Log.e(ExtAudioRecorder::class.java.name, e.message)
            } else {
                Log.e(
                    ExtAudioRecorder::class.java.name,
                    "Unknown error occurred while initializing recording"
                )
            }
            state = State.ERROR
        }
    }

    private fun fireFailEvent(failType: Int, failCause: Throwable?) {
        configuration.listener?.let {
            val r = Runnable { it.recordFailed(failType) }
            r.run()
        }
    }
}