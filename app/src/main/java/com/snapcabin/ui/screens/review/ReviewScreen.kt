package com.snapcabin.ui.screens.review

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinSecondary
import kotlinx.coroutines.delay

@Composable
fun ReviewScreen(
    photo: Bitmap?,
    autoAcceptSeconds: Int = 10,
    onRetake: () -> Unit,
    onAccept: () -> Unit
) {
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var countdown by remember { mutableIntStateOf(autoAcceptSeconds) }

    // Auto-accept countdown
    LaunchedEffect(autoAcceptSeconds, lastInteraction) {
        if (autoAcceptSeconds <= 0) return@LaunchedEffect
        countdown = autoAcceptSeconds
        while (countdown > 0) {
            delay(1000)
            val elapsed = (System.currentTimeMillis() - lastInteraction) / 1000
            countdown = (autoAcceptSeconds - elapsed.toInt()).coerceAtLeast(0)
        }
        onAccept()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        lastInteraction = System.currentTimeMillis()
                        tryAwaitRelease()
                    }
                )
            }
    ) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = "Captured photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = "No photo captured",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Auto-accept indicator
        if (autoAcceptSeconds > 0 && countdown > 0) {
            Text(
                text = "Auto-accepting in ${countdown}s",
                style = MaterialTheme.typography.bodyMedium,
                color = CabinAccent,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Action buttons
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            BigButton(
                text = "RETAKE",
                onClick = onRetake,
                containerColor = MaterialTheme.colorScheme.surface
            )
            BigButton(
                text = "ACCEPT",
                onClick = onAccept,
                containerColor = CabinSecondary
            )
        }
    }
}
