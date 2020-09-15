package com.youdao.speechEvaluatorDemo.media

import android.media.MediaPlayer
import android.os.AsyncTask
import android.util.Log
import com.youdao.sdk.common.YouDaoLog
import com.youdao.sdk.common.util.AsyncTasks

/**
 * @author lukun
 */
const val PLAY_LOG = "TranslatePlay ："

interface SuccessListener {
    fun success()
    fun playOver()
}

fun startPlayVoice(url: String, listener: SuccessListener) {
    try {
        AsyncTasks.safeExecuteOnExecutor(PlayTask(url, listener))
    } catch (e: java.lang.Exception) {
        YouDaoLog.e(PLAY_LOG + "AudioMgr startPlayVoice", e);
        Log.d("AudioMgr", "fail to fetch data: ", e);
    }
}

class PlayTask constructor(val url: String, private val listener: SuccessListener) :
    AsyncTask<Void, Void, Void>() {
    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        if (isCancelled) {
            PlayMgr.mediaPlayer.stop()
            onCancelled()
            return
        }
    }

    override fun doInBackground(vararg params: Void?): Void? {
        val mediaPlayer = PlayMgr.mediaPlayer
        mediaPlayer.setOnCompletionListener {
            listener?.let {
                it.playOver()
            }
        }
        mediaPlayer.setOnPreparedListener { mp ->
            YouDaoLog.e(PLAY_LOG + "AudioMgr playTask on prepareOk")
            mp.start()
            YouDaoLog.e(PLAY_LOG + "AudioMgr playTask play onStart")
        }

        mediaPlayer.setOnErrorListener(MediaPlayer.OnErrorListener { _, what, extra ->
            YouDaoLog.e(PLAY_LOG + "AudioMgr playTask onError " + what + "---" + extra)
            return@OnErrorListener false
        })
        try {

        } catch (e: Exception) {
            e.printStackTrace()
            YouDaoLog.e(PLAY_LOG + "AudioMgr playTask prepare error = " + e.toString())
        }
        return null
    }

}

