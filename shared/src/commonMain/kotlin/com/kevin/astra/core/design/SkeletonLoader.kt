package com.kevin.astra.core.design

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    cornerRadius: Dp = 6.dp,
) {
    val infinite = rememberInfiniteTransition(label = "skeleton")
    val alpha by infinite.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .alpha(alpha)
            .background(AstraColors.SurfaceElevated),
    )
}

@Composable
fun SkeletonConversationItem(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AstraColors.Surface, RoundedCornerShape(16.dp))
            .padding(AstraSpacing.M),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SkeletonBox(modifier = Modifier.fillMaxWidth(0.55f), height = 14.dp)
            SkeletonBox(modifier = Modifier.width(60.dp), height = 12.dp)
        }
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.80f), height = 12.dp)
        SkeletonBox(modifier = Modifier.fillMaxWidth(0.40f), height = 12.dp)
    }
}

@Composable
fun SkeletonList(count: Int = 4, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AstraSpacing.S),
    ) {
        repeat(count) {
            SkeletonConversationItem()
        }
    }
}
