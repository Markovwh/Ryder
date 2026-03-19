package com.example.ryder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import common.data.UserPreferences
import common.ryder.RyderApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val userPreferences = UserPreferences(this)

        setContent {
            RyderApp(userPreferences = userPreferences)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    RyderApp()
}