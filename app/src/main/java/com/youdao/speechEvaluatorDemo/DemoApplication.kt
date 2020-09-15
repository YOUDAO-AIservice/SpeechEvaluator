package com.youdao.speechEvaluatorDemo

import android.app.Application
import com.tencent.mmkv.MMKV
import com.youdao.sdk.app.YouDaoApplication


class DemoApplication : Application() {


    companion object {
        private var youApplication: DemoApplication? = null

        fun getInstance(): DemoApplication? {
            return youApplication
        }
    }

    override fun onCreate() {
        super.onCreate()
        MMKV.initialize(this)
//        YouDaoApplication.init(this, "") //创建应用，每个应用都会有一个Appid，绑定对应的翻译服务实例，即可使用
        youApplication = this
        val rootDir = MMKV.initialize(this)
    }


}