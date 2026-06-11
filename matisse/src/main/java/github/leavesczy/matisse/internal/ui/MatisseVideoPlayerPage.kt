package github.leavesczy.matisse.internal.ui

import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import github.leavesczy.matisse.R
import github.leavesczy.matisse.internal.logic.MatisseVideoPlayerPageViewState

@Composable
internal fun MatisseVideoPlayerPage(pageViewState: MatisseVideoPlayerPageViewState) {
    AnimatedVisibility(
        modifier = Modifier
            .fillMaxSize(),
        visible = pageViewState.visible,
        enter = slideInHorizontally(
            animationSpec = tween(
                durationMillis = 350,
                easing = FastOutSlowInEasing
            ),
            initialOffsetX = { it }
        ),
        exit = slideOutHorizontally(
            animationSpec = tween(
                durationMillis = 350,
                easing = FastOutSlowInEasing
            ),
            targetOffsetX = { it }
        )
    ) {
        MatisseVideoPlayerPageContent(pageViewState = pageViewState)
    }
}

@Composable
private fun MatisseVideoPlayerPageContent(pageViewState: MatisseVideoPlayerPageViewState) {
    BackHandler(
        enabled = pageViewState.visible,
        onBack = pageViewState.onDismissRequest
    )
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.matisse_video_player_background_color))
            .clickableNoRipple(onClick = {}),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            modifier = Modifier
                .weight(weight = 1f)
        )
        MatisseVideoPlayer(
            modifier = Modifier
                .weight(weight = 16f)
                .fillMaxHeight(),
            videoUri = pageViewState.videoUri
        )
        Spacer(
            modifier = Modifier
                .weight(weight = 1f)
        )
    }
}

@Composable
private fun MatisseVideoPlayer(
    modifier: Modifier,
    videoUri: Uri
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackState = remember {
        VideoPlaybackState()
    }
    DisposableEffect(key1 = lifecycleOwner) {
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                val position = playbackState.resumePositionMs
                if (position > 0) {
                    playbackState.videoView?.seekTo(position)
                    playbackState.resumePositionMs = -1
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                val videoView = playbackState.videoView
                if (videoView != null) {
                    playbackState.resumePositionMs = videoView.currentPosition
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            VideoView(context).apply {
                val onPreparedListener = MediaPlayer.OnPreparedListener {
                    val controller = MediaController(context)
                    controller.setAnchorView(this)
                    controller.setMediaPlayer(this)
                    setMediaController(controller)
                }
                setOnPreparedListener(onPreparedListener)
                setVideoURI(videoUri)
                start()
                playbackState.videoView = this
            }
        },
        onRelease = { videoView ->
            videoView.setOnPreparedListener(null)
            videoView.suspend()
            playbackState.videoView = null
        }
    )
}

private class VideoPlaybackState {
    var videoView: VideoView? = null
    var resumePositionMs: Int = -1
}
