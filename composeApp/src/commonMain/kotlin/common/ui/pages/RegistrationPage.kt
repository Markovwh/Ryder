package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import common.ui.pages.components.RyderAccent
import kotlinx.coroutines.launch

@Composable
fun RegistrationPage(
    backendError: String? = null,
    nicknameBackendError: String? = null,
    onCheckNicknameAvailable: suspend (String) -> Boolean,
    onRegister: (
        email: String,
        password: String,
        nickname: String,
        firstName: String,
        lastName: String
    ) -> Unit,
    onLoginClick: () -> Unit
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var nicknameError by remember { mutableStateOf<String?>(null) }
    var firstNameError by remember { mutableStateOf<String?>(null) }
    var lastNameError by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF1A1A1A),
        unfocusedTextColor = Color(0xFF1A1A1A),
        focusedBorderColor = RyderAccent,
        unfocusedBorderColor = Color(0xFF9E9E9E),
        focusedLabelColor = RyderAccent,
        unfocusedLabelColor = Color(0xFF757575),
        cursorColor = RyderAccent
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEEEEEE))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Reģistrācija",
            color = RyderAccent,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            shape = RoundedCornerShape(12.dp),
            value = email,
            onValueChange = { email = it; emailError = null },
            label = { Text("E-pasts") },
            isError = emailError != null,
            supportingText = {
                Text(
                    emailError ?: "Piemērs: lietotajs@epasts.lv",
                    color = if (emailError != null) Color(0xFFE53935) else Color(0xFF757575)
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        val effectiveNicknameError = nicknameError ?: nicknameBackendError
        OutlinedTextField(
            shape = RoundedCornerShape(12.dp),
            value = nickname,
            onValueChange = { nickname = it; nicknameError = null },
            label = { Text("Lietotājvārds") },
            isError = effectiveNicknameError != null,
            supportingText = {
                Text(
                    effectiveNicknameError ?: "Redzams citiem lietotājiem",
                    color = if (effectiveNicknameError != null) Color(0xFFE53935) else Color(0xFF757575)
                )
            },
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        OutlinedTextField(
            shape = RoundedCornerShape(12.dp),
            value = firstName,
            onValueChange = { firstName = it; firstNameError = null },
            label = { Text("Vārds") },
            isError = firstNameError != null,
            supportingText = {
                Text(
                    firstNameError ?: "Tavs īstais vārds",
                    color = if (firstNameError != null) Color(0xFFE53935) else Color(0xFF757575)
                )
            },
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        OutlinedTextField(
            shape = RoundedCornerShape(12.dp),
            value = lastName,
            onValueChange = { lastName = it; lastNameError = null },
            label = { Text("Uzvārds") },
            isError = lastNameError != null,
            supportingText = {
                Text(
                    lastNameError ?: "Tavs īstais uzvārds",
                    color = if (lastNameError != null) Color(0xFFE53935) else Color(0xFF757575)
                )
            },
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        OutlinedTextField(
            shape = RoundedCornerShape(12.dp),
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = { Text("Parole") },
            isError = passwordError != null,
            supportingText = {
                Text(
                    passwordError ?: "Vismaz 8 simboli",
                    color = if (passwordError != null) Color(0xFFE53935) else Color(0xFF757575)
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        OutlinedTextField(
            shape = RoundedCornerShape(12.dp),
            value = confirmPassword,
            onValueChange = { confirmPassword = it; confirmPasswordError = null },
            label = { Text("Apstiprināt paroli") },
            isError = confirmPasswordError != null,
            supportingText = {
                Text(
                    confirmPasswordError ?: "Ievadi paroli vēlreiz",
                    color = if (confirmPasswordError != null) Color(0xFFE53935) else Color(0xFF757575)
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        backendError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color(0xFFE53935), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    var valid = true

                    if (email.isBlank()) { emailError = "E-pasts ir obligāts"; valid = false }
                    else if (!email.matches(emailRegex)) { emailError = "Nederīgs e-pasta formāts"; valid = false }

                    if (nickname.isBlank()) {
                        nicknameError = "Lietotājvārds ir obligāts"
                        valid = false
                    } else {
                        isChecking = true
                        val available = onCheckNicknameAvailable(nickname.trim())
                        isChecking = false
                        if (!available) {
                            nicknameError = "Šis lietotājvārds jau ir aizņemts"
                            valid = false
                        }
                    }

                    if (firstName.isBlank()) { firstNameError = "Vārds ir obligāts"; valid = false }
                    if (lastName.isBlank()) { lastNameError = "Uzvārds ir obligāts"; valid = false }
                    if (password.isBlank()) { passwordError = "Parole ir obligāta"; valid = false }
                    else if (password.length < 8) { passwordError = "Parolei jābūt vismaz 8 simbolus garai"; valid = false }
                    if (confirmPassword.isBlank()) { confirmPasswordError = "Lūdzu apstipriniet paroli"; valid = false }
                    else if (password != confirmPassword) { confirmPasswordError = "Paroles nesakrīt"; valid = false }

                    if (valid) onRegister(email, password, nickname, firstName, lastName)
                }
            },
            enabled = !isChecking,
            colors = ButtonDefaults.buttonColors(
                containerColor = RyderAccent,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFCCCCCC)
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            if (isChecking) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Izveidot kontu", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = buildAnnotatedString { append("Jau reģistrējies? Ieiet sistēmā") },
            color = RyderAccent,
            fontSize = 14.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onLoginClick() }
        )
    }
}
