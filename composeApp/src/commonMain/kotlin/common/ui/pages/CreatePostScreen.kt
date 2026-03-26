package common.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import common.data.PostRepository
import common.model.Post
import common.model.User
import common.ui.pages.components.AppColors
import common.ui.pages.components.RyderAccent
import common.ui.pages.components.UserAvatar
import kotlinx.coroutines.launch

@Composable
fun CreatePostScreen(
    currentUser: User,
    onPostCreated: (Post) -> Unit,
    onCancel: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var visibility by remember { mutableStateOf("Publisks") }
    var isUploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val repository = PostRepository()
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        val msg = errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        errorMessage = null
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> selectedUris = selectedUris + uris }

    val bg = AppColors.background
    val surface = AppColors.surface
    val textPrimary = AppColors.textPrimary
    val textSecondary = AppColors.textSecondary
    val inputBorder = AppColors.inputBorder
    val divider = AppColors.divider

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        focusedBorderColor = RyderAccent,
        unfocusedBorderColor = inputBorder,
        focusedLabelColor = RyderAccent,
        unfocusedLabelColor = textSecondary,
        cursorColor = RyderAccent
    )

    Scaffold(
        containerColor = bg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Aizvērt", tint = textPrimary)
                }
                Text(
                    text = "Jauna ziņa",
                    color = textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = {
                        if (description.isBlank() && selectedUris.isEmpty()) {
                            errorMessage = "Pievienojiet aprakstu vai mediju"
                            return@Button
                        }
                        isUploading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                val uploadedUrls = selectedUris.map { uri ->
                                    repository.uploadMedia(uri, currentUser.uid)
                                }
                                val post = Post(
                                    userId = currentUser.uid,
                                    user = currentUser,
                                    mediaUrls = uploadedUrls,
                                    description = description.trim(),
                                    visibility = visibility
                                )
                                val savedPost = repository.createPost(post)
                                onPostCreated(savedPost)
                            } catch (e: Exception) {
                                errorMessage = "Kļūda: ${e.localizedMessage}"
                                isUploading = false
                            }
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RyderAccent,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFCCCCCC)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Publicēt", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = divider)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(
                        profilePicture = currentUser.profilePicture,
                        nickname = currentUser.nickname,
                        size = 44.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = currentUser.nickname,
                        color = textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                OutlinedTextField(
                    shape = RoundedCornerShape(12.dp),
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Raksti ko notiek...", color = AppColors.textHint) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    colors = textFieldColors,
                    maxLines = 8
                )

                Text("Foto / Video", color = textSecondary, fontSize = 13.sp)

                if (selectedUris.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(surface, RoundedCornerShape(12.dp))
                            .clickable {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = RyderAccent,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Pievienot foto / video", color = textSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(selectedUris) { uri ->
                            Box(modifier = Modifier.size(120.dp)) {
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AppColors.avatarPlaceholder),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedUris = selectedUris - uri },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(28.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Noņemt",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(surface, RoundedCornerShape(8.dp))
                                    .clickable {
                                        mediaPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Pievienot vairāk", tint = RyderAccent)
                            }
                        }
                    }
                }

                Text("Privātums", color = textSecondary, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Publisks", "Draugi", "Privāts").forEach { option ->
                        val selected = visibility == option
                        OutlinedButton(
                            onClick = { visibility = option },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) RyderAccent else Color.Transparent,
                                contentColor = if (selected) Color.White else textPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) RyderAccent else inputBorder
                            )
                        ) {
                            Text(option, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
