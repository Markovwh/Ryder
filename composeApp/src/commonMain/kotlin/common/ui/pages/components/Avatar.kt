package common.ui.pages.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

/**
 * Circular avatar — shows [profilePicture] if provided, otherwise a circle with
 * RyderAccent background and the first letter of [nickname] in white.
 */
@Composable
fun UserAvatar(
    profilePicture: String?,
    nickname: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    if (!profilePicture.isNullOrEmpty()) {
        Image(
            painter = rememberAsyncImagePainter(profilePicture),
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(AppColors.avatarPlaceholder),
            contentScale = ContentScale.Crop
        )
    } else {
        Surface(
            modifier = modifier.size(size),
            shape = CircleShape,
            color = RyderAccent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = nickname.take(1).uppercase().ifEmpty { "?" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp
                )
            }
        }
    }
}
