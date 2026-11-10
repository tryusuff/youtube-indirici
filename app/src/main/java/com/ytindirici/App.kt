package com.ytindirici

import android.app.Application
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {
    var initOk = false
    var initError: String? = null

    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
            initOk = true
        } catch (e: Exception) {
            initError = e.message
        }
    }
}
