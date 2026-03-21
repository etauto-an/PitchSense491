package com.example.pitchsense.ui.theme

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
* Adapts to the changes of size for letter, images, buttons, etc, based on screen size.
* */

fun android.content.Context.findActivity(): Activity? {
    var context = this
    while (context is android.content.ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun isTablet(): Boolean {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return false
    val windowSizeClass = calculateWindowSizeClass(activity)
    return windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded ||
           windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
}

object Dimensions {
    val titleFontSize @Composable get() = if (isTablet()) 40.sp else 20.sp
    val bodyFontSize @Composable get() = if (isTablet()) 25.sp else 14.sp
    val labelFontSize @Composable get() = if (isTablet()) 25.sp else 11.sp
    val gridTextSize @Composable get() = if (isTablet()) 25.sp else 10.sp

    val badgeSize @Composable get() = if (isTablet()) 52.dp else 32.dp
    val badgeFontSize @Composable get() = if (isTablet()) 18.sp else 12.sp

    // This is for changes in buttons.
    val buttonHeight @Composable get() = if (isTablet()) 64.dp else 48.dp
    val buttonFontSize @Composable get() = if (isTablet()) 30.sp else 15.sp

    // This is for changes in spacing.
    val cardPadding @Composable get() = if (isTablet()) 24.dp else 12.dp
    val spacingLarge @Composable get() = if (isTablet()) 48.dp else 32.dp
    val spacingMedium @Composable get() = if (isTablet()) 24.dp else 16.dp
    val spacingSmall @Composable get() = if (isTablet()) 12.dp else 8.dp
}
