package common.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import common.data.AuthService
import common.data.PostRepository
import common.model.User
import common.ui.pages.components.RyderRed
import kotlinx.coroutines.launch

@Composable
fun EditProfileScreen(
    user: User,
    authService: AuthService,
    onSaved: (User) -> Unit,
    onCancel: () -> Unit
) {
    var nickname by remember { mutableStateOf(user.nickname) }
    var firstName by remember { mutableStateOf(user.firstName) }
    var lastName by remember { mutableStateOf(user.lastName) }
    var bio by remember { mutableStateOf(user.bio) }
    var bike by remember { mutableStateOf(user.bike) }
    var profilePrivacy by remember { mutableStateOf(user.profilePrivacy) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var nicknameError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val repository = PostRepository()

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pickedUri = uri }

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = RyderRed,
        unfocusedBorderColor = Color.Gray,
        focusedLabelColor = RyderRed,
        unfocusedLabelColor = Color.Gray,
        cursorColor = RyderRed
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Atcelt", tint = Color.White)
            }
            Text(
                text = "Rediģēt profilu",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = {
                    if (nickname.isBlank()) {
                        nicknameError = "Segvārds nevar būt tukšs"
                        return@Button
                    }
                    isSaving = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val newPictureUrl = if (pickedUri != null) {
                                repository.uploadProfilePicture(pickedUri!!, user.uid)
                            } else {
                                user.profilePicture
                            }
                            val updatedUser = user.copy(
                                nickname = nickname.trim(),
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                bio = bio.trim(),
                                bike = bike.trim(),
                                profilePrivacy = profilePrivacy,
                                profilePicture = newPictureUrl
                            )
                            val result = authService.updateUserData(updatedUser)
                            if (result.isSuccess) {
                                onSaved(updatedUser)
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Kļūda saglabājot"
                                isSaving = false
                            }
                        } catch (e: Exception) {
                            errorMessage = "Kļūda: ${e.localizedMessage}"
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RyderRed,
                    contentColor = Color.White,
                    disabledContainerColor = Color.DarkGray
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Saglabāt", fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider(color = Color.DarkGray)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile picture picker
            Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    painter = rememberAsyncImagePainter(
                        pickedUri ?: user.profilePicture ?: "https://picsum.photos/200"
                    ),
                    contentDescription = "Profila bilde",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .border(2.dp, RyderRed, CircleShape)
                        .clickable {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(RyderRed, CircleShape)
                        .clickable {
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✎", color = Color.White, fontSize = 14.sp)
                }
            }

            Text(
                text = "Mainīt profila bildi",
                color = RyderRed,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            // Nickname
            OutlinedTextField(
                shape = RoundedCornerShape(12.dp),
                value =nickname,
                onValueChange = { nickname = it; nicknameError = null },
                label = { Text("Segvārds") },
                isError = nicknameError != null,
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            nicknameError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

            // First name
            OutlinedTextField(
                shape = RoundedCornerShape(12.dp),
                value =firstName,
                onValueChange = { firstName = it },
                label = { Text("Vārds") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Last name
            OutlinedTextField(
                shape = RoundedCornerShape(12.dp),
                value =lastName,
                onValueChange = { lastName = it },
                label = { Text("Uzvārds") },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Bio
            OutlinedTextField(
                shape = RoundedCornerShape(12.dp),
                value =bio,
                onValueChange = { bio = it },
                label = { Text("Par sevi") },
                placeholder = { Text("Pastāsti par sevi...", color = Color.Gray) },
                colors = textFieldColors,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
            )

            // Bike
            OutlinedTextField(
                shape = RoundedCornerShape(12.dp),
                value =bike,
                onValueChange = { bike = it },
                label = { Text("Motocikls") },
                placeholder = { Text("piem. Honda CB500F", color = Color.Gray) },
                colors = textFieldColors,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Privacy
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Profila privātums", color = Color.Gray, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Publisks", "Privāts").forEach { option ->
                        val selected = profilePrivacy == option
                        OutlinedButton(
                            onClick = { profilePrivacy = option },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) RyderRed else Color.Transparent,
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) RyderRed else Color.Gray
                            )
                        ) {
                            Text(option)
                        }
                    }
                }
            }

            errorMessage?.let {
                Text(it, color = Color.Red, fontSize = 13.sp)
            }
        }
    }
}