package com.youdao.speechEvaluatorDemo.media

import android.media.MediaPlayer
import java.lang.Exception

class PlayMgr private constructor() {
    val PLAY_LOG = "TranslatePlay ："

    companion object {
        val mgr: PlayMgr = PlayMgrHandler.audioMgr
        val mediaPlayer: MediaPlayer = PlayMgrHandler.mediaPlayer
    }

    @Synchronized
    fun startMediaPlayer(audioPath: String) {
        mediaPlayer.release()
        try {
            mediaPlayer.setDataSource(audioPath)
            mediaPlayer.prepare() // 进行缓冲
            mediaPlayer.start()
        } catch (e: Exception) {

        }
    }

    private object PlayMgrHandler {
        val audioMgr = PlayMgr()
        val mediaPlayer = MediaPlayer()
    }

}