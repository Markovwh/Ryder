package common.ui.pages.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import common.model.Post
import java.util.concurrent.TimeUnit

@Composable
fun PostCard(post: Post) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 8.dp)
    ) {
        // User info row
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            Image(
                painter = rememberAsyncImagePainter(post.user.profilePicture ?: "https://picsum.photos/200"),
                contentDescription = "Profila bilde",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = post.user.nickname, color = Color.White)
        }

        // Media: swipeable pager if multiple, single image otherwise
        if (post.mediaUrls.isNotEmpty()) {
            val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })
            Box {
                HorizontalPager(state = pagerState) { page ->
                    Image(
                        painter = rememberAsyncImagePainter(post.mediaUrls[page]),
                        contentDescription = "Ziņas medijs",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color.DarkGray),
                        contentScale = ContentScale.Crop
                    )
                }
                // Page indicator dots
                if (post.mediaUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(post.mediaUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (pagerState.currentPage == index) Color.White
                                        else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Description
        if (post.description.isNotEmpty()) {
            Text(
                text = post.description,
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
        }

        // Time
        Text(
            text = PostCardTimeFormatter.formatTimeAgo(post.createdAt),
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
    }
}

object PostCardTimeFormatter {
    fun formatTimeAgo(timeMillis: Long): String {
        if (timeMillis == 0L) return ""
        val diff = System.currentTimeMillis() - timeMillis
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            minutes < 1 -> "Tikko"
            minutes < 60 -> "Pirms $minutes min"
            hours < 24 -> "Pirms $hours st"
            else -> "Pirms $days d"
        }
    }
}