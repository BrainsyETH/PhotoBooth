package com.snapcabin.ui.screens.thankyou

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinBackground
import com.snapcabin.ui.theme.CabinOnBackground
import kotlinx.coroutines.delay

@Composable
fun ThankYouScreen(
    onDone: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(5000)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CabinBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Thank You",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = CabinOnBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your photo is ready",
                fontSize = 24.sp,
                color = CabinAccent,
                textAlign = TextAlign.Center
            )
        }
    }
}
