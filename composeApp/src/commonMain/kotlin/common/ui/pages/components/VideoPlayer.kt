package common.ui.pages.components

import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun rememberVideoThumbnailLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        ImageLoader.Builder(context)
            .components { add(VideoFrameDecoder.Factory()) }
            .build()
    }
}

fun String.isVideoUrl(): Boolean {
    val path = substringBefore("?").lowercase()
    return path.endsWith(".mp4") || path.endsWith(".mov") ||
            path.endsWith(".webm") || path.endsWith(".3gp")
}

@Composable
fun VideoPlayer(url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    var isPlaying by remember { mutableStateOf(true) }
    var showIcon by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (player.isPlaying) {
                        player.pause()
                        isPlaying = false
                    } else {
                        player.play()
                        isPlaying = true
                    }
                    showIcon = true
                    scope.launch {
                        delay(800)
                        showIcon = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = showIcon,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
