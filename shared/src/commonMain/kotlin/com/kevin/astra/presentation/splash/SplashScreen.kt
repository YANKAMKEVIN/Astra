package com.kevin.astra.presentation.splash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevin.astra.core.design.AstraColors
import com.kevin.astra.core.design.AstraSpacing
import com.kevin.astra.core.design.AstraTypography
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    contentPadding: PaddingValues,
    onFinished: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(1_800)
        onFinished()
    }

    val transition = rememberInfiniteTransition(label = "splash-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splash-pulse-alpha",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AstraColors.Background)
            .padding(contentPadding)
            .padding(AstraSpacing.XL),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(AstraColors.Primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .alpha(pulse)
                    .background(AstraColors.Secondary, CircleShape),
            )
        }
        Spacer(Modifier.height(AstraSpacing.XL))
        Text(
            text = "ASTRA",
            style = AstraTypography.DisplayLarge,
            color = AstraColors.TextPrimary,
        )
        Spacer(Modifier.height(AstraSpacing.S))
        Text(
            text = "Secure Local AI\nfor Critical Operations",
            style = AstraTypography.Title,
            color = AstraColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AstraSpacing.XXL))
        Text(
            text = "Initializing local AI environment...",
            style = AstraTypography.Metric,
            color = AstraColors.Secondary,
            textAlign = TextAlign.Center,
        )
    }
}
