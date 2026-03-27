package com.example.pitchsense.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview. Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.pitchsense.R
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



/** Splash/entry screen. No authentication required for MVP. */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(onLoginSuccess = {})
}
@Composable
fun LoginScreen(onEnter: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        HeaderBar(title = "PitchSense")

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
                .background(Color.White)
                .padding(cardPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.pitchsense_logo),
                contentDescription = "PitchSense logo",
                modifier = Modifier.size(300.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onEnter,
                modifier = Modifier.fillMaxWidth().height(Dimensions.buttonHeight)
            ) {
                Text("Enter", fontSize = Dimensions.buttonFontSize)
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        }
    }
}
