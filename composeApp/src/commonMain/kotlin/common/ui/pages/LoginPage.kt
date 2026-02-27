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
    onLogin: (email: String, password: String) -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onRegisterClick: () -> Unit,      // callback for sign up
    onContinueAsGuest: () -> Unit     // callback for guest
) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            text = "Login",
            color = RyderRed,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        // Sign up link below title, underlined
        Text(
            text = "Don't have an account? Sign up.",
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
            label = { Text("Email") },
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
            label = { Text("Password") },
            isError = passwordError != null,
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        passwordError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }

        Spacer(Modifier.height(24.dp))

        // Login button
        Button(
            onClick = {
                var valid = true
                if (email.isBlank()) { emailError = "Email is required"; valid = false }
                if (password.isBlank()) { passwordError = "Password is required"; valid = false }
                if (valid) onLogin(email.trim(), password)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = RyderRed,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text("Log In")
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
            Text("Continue as Guest")
        }

        Spacer(Modifier.height(12.dp))

        // Forgot password link
        Text(
            text = "Forgot password?",
            color = RyderRed,
            fontSize = 14.sp,
            modifier = Modifier.clickable {
                if (email.isNotBlank()) {
                    onForgotPassword(email.trim())
                } else {
                    emailError = "Enter your email to reset password"
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