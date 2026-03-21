package com.example.pitchsense.ui.screens

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

/** Splash/entry screen. No authentication required for MVP. */
@Composable
fun LoginScreen(onEnter: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        HeaderBar(title = "PitchSense")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(28.dp),
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
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Enter", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
