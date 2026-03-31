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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.ui.pages.components.RyderAccent

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
            text = "Pieslēgties",
            color = RyderAccent,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Nav konta? Reģistrēties.",
            color = RyderAccent,
            fontSize = 14.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { onRegisterClick() }
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            shape = RoundedCornerShape(12.dp),
            value = email,
            onValueChange = { email = it; emailError = null },
            label = { Text("E-pasts") },
            isError = emailError != null || backendError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        emailError?.let { Text(it, color = Color(0xFFE53935), fontSize = 12.sp) }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            shape = RoundedCornerShape(12.dp),
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = { Text("Parole") },
            isError = passwordError != null || backendError != null,
            supportingText = {
                val msg = passwordError ?: backendError
                if (msg != null) Text(msg, color = Color(0xFFE53935))
            },
            visualTransformation = PasswordVisualTransformation(),
            colors = textFieldColors,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 8.dp)
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it },
                colors = CheckboxDefaults.colors(checkedColor = RyderAccent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Atcerēties mani", color = Color(0xFF1A1A1A))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                var valid = true
                if (email.isBlank()) { emailError = "E-pasts ir obligāts"; valid = false }
                if (password.isBlank()) { passwordError = "Parole ir obligāta"; valid = false }
                if (valid) onLogin(email.trim(), password, rememberMe)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = RyderAccent,
                contentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text("Pieslēgties", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onContinueAsGuest() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E0E0),
                contentColor = Color(0xFF1A1A1A)
            ),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text("Turpināt kā viesis")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Aizmirsāt paroli?",
            color = RyderAccent,
            fontSize = 14.sp,
            modifier = Modifier.clickable {
                if (email.isNotBlank()) {
                    onForgotPassword(email.trim())
                } else {
                    emailError = "Ievadiet e-pastu, lai atiestatītu paroli"
                }
            }
        )

    }
}
