@file:OptIn(ExperimentalMaterial3Api::class)

package common.ui.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.CameraAlt
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
import common.data.GroupRepository
import common.model.Group
import common.model.User
import common.ui.pages.components.RyderRed
import kotlinx.coroutines.launch

@Composable
fun CreateGroupScreen(
    currentUser: User,
    onCreated: (String) -> Unit,
    onCancel: () -> Unit
) {
    val repo = remember { GroupRepository() }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var pictureUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pictureUri = it }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Surface(color = Color(0xFF111111)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Atcelt", tint = Color.White)
                    }
                    Text(
                        "Izveidot grupu",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (name.isBlank()) { error = "Nosaukums nedrīkst būt tukšs"; return@TextButton }
                            isLoading = true
                            scope.launch {
                                try {
                                    val group = Group(
                                        name = name.trim(),
                                        description = description.trim(),
                                        ownerId = currentUser.uid,
                                        adminIds = listOf(currentUser.uid),
                                        memberIds = listOf(currentUser.uid)
                                    )
                                    val created = repo.createGroup(group)
                                    if (pictureUri != null) {
                                        val picUrl = repo.uploadGroupPicture(pictureUri!!, created.id)
                                        repo.updateGroup(created.copy(pictureUrl = picUrl))
                                    }
                                    onCreated(created.id)
                                } catch (e: Exception) {
                                    error = "Kļūda: ${e.message}"
                                    isLoading = false
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text(
                            "Izveidot",
                            color = if (!isLoading) RyderRed else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A))
                    .border(2.dp, RyderRed, CircleShape)
                    .clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (pictureUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(pictureUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Text("Foto", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Grupas nosaukums *", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RyderRed,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = RyderRed,
                    cursorColor = RyderRed
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Apraksts", color = Color.Gray) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = RyderRed,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = RyderRed,
                    cursorColor = RyderRed
                ),
                maxLines = 5
            )

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Color.Red, fontSize = 13.sp)
            }

            if (isLoading) {
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = RyderRed)
            }
        }
    }
}
