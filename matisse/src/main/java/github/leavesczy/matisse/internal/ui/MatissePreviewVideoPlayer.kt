package github.leavesczy.matisse.internal.ui

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import github.leavesczy.matisse.internal.MatisseLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 预览页内嵌的视频播放器。
 *
 * 使用 TextureView 而非 SurfaceView，使画面能跟随 Pager 翻页时的 graphicsLayer 缩放与透明度变化。
 * 点击画面切换暂停 / 继续；播放完成或出错时回调 [onPlaybackEnded]，由调用方恢复为封面状态。
 * 宿主进入 onStop（界面不可见）时暂停正在播放的视频，回到 onStart 后自动继续；
 * 分屏等仍可见但失去焦点的场景不暂停。播放前申请音频焦点，失去焦点时暂停。
 */
@Composable
internal fun MatissePreviewVideoPlayer(
    modifier: Modifier,
    videoUri: Uri,
    onPlaybackEnded: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnPlaybackEnded by rememberUpdatedState(newValue = onPlaybackEnded)
    val playerState = remember(key1 = videoUri) {
        PreviewVideoPlayerState(
            context = context.applicationContext,
            videoUri = videoUri,
            onPlaybackEnded = {
                currentOnPlaybackEnded()
            }
        )
    }
    DisposableEffect(key1 = lifecycleOwner, key2 = playerState) {
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                playerState.onHostStart()
            }

            override fun onStop(owner: LifecycleOwner) {
                playerState.onHostStop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer = lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer = lifecycleObserver)
        }
    }
    Box(
        modifier = modifier
            .clickableNoRipple(onClick = playerState::togglePlayback),
        contentAlignment = Alignment.Center
    ) {
        val videoAspectRatio = playerState.videoAspectRatio
        AndroidView(
            modifier = Modifier
                .then(
                    other = if (videoAspectRatio > 0f) {
                        Modifier
                            .aspectRatio(ratio = videoAspectRatio)
                    } else {
                        Modifier
                            .fillMaxSize()
                    }
                )
                // 首帧渲染前保持透明，让下层封面继续可见，避免黑屏闪烁
                .alpha(alpha = if (playerState.isFirstFrameRendered) 1f else 0f),
            factory = { viewContext ->
                TextureView(viewContext).apply {
                    surfaceTextureListener = playerState
                }
            },
            onRelease = { textureView ->
                textureView.surfaceTextureListener = null
                playerState.release()
            }
        )
        if (playerState.isPaused) {
            MatisseVideoIcon(
                modifier = Modifier
                    .size(size = 50.dp)
            )
        }
    }
}

