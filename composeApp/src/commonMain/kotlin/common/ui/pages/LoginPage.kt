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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.ui.pages.components.RyderRed

@Composable
fun LoginPage(
    backendError: String? = null,
    onLogin: (email: String, password: String, rememberMe: Boolean) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onRegisterClick: () -> Unit,
    onContinueAsGuest: () -> Unit
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

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
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Title
        Text(
            text = "Pieslēgties",
            color = RyderRed,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        // Sign up link below title, underlined
        Text(
            text = "Nav konta? Reģistrēties.",
            color = RyderRed,
            fontSize = 14.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onRegisterClick() }
        )

        Spacer(Modifier.height(24.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text("E-pasts") },
            isError = emailError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        emailError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

        Spacer(Modifier.height(12.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
            },
            label = { Text("Parole") },
            isError = passwordError != null,
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        passwordError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

        Spacer(Modifier.height(12.dp))

        // Remember Me checkbox
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors = CheckboxDefaults.colors(checkedColor = RyderRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Atcerēties mani", color = Color.White)
        }

        Spacer(Modifier.height(12.dp))

        // Login button
        Button(
            onClick = {
                var valid = true
                if (email.isBlank()) { emailError = "E-pasts ir obligāts"; valid = false }
                if (password.isBlank()) { passwordError = "Parole ir obligāta"; valid = false }
                if (valid) onLogin(email.trim(), password, rememberMe)  // pass rememberMe
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = RyderRed,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text("Pieslēgties")
        }

        Spacer(Modifier.height(12.dp))

        // Continue as Guest button
        Button(
            onClick = { onContinueAsGuest() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Gray,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text("Turpināt kā viesis")
        }

        Spacer(Modifier.height(12.dp))

        // Forgot password link
        Text(
            text = "Aizmirsāt paroli?",
            color = RyderRed,
            fontSize = 14.sp,
            modifier = Modifier.clickable {
                if (email.isNotBlank()) {
                    onForgotPassword(email.trim())
                } else {
                    emailError = "Ievadiet e-pastu, lai atiestatītu paroli"
                }
            }
        )

        backendError?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                color = Color.Red,
                fontSize = 13.sp
            )
        }
    }
}