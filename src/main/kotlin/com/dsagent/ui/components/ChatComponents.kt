package com.dsagent.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dsagent.ui.theme.*

@Composable
fun UserBubble(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            modifier = Modifier.widthIn(max = 500.dp),
            color = LightGray,
            shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
            shadowElevation = 1.dp
        ) {
            Text(text, modifier = Modifier.padding(12.dp), color = DarkText, fontSize = 14.sp)
        }
    }
}

@Composable
fun AIResponse(text: String) {
    val blocks = parseMarkdownBlocks(text)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Text -> Text(
                    block.text, color = DarkText, fontSize = 14.sp, lineHeight = 22.sp
                )
                is MarkdownBlock.Code -> CodeBlockView(block.code, block.language)
            }
        }
    }
}

@Composable
fun CodeBlockView(code: String, language: String) {
    var copied by remember { mutableStateOf(false) }
    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
    
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = CodeBackground,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(language, color = LightBlue, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                TextButton(onClick = {
                    clipboard.setContents(java.awt.datatransfer.StringSelection(code), null)
                    copied = true
                }) {
                    Text(if (copied) "Copiado" else "Copiar", color = LightBlue, fontSize = 11.sp)
                }
            }
            Text(
                code, modifier = Modifier.horizontalScroll(rememberScrollState()).padding(12.dp),
                fontFamily = FontFamily.Monospace, color = White, fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ThinkingIndicator() {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LightBlue, strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
        Text("Pensando...", color = LightBlue, fontSize = 12.sp)
    }
}

@Composable
fun ChatInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    isStreaming: Boolean,
    thinkingEnabled: Boolean,
    onToggleThinking: () -> Unit,
    searchEnabled: Boolean,
    onToggleSearch: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = White,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f)
                        .background(LightGray, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    textStyle = TextStyle(fontSize = 14.sp, color = DarkText),
                    decorationBox = { inner ->
                        Box { if (messageText.isEmpty()) Text("Mensaje...", color = GrayText, fontSize = 14.sp); inner() }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(
                    onClick = onSend,
                    modifier = Modifier.size(42.dp).background(
                        if (messageText.isNotBlank()) LightBlue else GrayBorder, CircleShape
                    )
                ) {
                    if (isStreaming) {
                        Icon(Icons.Rounded.Stop, "Detener", White, Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Rounded.Send, "Enviar",
                            tint = if (messageText.isNotBlank()) White else GrayText,
                            modifier = Modifier.size(20.dp).rotate(-45f))
                    }
                }
            }
            
            // Botones debajo
            Row(modifier = Modifier.padding(top = 6.dp)) {
                Surface(
                    onClick = onToggleThinking,
                    modifier = Modifier.padding(end = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (thinkingEnabled) LightBlue.copy(alpha = 0.12f) else LightGray,
                    border = BorderStroke(1.dp, if (thinkingEnabled) LightBlue.copy(alpha = 0.4f) else GrayBorder)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Psychology, null, Modifier.size(14.dp), tint = if (thinkingEnabled) LightBlue else GrayText)
                        Spacer(Modifier.width(4.dp))
                        Text("Pensar", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (thinkingEnabled) LightBlue else GrayText)
                    }
                }
                
                Surface(
                    onClick = onToggleSearch,
                    shape = RoundedCornerShape(16.dp),
                    color = if (searchEnabled) LightBlue.copy(alpha = 0.12f) else LightGray,
                    border = BorderStroke(1.dp, if (searchEnabled) LightBlue.copy(alpha = 0.4f) else GrayBorder)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Language, null, Modifier.size(14.dp), tint = if (searchEnabled) LightBlue else GrayText)
                        Spacer(Modifier.width(4.dp))
                        Text("Buscar", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (searchEnabled) LightBlue else GrayText)
                    }
                }
            }
        }
    }
}

sealed class MarkdownBlock {
    data class Text(val text: String) : MarkdownBlock()
    data class Code(val code: String, val language: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val codeRegex = Regex("```(\\w*)\\n([\\s\\S]*?)```")
    var lastIndex = 0
    
    codeRegex.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            val before = text.substring(lastIndex, match.range.first)
            if (before.isNotBlank()) blocks.add(MarkdownBlock.Text(cleanInlineMarkdown(before)))
        }
        blocks.add(MarkdownBlock.Code(match.groupValues[2].trim(), match.groupValues[1].ifEmpty { "code" }))
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < text.length) {
        val after = text.substring(lastIndex)
        if (after.isNotBlank()) blocks.add(MarkdownBlock.Text(cleanInlineMarkdown(after)))
    }
    
    if (blocks.isEmpty() && text.isNotBlank()) blocks.add(MarkdownBlock.Text(cleanInlineMarkdown(text)))
    return blocks
}

fun cleanInlineMarkdown(text: String): String {
    return text
        .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("\\*(.*?)\\*"), "$1")
        .replace(Regex("`(.*?)`"), "$1")
        .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
        .replace(Regex("^#{1,3}\\s+", RegexOption.MULTILINE), "")
        .replace(Regex("^\\s*\\*\\s+", RegexOption.MULTILINE), "- ")
        .trim()
}
