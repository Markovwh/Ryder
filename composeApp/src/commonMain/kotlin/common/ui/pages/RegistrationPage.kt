package common.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import common.ui.pages.components.RyderRed

@Composable
fun RegistrationPage(onContinue: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(48.dp))

        // App title
        Text(
            text = "Ryder",
            color = RyderRed,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create your account",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Username input box
        InputBox(label = "Username")

        Spacer(modifier = Modifier.height(16.dp))

        // Email input box
        InputBox(label = "Email")

        Spacer(modifier = Modifier.height(16.dp))

        // Password input box
        InputBox(label = "Password")

        Spacer(modifier = Modifier.height(32.dp))

        // Register button
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RyderRed
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Sign Up",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Already have an account
        Text(
            text = "Already have an account? Log in",
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun InputBox(label: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        color = Color(0xFF1C1C1C),
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}