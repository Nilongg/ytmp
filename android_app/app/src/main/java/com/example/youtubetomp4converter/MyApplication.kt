package com.example.youtubetomp4converter

import android.app.Application
import android.util.Log
import android.widget.Toast
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@MyApplication)
                // You can optionally update youtube-dl here. It's a network call.
                // YoutubeDL.getInstance().updateYoutubeDL(this@MyApplication, YoutubeDL.UpdateChannel.STABLE)
                Log.i("MyApplication", "YoutubeDL initialized successfully.")
                withContext(Dispatchers.Main) {
                    // Optional: Show a toast or update UI if needed
                    // Toast.makeText(this@MyApplication, "youtube-dl initialized", Toast.LENGTH_SHORT).show()
                }
            } catch (e: YoutubeDLException) {
                Log.e("MyApplication", "Failed to initialize YoutubeDL", e)
                withContext(Dispatchers.Main) {
                     Toast.makeText(this@MyApplication, "Failed to initialize youtube-dl: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("MyApplication", "An unexpected error occurred during YoutubeDL initialization", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MyApplication, "Unexpected error initializing youtube-dl", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
