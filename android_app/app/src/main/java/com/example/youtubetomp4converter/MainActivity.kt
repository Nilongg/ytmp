package com.example.youtubetomp4converter

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.youtubetomp4converter.ui.theme.YouTubeToMp4ConverterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YouTubeToMp4ConverterTheme {
                MainScreenWithViewModel()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithViewModel(mainViewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val youtubeUrl = mainViewModel.youtubeUrl
    val statusText = mainViewModel.statusText
    val isLoading = mainViewModel.isLoading
    val downloadProgress = mainViewModel.downloadProgress

    var hasStoragePermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Scoped storage on Q+
            mutableStateOf(true) // For Q+, WRITE_EXTERNAL_STORAGE is not needed for app specific dir or MediaStore
                                 // However, our current code saves to public Downloads with file paths.
                                 // requestLegacyExternalStorage=true helps on Q.
                                 // On R+ (API 30+), this direct path saving to Downloads is problematic without MANAGE_EXTERNAL_STORAGE
                                 // For this exercise, we rely on requestLegacyExternalStorage for API 29.
                                 // For API < 29, we need WRITE_EXTERNAL_STORAGE.
            mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

        } else { // For API < Q (29)
            mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasStoragePermission = isGranted
            if (isGranted) {
                mainViewModel.startDownload() // Proceed with download if permission granted
            } else {
                mainViewModel.updateStatusText("Storage permission denied. Cannot save video.")
            }
        }
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "YouTube to MP4 Converter",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = youtubeUrl,
                onValueChange = { mainViewModel.updateYoutubeUrl(it) },
                label = { Text("YouTube Video URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = statusText.contains("URL", ignoreCase = true) && (statusText.startsWith("Error:") || statusText.startsWith("Please enter a valid"))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // On Android 10 (API 29) with requestLegacyExternalStorage="true",
                        // WRITE_EXTERNAL_STORAGE is implicitly granted for legacy ops, or not strictly needed for app-specific dirs.
                        // However, our current code writes to public Downloads.
                        // On Android 11+ (API 30+), direct file path to public Downloads is an issue.
                        // For simplicity, we'll assume legacy storage works on 29, and for <29 we check explicitly.
                        // A more robust solution for 30+ would use MediaStore or SAF.
                        // We proceed with download, relying on manifest flags and current file path logic.
                        // If targetSdk were 30+, this path would likely fail without MANAGE_EXTERNAL_STORAGE.
                        mainViewModel.startDownload()
                    } else { // Needs explicit permission for API < 29 (Android 6.0 to 9.0)
                        if (hasStoragePermission) {
                            mainViewModel.startDownload()
                        } else {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading && youtubeUrl.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DOWNLOADING...")
                } else {
                    Text("Download & Convert to MP4")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                LinearProgressIndicator(
                    progress = downloadProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    YouTubeToMp4ConverterTheme {
        MainScreenWithViewModel(mainViewModel = MainViewModel(Application()))
    }
}
