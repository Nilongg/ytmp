package com.example.youtubetomp4converter

import android.app.Application
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.DownloadProgressCallback
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var youtubeUrl by mutableStateOf("")
        private set

    var statusText by mutableStateOf("Enter a YouTube URL and click 'Download'.")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var downloadProgress by mutableStateOf(0f)
        private set

    fun updateYoutubeUrl(newUrl: String) {
        youtubeUrl = newUrl
        if (newUrl.isNotBlank() && statusText.startsWith("Please enter a valid")) {
            statusText = "Ready to download."
        }
    }

    fun updateStatusText(newStatus: String) {
        statusText = newStatus
        // Potentially clear loading/progress if status indicates a final state like permission denial
        if (newStatus.contains("permission denied", ignoreCase = true)) {
            isLoading = false
            downloadProgress = 0f
        }
    }

    fun startDownload() {
        if (youtubeUrl.isBlank() || !isValidYoutubeUrl(youtubeUrl)) {
            statusText = "Please enter a valid YouTube URL (e.g., https://www.youtube.com/watch?v=...)"
            return
        }

        isLoading = true
        downloadProgress = 0f
        statusText = "Initializing download for: $youtubeUrl"

        viewModelScope.launch {
            val initialStatusMessage = downloadVideo(youtubeUrl)
            // If downloadVideo encounters an immediate error (e.g., directory creation fails),
            // it will return an error message. Otherwise, the callback handles status updates.
            if (!isLoading && initialStatusMessage.startsWith("Error:")) {
                statusText = initialStatusMessage
            } else if (isLoading) { // If still loading, it means process started.
                statusText = initialStatusMessage // e.g., "Download initiated..."
            }
        }
    }

    private fun isValidYoutubeUrl(url: String): Boolean {
        // Basic check, can be improved with more robust regex
        return url.contains("youtube.com/") || url.contains("youtu.be/")
    }

    private suspend fun downloadVideo(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val youtubeDLDir = getAppSpecificDownloadDir()
                if (!youtubeDLDir.exists() && !youtubeDLDir.mkdirs()) {
                    Log.e("MainViewModel", "Failed to create download directory: $youtubeDLDir")
                    isLoading = false // Critical error, stop loading
                    return@withContext "Error: Could not create download directory."
                }

                val request = YoutubeDLRequest(url)
                // Prefer MP4 format, fallback to best video + best audio and let ffmpeg handle muxing.
                // yt-dlp (used by youtube-dl-android) default is often "bv*+ba/b"
                // Explicitly asking for mp4 container where possible:
                request.addOption("-f", "bv[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/bv*+ba/b")
                request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s - %(id)s.%(ext)s")
                request.addOption("--no-mtime") // Prevents setting modification time from server
                // request.addOption("--write-thumbnail") // Optionally download thumbnail
                // request.addOption("--sponsorblock-remove", "all") // Example for removing sponsor segments

                Log.i("MainViewModel", "Download destination: ${youtubeDLDir.absolutePath}")
                Log.i("MainViewModel", "Executing command: ${request.buildCommand().joinToString(" ")}")

                val processId = "ytDownload:${System.currentTimeMillis()}"
                // This call is blocking for setup but the download itself is async via callback
                YoutubeDL.getInstance().execute(request, processId, callback)

                // This message is shown if execute() itself doesn't throw an immediate exception.
                // The actual download progress/completion/error will be handled by the callback.
                "Download process initiated. Monitoring progress..."

            } catch (e: YoutubeDLException) {
                Log.e("MainViewModel", "YoutubeDLException during download setup or execution", e)
                isLoading = false
                "Download Error (YT): ${e.message?.substringBefore('\n')}" // Get first line of error
            } catch (e: InterruptedException) {
                Log.w("MainViewModel", "Download interrupted", e)
                isLoading = false
                "Download was interrupted."
            }
            catch (e: Exception) {
                Log.e("MainViewModel", "Generic error during download setup or execution", e)
                isLoading = false
                "Download Error: ${e.message?.substringBefore('\n')}"
            }
        }
    }

    private val callback = object : DownloadProgressCallback {
        override fun onProgressUpdate(progress: Float, etaInSeconds: Long, line: String?) {
            downloadProgress = progress.coerceIn(0f, 100f)
            // Try to get the most relevant part of the log line for display
            val relevantLine = line?.lines()?.lastOrNull { it.isNotBlank() && (it.contains("[download]") || it.contains("[ffmpeg]")) }?.trim() ?: line?.trim() ?: "Processing..."
            statusText = "Progress: ${String.format("%.1f", downloadProgress)}% (ETA: ${etaInSeconds}s)\n${relevantLine.take(150)}"
            Log.d("MainViewModel", "Download Progress: $progress%, ETA: $etaInSeconds s, Line: $line")
        }

        override fun onDownloadComplete(file: File, id: String?) {
            statusText = "Download Complete! Saved to:\n${file.absolutePath}"
            downloadProgress = 100f
            isLoading = false
            Log.i("MainViewModel", "Download complete for ID '$id': ${file.absolutePath}")
        }

        override fun onDownloadError(error: Throwable, id: String?) {
            val errorMessage = error.message?.substringBefore('\n') ?: "Unknown download error"
            statusText = "Download Error for ID '$id': $errorMessage"
            isLoading = false
            Log.e("MainViewModel", "Download error for ID '$id': ${error.message}", error)
        }
    }

    private fun getAppSpecificDownloadDir(): File {
        // Saving to public Downloads directory for user visibility.
        // This requires WRITE_EXTERNAL_STORAGE permission on API <= 28.
        // On API 29+, if not using requestLegacyExternalStorage, MediaStore API is preferred.
        // Since we have requestLegacyExternalStorage="true" and targetSdk < 29 for WRITE_EXTERNAL_STORAGE,
        // this should work for broader compatibility for now.
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDir = File(downloadsDir, "YouTubeToMp4Converter")
        if (!appDir.exists()) {
            val created = appDir.mkdirs()
            if (!created) {
                Log.w("MainViewModel", "Could not create app download directory: $appDir (using downloadsDir directly)")
                // Fallback to root of Downloads if sub-folder creation fails
                if(downloadsDir.exists() || downloadsDir.mkdirs()){
                    return downloadsDir
                }
            }
        }
        return appDir
    }
}
