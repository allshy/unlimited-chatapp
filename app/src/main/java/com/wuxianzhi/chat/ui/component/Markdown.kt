package com.wuxianzhi.chat.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 *  - GFM tables (| col | col |)
 *  - inline code (`code`)
 *  - **bold** and *italic* / _italic_
 *  - basic headings (#, ##, ###)
 *  - inline HTML <br>, <br/>, <br /> → real line breaks
 * Not a real CommonMark parser, but plenty for chat. Good enough for AI replies.
 */
@Composable
fun MarkdownText(text: String, modifier: Modifier = Modifier) {
    val blocks = remember(text) { splitIntoBlocks(preprocess(text)) }
    Column(modifier) {
        blocks.forEach { block ->
            when (block) {
                is Block.Code -> CodeBlock(block.lang, block.code)
                is Block.Table -> TableBlock(block)
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

@Composable
private fun TableBlock(table: Block.Table) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val headerBg = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = LocalContentColor.current

    Box(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.border(1.dp, borderColor, RoundedCornerShape(6.dp))
        ) {
            // header
            if (table.header.isNotEmpty()) {
                TableRow(
                    cells = table.header,
                    bold = true,
                    bg = headerBg,
                    onSurface = onSurface,
                    borderColor = borderColor,
                )
            }
            // body
            table.rows.forEachIndexed { idx, row ->
                TableRow(
                    cells = row,
                    bold = false,
                    bg = if (idx % 2 == 0) Color.Transparent else headerBg.copy(alpha = 0.35f),
                    onSurface = onSurface,
                    borderColor = borderColor,
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    bold: Boolean,
    bg: Color,
    onSurface: Color,
    borderColor: Color,
) {
    Row(modifier = Modifier.background(bg)) {
        cells.forEachIndexed { i, cell ->
            if (i > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .background(borderColor)
                )
            }
            Text(
                text = renderInline(cell),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = onSurface,
                modifier = Modifier
                    .width(160.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

private sealed interface Block {
    data class Paragraph(val text: String) : Block
    data class Code(val lang: String?, val code: String) : Block
    data class Table(val header: List<String>, val rows: List<List<String>>) : Block
}

/** Normalize inline HTML and stray whitespace before block parsing. */
private fun preprocess(src: String): String {
    return src
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</?p>", RegexOption.IGNORE_CASE), "\n")
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

        // Fenced code block
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
            continue
        }

        // GFM table: header row, then separator row, then any number of body rows
        if (isTableRow(line) && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
            flushPara()
            val header = parseCells(line)
            i += 2  // skip header + separator
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && isTableRow(lines[i])) {
                rows += parseCells(lines[i])
                i++
            }
            out += Block.Table(header, rows)
            continue
        }

        // Regular paragraph line
        if (buf.isNotEmpty()) buf.append('\n')
        buf.append(line)
        i++
    }
    flushPara()
    return out
}

private fun isTableRow(line: String): Boolean {
    val t = line.trim()
    return t.startsWith("|") && t.count { it == '|' } >= 2
}

private fun isTableSeparator(line: String): Boolean {
    val t = line.trim()
    if (!t.startsWith("|")) return false
    // Cells of a separator row contain only `-`, `:`, spaces (and `|` delimiters)
    return t.all { it == '|' || it == '-' || it == ':' || it == ' ' } &&
        t.contains("-")
}

private fun parseCells(line: String): List<String> {
    val trimmed = line.trim().trim('|')
    return trimmed.split("|").map { it.trim() }
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

    // strip leading heading markers
    var headingLevel = 0
    when {
        s.startsWith("### ") -> { i = 4; headingLevel = 3 }
        s.startsWith("## ")  -> { i = 3; headingLevel = 2 }
        s.startsWith("# ")   -> { i = 2; headingLevel = 1 }
    }

    while (i < s.length) {
        if (consume("**", bold)) continue
        if (consume("`", codeBg)) continue
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

    if (headingLevel > 0) {
        val size = when (headingLevel) { 1 -> 20.sp; 2 -> 18.sp; else -> 16.sp }
        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = size), 0, length)
    }
}