@Stable
private class PreviewVideoPlayerState(
    private val context: Context,
    private val videoUri: Uri,
    private val onPlaybackEnded: () -> Unit
) : TextureView.SurfaceTextureListener {

    var videoAspectRatio by mutableFloatStateOf(value = 0f)
        private set

    var isFirstFrameRendered by mutableStateOf(value = false)
        private set

    var isPaused by mutableStateOf(value = false)
        private set

    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
        .build()

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pause()
        }
    }

    /** API 26+ 为 AudioFocusRequest；以 Any 持有避免低版本加载该类。 */
    private var audioFocusRequest: Any? = null

    private val playerScope = CoroutineScope(context = SupervisorJob() + Dispatchers.Main.immediate)

    private var prepareJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null

    private var surface: Surface? = null

    private var isPrepared = false

    private var isHostStarted = false

    private var resumeWhenHostStarted = false

    private var isReleased = false

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        if (mediaPlayer != null || isReleased) {
            return
        }
        val playerSurface = Surface(surfaceTexture)
        surface = playerSurface
        val player = MediaPlayer()
        mediaPlayer = player
        player.setAudioAttributes(audioAttributes)
        player.setSurface(playerSurface)
        player.setOnPreparedListener {
            isPrepared = true
            if (isHostStarted) {
                start()
            } else {
                resumeWhenHostStarted = true
            }
        }
        player.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
            if (videoWidth > 0 && videoHeight > 0) {
                videoAspectRatio = videoWidth.toFloat() / videoHeight
            }
        }
        player.setOnInfoListener { _, what, _ ->
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                isFirstFrameRendered = true
            }
            false
        }
        player.setOnCompletionListener {
            onPlaybackEnded()
        }
        player.setOnErrorListener { _, _, _ ->
            onPlaybackEnded()
            true
        }
        // IO 上打开 FD；setDataSource(FileDescriptor) 返回后即可 close，播放器会 dup 描述符。
        prepareJob = playerScope.launch {
            try {
                withContext(context = Dispatchers.IO) {
                    val openedDescriptor = try {
                        context.contentResolver.openFileDescriptor(videoUri, "r")
                    } catch (throwable: Throwable) {
                        MatisseLog.e(throwable = throwable)
                        null
                    }
                    openedDescriptor.use { fileDescriptor ->
                        withContext(context = Dispatchers.Main.immediate) {
                            if (isReleased || mediaPlayer !== player) {
                                return@withContext
                            }
                            if (fileDescriptor == null) {
                                onPlaybackEnded()
                                return@withContext
                            }
                            try {
                                player.setDataSource(fileDescriptor.fileDescriptor)
                                if (isReleased || mediaPlayer !== player) {
                                    return@withContext
                                }
                                player.prepareAsync()
                            } catch (throwable: Throwable) {
                                if (throwable is CancellationException) {
                                    throw throwable
                                }
                                MatisseLog.e(throwable = throwable)
                                onPlaybackEnded()
                            }
                        }
                    }
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) {
                    throw throwable
                }
                MatisseLog.e(throwable = throwable)
                onPlaybackEnded()
            }
        }
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture,
        width: Int,
        height: Int
    ) = Unit

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        release()
        onPlaybackEnded()
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) = Unit

    fun togglePlayback() {
        val player = mediaPlayer ?: return
        if (!isPrepared) {
            return
        }
        if (player.isPlaying) {
            pause()
        } else {
            start()
        }
    }

    fun onHostStart() {
        isHostStarted = true
        if (resumeWhenHostStarted) {
            resumeWhenHostStarted = false
            start()
        }
    }

    fun onHostStop() {
        isHostStarted = false
        val player = mediaPlayer ?: return
        if (isPrepared && player.isPlaying) {
            pause()
            resumeWhenHostStarted = true
        }
    }

    fun release() {
        if (isReleased) {
            return
        }
        isReleased = true
        prepareJob?.cancel()
        prepareJob = null
        mediaPlayer?.let {
            it.setOnPreparedListener(null)
            it.setOnVideoSizeChangedListener(null)
            it.setOnInfoListener(null)
            it.setOnCompletionListener(null)
            it.setOnErrorListener(null)
            it.release()
        }
        mediaPlayer = null
        surface?.release()
        surface = null
        isPrepared = false
        resumeWhenHostStarted = false
        abandonAudioFocus()
        playerScope.cancel()
    }

    private fun start() {
        val player = mediaPlayer ?: return
        if (!isPrepared) {
            return
        }
        // 例如通话中拿不到焦点：保持暂停并显示播放图标，由用户稍后再点
        if (!requestAudioFocus()) {
            isPaused = true
            return
        }
        player.start()
        isPaused = false
    }

    private fun pause() {
        val player = mediaPlayer ?: return
        if (isPrepared && player.isPlaying) {
            player.pause()
            isPaused = true
        }
    }

    private fun requestAudioFocus(): Boolean {
        val manager = audioManager ?: return true
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = (audioFocusRequest as? AudioFocusRequest) ?: AudioFocusRequest
                .Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
                .also {
                    audioFocusRequest = it
                }
            manager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            manager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        val manager = audioManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = audioFocusRequest as? AudioFocusRequest ?: return
            manager.abandonAudioFocusRequest(request)
        } else {
            @Suppress("DEPRECATION")
            manager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

}
