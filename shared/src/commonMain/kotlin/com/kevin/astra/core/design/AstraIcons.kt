package com.kevin.astra.core.design

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * ASTRA's bespoke line-icon set — thin, monochrome stroked vectors on a 24×24
 * viewport, tinted by the caller. No icon dependency: the app draws its own
 * pictograms so the whole UI shares one crisp, premium visual language.
 */
object AstraIcons {

    private fun lineIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) { block() }
        }.build()

    /** Memory chip — RAM. */
    val Memory: ImageVector = lineIcon("Memory") {
        moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 17f); lineTo(7f, 17f); close()
        moveTo(10f, 10f); lineTo(14f, 10f); lineTo(14f, 14f); lineTo(10f, 14f); close()
        moveTo(10f, 7f); lineTo(10f, 4.5f)
        moveTo(14f, 7f); lineTo(14f, 4.5f)
        moveTo(10f, 17f); lineTo(10f, 19.5f)
        moveTo(14f, 17f); lineTo(14f, 19.5f)
        moveTo(7f, 10f); lineTo(4.5f, 10f)
        moveTo(7f, 14f); lineTo(4.5f, 14f)
        moveTo(17f, 10f); lineTo(19.5f, 10f)
        moveTo(17f, 14f); lineTo(19.5f, 14f)
    }

    /** Stacked layers — storage. */
    val Layers: ImageVector = lineIcon("Layers") {
        moveTo(12f, 4f); lineTo(20f, 8f); lineTo(12f, 12f); lineTo(4f, 8f); close()
        moveTo(4f, 12f); lineTo(12f, 16f); lineTo(20f, 12f)
        moveTo(4f, 16f); lineTo(12f, 20f); lineTo(20f, 16f)
    }

    /** Smartphone — platform. */
    val Smartphone: ImageVector = lineIcon("Smartphone") {
        moveTo(7f, 3.5f); lineTo(17f, 3.5f); lineTo(17f, 20.5f); lineTo(7f, 20.5f); close()
        moveTo(10f, 18f); lineTo(14f, 18f)
    }

    /** Processor — CPU / NPU. */
    val Cpu: ImageVector = lineIcon("Cpu") {
        moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 17f); lineTo(7f, 17f); close()
        moveTo(9.5f, 9.5f); lineTo(14.5f, 9.5f); lineTo(14.5f, 14.5f); lineTo(9.5f, 14.5f); close()
        moveTo(9f, 7f); lineTo(9f, 4.5f)
        moveTo(15f, 7f); lineTo(15f, 4.5f)
        moveTo(9f, 17f); lineTo(9f, 19.5f)
        moveTo(15f, 17f); lineTo(15f, 19.5f)
        moveTo(7f, 9f); lineTo(4.5f, 9f)
        moveTo(7f, 15f); lineTo(4.5f, 15f)
        moveTo(17f, 9f); lineTo(19.5f, 9f)
        moveTo(17f, 15f); lineTo(19.5f, 15f)
    }

    /** Terminal — runtime. */
    val Terminal: ImageVector = lineIcon("Terminal") {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
        moveTo(7.5f, 9.5f); lineTo(10.5f, 12f); lineTo(7.5f, 14.5f)
        moveTo(12.5f, 14.5f); lineTo(16.5f, 14.5f)
    }

    /** House — Home tab. */
    val Home: ImageVector = lineIcon("Home") {
        moveTo(4f, 10.5f); lineTo(12f, 4f); lineTo(20f, 10.5f)
        moveTo(6f, 9f); lineTo(6f, 20f); lineTo(18f, 20f); lineTo(18f, 9f)
        moveTo(10f, 20f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 20f)
    }

    /** Four-point spark — Chat / assistant. */
    val Sparkle: ImageVector = lineIcon("Sparkle") {
        moveTo(12f, 4f); lineTo(13.6f, 10.4f); lineTo(20f, 12f); lineTo(13.6f, 13.6f)
        lineTo(12f, 20f); lineTo(10.4f, 13.6f); lineTo(4f, 12f); lineTo(10.4f, 10.4f); close()
    }

    /** Page with lines — Docs. */
    val Article: ImageVector = lineIcon("Article") {
        moveTo(5f, 4f); lineTo(19f, 4f); lineTo(19f, 20f); lineTo(5f, 20f); close()
        moveTo(8f, 8.5f); lineTo(16f, 8.5f)
        moveTo(8f, 12f); lineTo(16f, 12f)
        moveTo(8f, 15.5f); lineTo(13f, 15.5f)
    }

    /** Bar chart — Benchmark. */
    val BarChart: ImageVector = lineIcon("BarChart") {
        moveTo(4f, 20f); lineTo(20f, 20f)
        moveTo(7f, 20f); lineTo(7f, 13f)
        moveTo(12f, 20f); lineTo(12f, 8f)
        moveTo(17f, 20f); lineTo(17f, 11f)
    }

    /** Sliders — System / settings. */
    val Tune: ImageVector = lineIcon("Tune") {
        moveTo(4f, 8f); lineTo(20f, 8f)
        moveTo(4f, 16f); lineTo(20f, 16f)
        moveTo(13f, 6.2f); lineTo(15f, 6.2f); lineTo(15f, 9.8f); lineTo(13f, 9.8f); close()
        moveTo(8f, 14.2f); lineTo(10f, 14.2f); lineTo(10f, 17.8f); lineTo(8f, 17.8f); close()
    }

    /** Shield with check — private compute. */
    val Shield: ImageVector = lineIcon("Shield") {
        moveTo(12f, 3f); lineTo(19f, 6f); lineTo(19f, 11f)
        curveTo(19f, 16f, 16f, 19.5f, 12f, 21f)
        curveTo(8f, 19.5f, 5f, 16f, 5f, 11f)
        lineTo(5f, 6f); close()
        moveTo(9f, 12f); lineTo(11.2f, 14.2f); lineTo(15f, 9.8f)
    }

    /** Monitor — display / GPU. */
    val Monitor: ImageVector = lineIcon("Monitor") {
        moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 15f); lineTo(4f, 15f); close()
        moveTo(9f, 19f); lineTo(15f, 19f)
        moveTo(12f, 15f); lineTo(12f, 19f)
    }

    /** Isometric cube — a model. */
    val Cube: ImageVector = lineIcon("Cube") {
        moveTo(12f, 3.5f); lineTo(20f, 7.5f); lineTo(20f, 16.5f); lineTo(12f, 20.5f)
        lineTo(4f, 16.5f); lineTo(4f, 7.5f); close()
        moveTo(4f, 7.5f); lineTo(12f, 11.5f); lineTo(20f, 7.5f)
        moveTo(12f, 11.5f); lineTo(12f, 20.5f)
    }

    /** Magnifier — search. */
    val Search: ImageVector = lineIcon("Search") {
        moveTo(11f, 4.5f)
        curveTo(14.6f, 4.5f, 17.5f, 7.4f, 17.5f, 11f)
        curveTo(17.5f, 14.6f, 14.6f, 17.5f, 11f, 17.5f)
        curveTo(7.4f, 17.5f, 4.5f, 14.6f, 4.5f, 11f)
        curveTo(4.5f, 7.4f, 7.4f, 4.5f, 11f, 4.5f)
        close()
        moveTo(15.8f, 15.8f); lineTo(20f, 20f)
    }

    /** Chevron pointing right. */
    val ChevronRight: ImageVector = lineIcon("ChevronRight") {
        moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
    }

    /** Microphone — voice. */
    val Mic: ImageVector = lineIcon("Mic") {
        moveTo(12f, 3.5f)
        curveTo(13.5f, 3.5f, 14.5f, 4.6f, 14.5f, 6f)
        lineTo(14.5f, 11f)
        curveTo(14.5f, 12.4f, 13.5f, 13.5f, 12f, 13.5f)
        curveTo(10.5f, 13.5f, 9.5f, 12.4f, 9.5f, 11f)
        lineTo(9.5f, 6f)
        curveTo(9.5f, 4.6f, 10.5f, 3.5f, 12f, 3.5f)
        close()
        moveTo(6f, 11f)
        curveTo(6f, 14.3f, 8.7f, 17f, 12f, 17f)
        curveTo(15.3f, 17f, 18f, 14.3f, 18f, 11f)
        moveTo(12f, 17f); lineTo(12f, 20.5f)
        moveTo(9f, 20.5f); lineTo(15f, 20.5f)
    }

    /** Envelope — email. */
    val Mail: ImageVector = lineIcon("Mail") {
        moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 18f); lineTo(4f, 18f); close()
        moveTo(4f, 7f); lineTo(12f, 13f); lineTo(20f, 7f)
    }

    /** Cloud — connected service. */
    val Cloud: ImageVector = lineIcon("Cloud") {
        moveTo(7f, 17f)
        curveTo(4.8f, 17f, 3f, 15.2f, 3f, 13f)
        curveTo(3f, 10.8f, 4.8f, 9f, 7f, 9f)
        curveTo(7.3f, 6.7f, 9.3f, 5f, 11.7f, 5f)
        curveTo(14.3f, 5f, 16.4f, 6.9f, 16.9f, 9.3f)
        curveTo(19.2f, 9.3f, 21f, 11.1f, 21f, 13.2f)
        curveTo(21f, 15.3f, 19.2f, 17f, 17f, 17f)
        close()
    }

    /** Stop square. */
    val Stop: ImageVector = lineIcon("Stop") {
        moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 17f); lineTo(7f, 17f); close()
    }

    /** Speaker with waves — audio output. */
    val Speaker: ImageVector = lineIcon("Speaker") {
        moveTo(5f, 9f); lineTo(9f, 9f); lineTo(13f, 5.5f); lineTo(13f, 18.5f)
        lineTo(9f, 15f); lineTo(5f, 15f); close()
        moveTo(16f, 9.5f); curveTo(17.5f, 11f, 17.5f, 13f, 16f, 14.5f)
        moveTo(18f, 7.5f); curveTo(20.5f, 10f, 20.5f, 14f, 18f, 16.5f)
    }

    /** Clock — history. */
    val Clock: ImageVector = lineIcon("Clock") {
        moveTo(12f, 4.5f)
        curveTo(16.1f, 4.5f, 19.5f, 7.9f, 19.5f, 12f)
        curveTo(19.5f, 16.1f, 16.1f, 19.5f, 12f, 19.5f)
        curveTo(7.9f, 19.5f, 4.5f, 16.1f, 4.5f, 12f)
        curveTo(4.5f, 7.9f, 7.9f, 4.5f, 12f, 4.5f)
        close()
        moveTo(12f, 8f); lineTo(12f, 12f); lineTo(15f, 14f)
    }

    /** Play triangle — demo. */
    val Play: ImageVector = lineIcon("Play") {
        moveTo(8f, 6f); lineTo(18f, 12f); lineTo(8f, 18f); close()
    }

    /** Camera — vision. */
    val Camera: ImageVector = lineIcon("Camera") {
        moveTo(4f, 8f); lineTo(8f, 8f); lineTo(9.5f, 5.5f); lineTo(14.5f, 5.5f); lineTo(16f, 8f)
        lineTo(20f, 8f); lineTo(20f, 19f); lineTo(4f, 19f); close()
        moveTo(12f, 10f)
        curveTo(13.9f, 10f, 15.5f, 11.6f, 15.5f, 13.5f)
        curveTo(15.5f, 15.4f, 13.9f, 17f, 12f, 17f)
        curveTo(10.1f, 17f, 8.5f, 15.4f, 8.5f, 13.5f)
        curveTo(8.5f, 11.6f, 10.1f, 10f, 12f, 10f)
        close()
    }
}

/** Convenience wrapper to render an [AstraIcons] vector at a consistent size/tint. */
@Composable
fun AstraIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = AstraColors.TextSecondary,
    size: Dp = 20.dp,
) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}
