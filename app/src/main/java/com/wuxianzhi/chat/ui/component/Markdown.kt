package com.wuxianzhi.chat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lightweight markdown-ish renderer. Handles:
 *  - fenced code blocks (```)
 *  - inline code (`code`)
 *  - **bold** and *italic* / _italic_
 *  - basic headings (#, ##, ###)
 *  - bullet/numbered lists (passes through)
 * Not a real CommonMark parser, but plenty for chat. Strips raw nothing.
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { splitIntoBlocks(text) }
    Column(modifier) {
        blocks.forEach { block ->
            when (block) {
                is Block.Code -> CodeBlock(block.lang, block.code)
                is Block.Paragraph -> {
                    Text(
                        text = renderInline(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalContentColor.current,
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlock(lang: String?, code: String) {
    val bg = MaterialTheme.colorScheme.surfaceVariant
    val onBg = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        if (!lang.isNullOrBlank()) {
            Text(
                text = lang,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = onBg,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = onBg,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        )
    }
}

private sealed interface Block {
    data class Paragraph(val text: String) : Block
    data class Code(val lang: String?, val code: String) : Block
}

private fun splitIntoBlocks(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val lines = src.lines()
    var i = 0
    val buf = StringBuilder()
    fun flushPara() {
        if (buf.isNotEmpty()) {
            out += Block.Paragraph(buf.toString().trimEnd())
            buf.setLength(0)
        }
    }
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trimStart()
        if (trimmed.startsWith("```")) {
            flushPara()
            val lang = trimmed.removePrefix("```").trim().ifBlank { null }
            i++
            val code = StringBuilder()
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                code.appendLine(lines[i])
                i++
            }
            if (i < lines.size) i++  // closing fence
            out += Block.Code(lang, code.toString().trimEnd('\n'))
        } else {
            if (buf.isNotEmpty()) buf.append('\n')
            buf.append(line)
            i++
        }
    }
    flushPara()
    return out
}

@Composable
private fun renderInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val s = text
    fun consume(prefix: String, style: SpanStyle): Boolean {
        if (!s.startsWith(prefix, i)) return false
        val endIdx = s.indexOf(prefix, i + prefix.length)
        if (endIdx < 0) return false
        withStyle(style) { append(s.substring(i + prefix.length, endIdx)) }
        i = endIdx + prefix.length
        return true
    }

    val bold = SpanStyle(fontWeight = FontWeight.Bold)
    val italic = SpanStyle(fontStyle = FontStyle.Italic)
    val codeBg = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = MaterialTheme.colorScheme.surfaceVariant,
    )

    // very simple heading handling for the first line of a paragraph
    var headingApplied = false
    if (s.startsWith("### ")) {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)) { /* placeholder */ }
        // fallthrough — keep simple, just strip the marker:
        i = 4
        headingApplied = true
    } else if (s.startsWith("## ")) {
        i = 3; headingApplied = true
    } else if (s.startsWith("# ")) {
        i = 2; headingApplied = true
    }

    while (i < s.length) {
        if (consume("**", bold)) continue
        if (consume("`", codeBg)) continue
        // single * italic — only if surrounded by non-space
        if (s[i] == '*' && i + 1 < s.length && s[i + 1] != ' ' && s[i + 1] != '*') {
            val end = s.indexOf('*', i + 1)
            if (end > i + 1) {
                withStyle(italic) { append(s.substring(i + 1, end)) }
                i = end + 1
                continue
            }
        }
        append(s[i])
        i++
    }
    if (headingApplied) {
        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp), 0, length)
    }
}
