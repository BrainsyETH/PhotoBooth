package com.snapcabin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinOnBackground
import com.snapcabin.ui.theme.CabinPrimary
import com.snapcabin.ui.theme.CabinSurface

@Composable
fun InactivityWarningDialog(
    remainingSeconds: Int,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CabinSurface)
                .padding(48.dp)
        ) {
            Text(
                text = "Still there?",
                fontSize = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = CabinOnBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Resetting in $remainingSeconds seconds",
                fontSize = 20.sp,
                color = CabinAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            BigButton(
                text = "I'M HERE",
                onClick = onDismiss,
                containerColor = CabinPrimary
            )
        }
    }
}
