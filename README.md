# SpeechEvaluator
[sdk下载地址](https://ai.youdao.com/download.s)
[使用文档](https://ai.youdao.com/DOCSIRMA/html/%E8%AF%AD%E9%9F%B3%E5%90%88%E6%88%90TTS/Android_SDK%E6%96%87%E6%A1%A3/%E8%AF%AD%E9%9F%B3%E5%90%88%E6%88%90%E6%9C%8D%E5%8A%A1/%E8%AF%AD%E9%9F%B3%E5%90%88%E6%88%90%E6%9C%8D%E5%8A%A1-Android_SDK%E6%96%87%E6%A1%A3.html)
# 从智云获取appkey
#### 登录智云官网
https://ai.youdao.com/newuser-promotion.s
#### 获取appkey方式：

根据提示输入应用信息创建应用，例如：
* 应用包名：com.youdao.speechEvaluatorDemo
* 应用签名信息：db931329685bcec29bf64578d1f48ca7
应用创建成功后应用id即为appkey

# 引入
#### build.gradle配置
```
android {
    sourceSets.main.jniLibs.srcDirs = ['libs']
    ...
}

dependencies {
    compile files('libs/YoudaoBase_v2.0.0.jar')
    compile files('libs/YoudaoTTS_v1.0.0.jar')
}
```
#### 添加权限
```
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```
#### 混淆
```
-ignorewarnings
-libraryjars libs/YoudaoBase_v2.0.0.jar
-libraryjars libs/YoudaoTTS_v1.0.0.jar
-keep class com.youdao.sdk.ydtranslate.** { ;}
-keep class com.youdao.speechsynthesizer.online.* { *;}
```
#### 初始化
```
//appkey即应用ID
YouDaoApplication.init(this, appkey);
```


