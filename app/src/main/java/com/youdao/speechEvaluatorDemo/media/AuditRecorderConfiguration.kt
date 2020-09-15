package com.youdao.speechEvaluatorDemo.media

import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Handler
import com.youdao.speechEvaluatorDemo.media.ExtAudioRecorder.RecorderListener

val SAMPLE_RATES = intArrayOf(44100, 22050, 11025, 8000)
val RECORDING_UNCOMPRESSED = true
val RECORDING_COMPRESSED = false

class AuditRecorderConfiguration private constructor(builder: Builder) {
    var listener: RecorderListener? = null
        private set
    var uncompressed = false
        private set
    var timerInterval = 0
        private set
    var rate = 0
        private set
    var source = 0
        private set
    var channelConfig = 0
        private set
    var format = 0
        private set
    var handler: Handler? = null
        private set

    init {
        listener = builder.listener
        uncompressed = builder.uncompressed
        timerInterval = builder.timerInterval
        rate = builder.rate
        source = builder.source
        format = builder.format
        handler = builder.handler
        channelConfig = builder.channelConfig
    }

    class Builder {
        var listener: RecorderListener? = null
        var uncompressed = false
        var timerInterval = 120
        var rate: Int = SAMPLE_RATES[3]
        var source = MediaRecorder.AudioSource.MIC
        var channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO
        var format = AudioFormat.ENCODING_PCM_16BIT
        var handler: Handler? = null

        /**
         * 声道设置
         */
        fun channelConfig(channelConfig: Int): Builder {
            this.channelConfig = channelConfig
            return this
        }

        /**
         * 录音失败的监听
         */
        fun recorderListener(listener: RecorderListener?): Builder {
            this.listener = listener
            return this
        }

        /** 是否压缩录音  */
        fun uncompressed(uncompressed: Boolean): Builder {
            this.uncompressed = uncompressed
            return this
        }

        /** 周期的时间间隔  */
        fun timerInterval(timeInterval: Int): Builder {
            timerInterval = timeInterval
            return this
        }

        /** 采样率  */
        fun rate(rate: Int): Builder {
            this.rate = rate
            return this
        }

        /** 音频源  */
        fun source(source: Int): Builder {
            this.source = source
            return this
        }

        /** 编码制式和采样大小  */
        fun format(format: Int): Builder {
            this.format = format
            return this
        }

        /** 返回what是振幅值 1-13   */
        fun handler(handler: Handler?): Builder {
            this.handler = handler
            return this
        }

        fun builder(): AuditRecorderConfiguration {
            return AuditRecorderConfiguration(this)
        }

    }
}