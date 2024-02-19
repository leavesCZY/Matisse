package github.leavesczy.matisse.internal

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import github.leavesczy.matisse.MediaResource
import github.leavesczy.matisse.R

/**
 * @Author: leavesCZY
 * @Date: 2024/2/18 15:37
 * @Desc:
 */
internal class MatisseVideoViewActivity : AppCompatActivity() {

    private val mediaResource by lazy(mode = LazyThreadSafetyMode.NONE) {
        intent.getParcelableExtra<MediaResource>(MediaResource::class.java.simpleName)!!
    }

    private val videoView by lazy(mode = LazyThreadSafetyMode.NONE) {
        findViewById<VideoView>(R.id.videoView)
    }

    private val mediaController by lazy(mode = LazyThreadSafetyMode.NONE) {
        MediaController(this)
    }

    private var lastPosition = -1

    private val onPreparedListener = MediaPlayer.OnPreparedListener {
        mediaController.setAnchorView(videoView)
        mediaController.setMediaPlayer(videoView)
        videoView.setMediaController(mediaController)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matisse_video_view)
        videoView.setOnPreparedListener(onPreparedListener)
        videoView.setVideoURI(mediaResource.uri)
        videoView.start()
    }

    override fun onResume() {
        super.onResume()
        if (lastPosition > 0) {
            videoView.seekTo(lastPosition)
        }
        lastPosition = -1
    }

    override fun onPause() {
        lastPosition = videoView.currentPosition
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        videoView.setOnPreparedListener(null)
        videoView.suspend()
    }

}