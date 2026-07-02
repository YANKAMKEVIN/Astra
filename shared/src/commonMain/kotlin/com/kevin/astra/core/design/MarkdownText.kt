package com.kevin.astra.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    baseStyle: TextStyle = AstraTypography.Body,
    baseColor: Color = AstraColors.TextPrimary,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AstraSpacing.XS)) {
        parseMarkdownBlocks(text).forEach { block ->
            when (block) {
                is MdBlock.H1 -> Text(
                    text = inlineAnnotated(block.text),
                    style = AstraTypography.Title,
                    color = baseColor,
                    fontWeight = FontWeight.Bold,
                )
                is MdBlock.H2 -> Text(
                    text = inlineAnnotated(block.text),
                    style = baseStyle,
                    color = baseColor,
                    fontWeight = FontWeight.SemiBold,
                )
                is MdBlock.Bullet -> Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                    Text(text = "•", style = baseStyle, color = AstraColors.Secondary)
                    Text(text = inlineAnnotated(block.text), style = baseStyle, color = baseColor)
                }
                is MdBlock.NumberedItem -> Row(horizontalArrangement = Arrangement.spacedBy(AstraSpacing.S)) {
                    Text(text = "${block.number}.", style = baseStyle, color = AstraColors.Secondary, fontWeight = FontWeight.Medium)
                    Text(text = inlineAnnotated(block.text), style = baseStyle, color = baseColor)
                }
                is MdBlock.Code -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AstraColors.SurfaceElevated, RoundedCornerShape(8.dp))
                        .border(1.dp, AstraColors.Border, RoundedCornerShape(8.dp))
                        .padding(AstraSpacing.M),
                ) {
                    Text(text = block.code, style = AstraTypography.Metric, color = AstraColors.Secondary)
                }
                is MdBlock.HRule -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AstraColors.Border),
                )
                is MdBlock.Paragraph -> Text(
                    text = inlineAnnotated(block.text),
                    style = baseStyle,
                    color = baseColor,
                )
                is MdBlock.Empty -> Spacer(Modifier.height(2.dp))
            }
        }
    }
}

// ── Block types ───────────────────────────────────────────────────────────────

private sealed interface MdBlock {
    data class H1(val text: String) : MdBlock
    data class H2(val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class NumberedItem(val number: Int, val text: String) : MdBlock
    data class Code(val code: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data object HRule : MdBlock
    data object Empty : MdBlock
}

private fun parseMarkdownBlocks(text: String): List<MdBlock> {
    val lines = text.lines()
    val blocks = mutableListOf<MdBlock>()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.startsWith("```") -> {
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MdBlock.Code(codeLines.joinToString("\n")))
                i++
            }
            line.startsWith("### ") -> {
                blocks.add(MdBlock.H2(line.removePrefix("### ")))
                i++
            }
            line.startsWith("## ") -> {
                blocks.add(MdBlock.H2(line.removePrefix("## ")))
                i++
            }
            line.startsWith("# ") -> {
                blocks.add(MdBlock.H1(line.removePrefix("# ")))
                i++
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                blocks.add(MdBlock.Bullet(line.drop(2)))
                i++
            }
            line.matches(Regex("^\\d+\\.\\s.*")) -> {
                val dotIdx = line.indexOf('.')
                val number = line.substring(0, dotIdx).toIntOrNull() ?: 1
                blocks.add(MdBlock.NumberedItem(number, line.substring(dotIdx + 2)))
                i++
            }
            line.matches(Regex("^[-*_]{3,}$")) -> {
                blocks.add(MdBlock.HRule)
                i++
            }
            line.isBlank() -> {
                if (blocks.lastOrNull() !is MdBlock.Empty) blocks.add(MdBlock.Empty)
                i++
            }
            else -> {
                blocks.add(MdBlock.Paragraph(line))
                i++
            }
        }
    }
    return blocks
}

// ── Inline formatting ─────────────────────────────────────────────────────────

private fun inlineAnnotated(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                } else { append(text[i]); i++ }
            }
            text.startsWith("*", i) -> {
                val end = text.indexOf("*", i + 1)
                if (end != -1 && !text.startsWith("*", end + 1)) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            text.startsWith("`", i) -> {
                val end = text.indexOf("`", i + 1)
                if (end != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = AstraColors.SurfaceElevated,
                        ),
                    ) {
                        append(" ${text.substring(i + 1, end)} ")
                    }
                    i = end + 1
                } else { append(text[i]); i++ }
            }
            else -> { append(text[i]); i++ }
        }
    }
}
