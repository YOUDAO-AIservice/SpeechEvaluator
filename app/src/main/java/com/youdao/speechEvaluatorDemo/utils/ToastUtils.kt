package com.youdao.speechEvaluatorDemo.utils

import android.content.Context
import android.widget.Toast
import com.youdao.speechEvaluatorDemo.DemoApplication


object ToastUtils {
    private var toast: Toast? = null
    fun show(msg: String) {
        DemoApplication.getInstance()?.let {
            showText(it, msg, Toast.LENGTH_SHORT)
        }
    }

    fun show(stringId: Int) {
        DemoApplication.getInstance()?.let {
            showText(it, stringId, Toast.LENGTH_SHORT)
        }
    }

    fun show(context: Context, msg: String) {
        showText(context, msg, Toast.LENGTH_SHORT)
    }

    fun show(context: Context, stringId: Int) {
        showText(context, stringId, Toast.LENGTH_SHORT)
    }

    private fun showText(
        context: Context,
        msg: String,
        length: Int
    ) {
        if (toast == null) {
            toast = Toast.makeText(context, msg, length)
        } else {
            toast!!.setText(msg)
        }
        toast!!.show()
    }

    private fun showText(
        context: Context,
        stringId: Int,
        length: Int
    ) {
        if (toast == null) {
            toast = Toast.makeText(context, stringId, length)
        } else {
            toast!!.setText(stringId)
        }
        toast!!.show()
    }
}
