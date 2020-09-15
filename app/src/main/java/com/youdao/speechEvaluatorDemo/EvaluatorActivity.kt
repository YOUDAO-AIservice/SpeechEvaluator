package com.youdao.speechEvaluatorDemo

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.os.Handler
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.youdao.sdk.app.EncryptHelper
import com.youdao.sdk.common.Constants
import com.youdao.sdk.common.YouDaoLog
import com.youdao.speechEvaluator.*
import com.youdao.speechEvaluatorDemo.media.*
import com.youdao.speechEvaluatorDemo.utils.ToastUtils
import com.youdao.speechEvaluatorDemo.utils.getContent
import com.youdao.speechEvaluatorDemo.utils.isFastDoubleClick
import kotlinx.android.synthetic.main.view_evaluator_layout.*
import java.io.File
import java.io.IOException

class EvaluatorActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = "SpeechEvaluatorView" + hashCode()
    private val TIMEOUT = 100000
    private val RATE = Constants.RATE_16000
    private val CHANNEL = 1
    private val FORMAT = SpEvaMediaType.TYPE_WAV
    private val mSpEvaParameters: SpEvaParameters =
        SpEvaParameters.Builder().channel(CHANNEL).language(SpEvaLanguage.ENGLISH).rate(RATE)
            .timeout(TIMEOUT).format(FORMAT)
            .build()
    private val mEvaluateText: String? = null
    private var mAudioFile: File? = null
    private var mRecorder: ExtAudioRecorder? = null
    private val mHandler = Handler()
    private var mIsRecord = false
    private var mIsPlaying = false

    private val listener: ExtAudioRecorder.RecorderListener =
        object : ExtAudioRecorder.RecorderListener {
            override fun recordFailed(type: Int) {
                if (type == 0) {
                    Toast.makeText(this@EvaluatorActivity, "录音失败，可能是没有给权限", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(this@EvaluatorActivity, "发生了未知错误", Toast.LENGTH_SHORT).show()
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_evaluator_layout)
        audio_layout.setOnClickListener(this)
        record_layout.setOnClickListener(this)
    }

    private fun startRecord() {
        YouDaoLog.w("$TAG startRecord")
        mIsRecord = true
        try {
            mAudioFile = File.createTempFile(
                "record_",
                ".$FORMAT"
            )
            val configuration: AuditRecorderConfiguration = AuditRecorderConfiguration.Builder()
                .recorderListener(listener)
                .handler(mHandler)
                .rate(RATE)
                .uncompressed(true)
                .builder()
            mRecorder = ExtAudioRecorder(configuration)
            mRecorder?.let {
                mAudioFile?.let { audioFile ->
                    it.setOutputFile(audioFile.absolutePath)
                    it.prepare()
                    it.start()
                }
            }

        } catch (e: IOException) {
            YouDaoLog.e("$TAG startRecord", e)
        }
    }

    private fun stopRecord() {
        YouDaoLog.w("$TAG stopRecord")
        try {
            val time: Int? = mRecorder?.stop()
            if (time != null) {
                if (time > 0) {
                    if (mAudioFile != null) {
                        Thread { evaluation() }.start()
                    }
                }
            }
            mRecorder?.release()
        } catch (e: Exception) {
            YouDaoLog.e("$TAG stopRecord", e)
        }
        mIsRecord = false
    }

    private fun evaluation() {
        YouDaoLog.w("$TAG evaluation")
        val bases64: String? = getBase64AudioData()
        if (bases64 == null) {
            ToastUtils.show("获取录音数据出现异常")
            return
        }
        SpeechEvaluate.getInstance(mSpEvaParameters)
            .evaluate(bases64, word_tv.text.toString(), object : SpEvaListener {
                override fun onError(errorCode: Int) {
                    val errorString = SpEvaErrorCode.getErrorString(errorCode)
                    YouDaoLog.e(errorString)
                    mHandler.post { ToastUtils.show(errorString) }
                }

                override fun onResult(result: SpEvaResult, input: String) {
                    mHandler.post { updateResultView(result) }
                }
            })
    }

    /**
     * 更新页面
     */
    private fun updateResultView(result: SpEvaResult) {
        result_container.visibility = View.VISIBLE
        score_right_tv.text = result.pronunciation.toString()
        score_complete_tv.text = result.integrity.toString()
        score_smooth_tv.text = result.fluency.toString()
        speed_tv.text = result.speed.toString()
        audio_time_tv.text = "[" + result.start + " " + result.end + "]"
        score_all_tv.text = result.overAll.toString()
        if (result.words != null) {
            if (result.words.size == 1) {
                own_pronunciation_container.visibility = View.VISIBLE
                standard_pronunciation_container.visibility = View.VISIBLE
                val word = result.words[0]
                updatePhonemeView(word)
            } else {
                own_pronunciation_container.visibility = View.INVISIBLE
                standard_pronunciation_container.visibility = View.INVISIBLE
                updateSentenceView(result.words)
            }
        }
    }

    /**
     * 如果是单词，则设置音标，标准音标和自己的发音音标
     */
    private fun updatePhonemeView(word: SpEvaWord?) {
        if (word != null) {
            val standardPronunciation = StringBuilder()
            standardPronunciation.append("[")
            val ownPronunciation = SpannableStringBuilder()
            ownPronunciation.append("[")
            var start: Int
            val phonemes = word.phonemes
            for (phoneme in phonemes) {
                start = ownPronunciation.length
                standardPronunciation.append(phoneme.phoneme)
                if (phoneme.isJudge) {
                    ownPronunciation.append(phoneme.phoneme)
                } else {
                    ownPronunciation.append(phoneme.calibration)
                    ownPronunciation.setSpan(
                        ForegroundColorSpan(resources.getColor(R.color.color_ff6583)),
                        start,
                        ownPronunciation.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            standardPronunciation.append("]")
            ownPronunciation.append("]")
            standard_pronunciation_tv.text = standardPronunciation.toString()
            own_pronunciation_tv.text = ownPronunciation
        }
    }

    /*
     * 如果是句子，没有显示音标，只是给句子中的单词标颜色，小于60红色，60-80深灰色，大于80绿色
     * */
    private fun updateSentenceView(words: List<SpEvaWord>) {
        val sentenceStringBuilder = SpannableStringBuilder()
        var start: Int
        for (word in words) {
            start = sentenceStringBuilder.length
            sentenceStringBuilder.append(word.word)
            val pronunciation = word.pronunciation
            when {
                pronunciation < 60 -> {
                    sentenceStringBuilder.setSpan(
                        ForegroundColorSpan(resources.getColor(R.color.color_ff6583)),
                        start,
                        sentenceStringBuilder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                pronunciation < 80 -> {
                }
                else -> {
                    sentenceStringBuilder.setSpan(
                        ForegroundColorSpan(resources.getColor(R.color.color_00cca2)),
                        start,
                        sentenceStringBuilder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            sentenceStringBuilder.append(" ")
        }
//        word_tv.text = sentenceStringBuilder
    }

    private fun getBase64AudioData(): String? {
        var result: String? = null
        try {
            val data: ByteArray? = getContent(mAudioFile?.absolutePath)
            result = EncryptHelper.getBase64(data)
        } catch (e: java.lang.Exception) {
            YouDaoLog.e("SpeechEvaluatorActivity getBase64AudioData", e)
        }
        return result
    }

    @Synchronized
    fun playVoice(speakUrl: String) {
        YouDaoLog.e("$PLAY_LOG$TAG click to playVoice Url = $speakUrl")
        if (!TextUtils.isEmpty(speakUrl)) {
            startPlayAudioAnim()
            ToastUtils.show("正在发音")
            startPlayVoice(speakUrl, object : SuccessListener {
                override fun success() {
                    mIsPlaying = false
                    YouDaoLog.e(PLAY_LOG + TAG + "playVoice success")
                }

                override fun playOver() {
                    YouDaoLog.e(PLAY_LOG + TAG + "playVoice playOver")
                    mIsPlaying = false
                    stopPlayAudioAnim()
                }
            })
        } else {
            ToastUtils.show("请先录制")
            mIsPlaying = false
        }
    }

    private fun startPlayAudioAnim() {
        val animationDrawable = audio_iv.drawable as AnimationDrawable
        animationDrawable.start()
    }

    private fun stopPlayAudioAnim() {
        val animationDrawable = audio_iv.drawable as AnimationDrawable
        animationDrawable.stop()
    }


    override fun onClick(v: View?) {
        if (isFastDoubleClick()) {
            return
        }
        when (v?.id) {
            R.id.audio_layout -> {
                if (mIsPlaying) {
                    ToastUtils.show("请稍等，正在播放录音")
                } else {
                    mIsPlaying = true
                    playVoice(mAudioFile?.absolutePath ?: "")
                }
            }
            R.id.record_layout -> {
                if (!mIsRecord) {
                    record_tv.text = "正在录音"
                    startRecord()
                } else {
                    record_tv.text = "点击录音"
                    audio_iv.isEnabled = true
                    stopRecord()
                }
            }
        }
    }
}