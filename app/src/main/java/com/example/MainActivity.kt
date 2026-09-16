package com.example

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.VideoItem
import com.example.ui.VTMainScreen
import com.example.ui.viewmodel.VTViewModel
import com.example.ui.viewmodel.VTViewModelFactory

class MainActivity : ComponentActivity() {

    private var viewModelInstance: VTViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as VTPlayerApplication

        setContent {
            val viewModel: VTViewModel = viewModel(
                factory = VTViewModelFactory(application)
            )
            viewModelInstance = viewModel

            VTMainScreen(viewModel = viewModel)
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val vm = viewModelInstance ?: return
        val activeVideo = vm.activePlayingVideo.value
        val isPlaying = vm.playerManager.playerState.value.isPlaying
        if (activeVideo != null && isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    val aspectRatio = Rational(16, 9)
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(aspectRatio)
                        .build()
                    enterPictureInPictureMode(params)
                } catch (_: Exception) {}
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModelInstance?.playerManager?.setPipActive(isInPictureInPictureMode)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (Intent.ACTION_VIEW == intent.action) {
            val uri: Uri? = intent.data
            if (uri != null) {
                val videoItem = VideoItem(
                    id = System.currentTimeMillis(),
                    title = uri.lastPathSegment ?: "External Video",
                    uriString = uri.toString(),
                    duration = 0L,
                    folderName = "External"
                )
                viewModelInstance?.playVideo(videoItem, listOf(videoItem))
            }
        }
    }
}

