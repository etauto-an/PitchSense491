package com.example.pitchsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.pitchsense.ui.components.HeaderBar

/**
 * The entry screen for user authentication.
 * - Uses 'remember' and 'mutableStateOf' to hold user input locally.
 * - @param onLoginSuccess: A callback triggered when the login button is clicked.
 * This allows the NavGraph to handle the actual screen transition.
 */
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    // These variables store the live text entered by the user.
    // 'remember' ensures the text isn't lost when the screen refreshes.
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // Ensures the layout doesn't overlap with the system's
            // gesture/navigation bar at the bottom.
            .navigationBarsPadding()
    ) {
        // Reuse the app-wide branding header.
        HeaderBar(title = "PitchSense Login")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            // Centers the login form vertically in the remaining space.
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Standard text field for email capture.
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Specialized text field for passwords to mask characters.
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                // Masks the text with dots for security.
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // The primary action button.
            Button(
                onClick = onLoginSuccess,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }
        }
    }
}