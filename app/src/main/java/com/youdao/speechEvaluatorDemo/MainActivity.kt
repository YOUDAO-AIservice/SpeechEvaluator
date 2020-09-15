package com.youdao.speechEvaluatorDemo

import android.Manifest.permission
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.tencent.mmkv.MMKV
import com.youdao.sdk.app.YouDaoApplication
import com.youdao.speechEvaluatorDemo.utils.isPermissionGranted
import com.youdao.speechEvaluatorDemo.utils.requestPermission
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    private val kv: MMKV = MMKV.defaultMMKV()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        evaluator_btn.setOnClickListener { startEvaluatorActivity() }
        go_web.setOnClickListener { goWebActivity() }
        val userKey: String? = kv.decodeString(KEY)
        userKey?.let {
            key_et.setText(it)
        }
        requestPermission()
    }

    private fun requestPermission(): Boolean {
        if (!isPermissionGranted(this@MainActivity, permission.RECORD_AUDIO)) {
            requestPermission(
                this@MainActivity,
                arrayOf(permission.RECORD_AUDIO),
                0
            )
            return false
        }
        return true
    }

    private fun startEvaluatorActivity() {
        val inputKey = key_et.editableText.toString()
        if (inputKey.isNotEmpty()) {
            if (requestPermission()) {
                kv.encode(KEY, inputKey)
                YouDaoApplication.init(this,inputKey)
                startActivity(Intent(this, EvaluatorActivity::class.java))
            }
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setMessage("请填写key")
            builder.setNegativeButton("确定") { dialog, _ ->
                dialog.dismiss()
            }
            builder.setPositiveButton("获取key") { dialog, _ ->
                dialog.dismiss()
                goWebActivity()
            }
            builder.create().show()
        }
    }

    private fun goWebActivity() {
        val uri: Uri = Uri.parse("https://ai.youdao.com/newuser-promotion.s")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }
}