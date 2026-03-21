package com.example.pitchsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview. Preview
import androidx.compose.ui.unit.dp
import com.example.pitchsense.ui.components.HeaderBar
import com.example.pitchsense.ui.theme.Dimensions
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import com.example.pitchsense.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background



/**
 * The entry screen for user authentication.
 * - Uses 'remember' and 'mutableStateOf' to hold user input locally.
 * - @param onLoginSuccess: A callback triggered when the login button is clicked.
 * This allows the NavGraph to handle the actual screen transition.
 */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginSuccess = {})
}
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

        Box(modifier = Modifier.fillMaxSize()) {
            // Image in the login screen, based on the Team.
            Image(
                painter = painterResource(id = R.drawable.login_page_image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )




        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimensions.spacingMedium),
            // Centers the login form vertically in the remaining space.
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimensions.spacingLarge))

            // Baseball team's Name
            Text(
                text = "Dodgers",
                fontSize = Dimensions.titleFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(Dimensions.spacingSmall)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Standard text field for email capture.
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", fontSize = Dimensions.labelFontSize) },
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)),
                singleLine = true,
                textStyle = TextStyle(fontSize = Dimensions.bodyFontSize)
            )

            Spacer(modifier = Modifier.height(Dimensions.spacingMedium))

            // Specialized text field for passwords to mask characters.
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", fontSize = Dimensions.labelFontSize) },
                // Masks the text with dots for security.
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)),
                singleLine = true,
                textStyle = TextStyle(fontSize = Dimensions.bodyFontSize)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // The primary action button.
            Button(
                onClick = onLoginSuccess,
                modifier = Modifier.fillMaxWidth().height(Dimensions.buttonHeight)
            ) {
                Text("Login", fontSize = Dimensions.buttonFontSize)
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        }
    }
}
