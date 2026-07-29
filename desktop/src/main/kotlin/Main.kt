package com.dsagent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

// Colores
val White = Color(0xFFFFFFFF)
val LightBlue = Color(0xFF4FC3F7)
val LightBlueVariant = Color(0xFF29B6F6)
val DarkText = Color(0xFF1E293B)
val GrayText = Color(0xFF64748B)
val LightGray = Color(0xFFF1F5F9)
val GrayBorder = Color(0xFFE2E8F0)
val ErrorRed = Color(0xFFFF6B6B)
val CodeBackground = Color(0xFF1E293B)

fun main() = application {
    val client = remember { DeepSeekClient() }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentMessage by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamedText by remember { mutableStateOf("") }
    var thinkingText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var thinkingEnabled by remember { mutableStateOf(true) }
    var searchEnabled by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        client.createSession()
    }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "DSAgent Desktop",
        state = WindowState(
            width = 900.dp,
            height = 700.dp,
            position = WindowPosition.Aligned(Alignment.Center)
        )
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = LightBlue,
                background = White,
                surface = White
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().background(White)) {
                // Barra superior
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = White,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "DeepSeek Agent Desktop",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DarkText
                        )
                        
                        Row {
                            // Boton Pensar
                            FilterChip(
                                selected = thinkingEnabled,
                                onClick = { thinkingEnabled = !thinkingEnabled },
                                label = { Text("Pensar") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Psychology, null, Modifier.size(16.dp))
                                },
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            
                            // Boton Buscar
                            FilterChip(
                                selected = searchEnabled,
                                onClick = { searchEnabled = !searchEnabled },
                                label = { Text("Buscar") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Language, null, Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }
                
                // Area de chat
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    reverseLayout = true
                ) {
                    // Mensaje en streaming
                    if (streamedText.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = Color.Transparent
                            ) {
                                Text(streamedText, style = MaterialTheme.typography.bodyMedium, color = DarkText)
                            }
                        }
                    }
                    
                    // Pensando
                    if (isThinking) {
                        item {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = LightBlue, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Pensando...", color = LightBlue, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    
                    items(messages.reversed()) { msg ->
                        when (msg) {
                            is ChatMessage.User -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Surface(
                                        modifier = Modifier.padding(vertical = 4.dp).widthIn(max = 400.dp),
                                        color = LightGray,
                                        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                                    ) {
                                        Text(msg.text, modifier = Modifier.padding(12.dp), color = DarkText)
                                    }
                                }
                            }
                            is ChatMessage.AI -> {
                                Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), color = Color.Transparent) {
                                    Text(msg.text, color = DarkText)
                                }
                            }
                        }
                    }
                }
                
                // Barra de entrada
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = currentMessage,
                            onValueChange = { currentMessage = it },
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                            textStyle = TextStyle(fontSize = 15.sp, color = DarkText),
                            decorationBox = { inner ->
                                Box {
                                    if (currentMessage.isEmpty()) Text("Mensaje...", color = GrayText)
                                    inner()
                                }
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (currentMessage.isNotBlank() && !isStreaming) {
                                        val msg = currentMessage
                                        messages = messages + ChatMessage.User(msg)
                                        currentMessage = ""
                                        isStreaming = true
                                        isThinking = true
                                        thinkingText = ""
                                        streamedText = ""
                                        
                                        CoroutineScope(Dispatchers.IO).launch {
                                            var fullResponse = ""
                                            client.streamResponse(msg, thinkingEnabled, searchEnabled).collect { event ->
                                                when (event) {
                                                    is StreamEvent.Thinking -> {
                                                        thinkingText += event.text
                                                    }
                                                    is StreamEvent.Response -> {
                                                        fullResponse += event.text
                                                        isThinking = false
                                                        streamedText = fullResponse
                                                    }
                                                    is StreamEvent.Done -> {
                                                        messages = messages + ChatMessage.AI(fullResponse)
                                                        isStreaming = false
                                                        streamedText = ""
                                                        thinkingText = ""
                                                    }
                                                    is StreamEvent.Error -> {
                                                        isStreaming = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        )
                        
                        IconButton(
                            onClick = { /* enviar */ },
                            modifier = Modifier.size(40.dp).background(LightBlue, CircleShape)
                        ) {
                            Icon(Icons.Rounded.Send, "Enviar", White, Modifier.size(20.dp).rotate(-45f))
                        }
                    }
                }
            }
        }
    }
}

// Cliente API
class DeepSeekClient {
    private val httpClient = HttpClient(CIO)
    private var sessionId: String? = null
    
    suspend fun createSession() {
        val response = httpClient.post("https://ds-flaskapi.onrender.com/api/session")
        val json = JSONObject(response.bodyAsText())
        sessionId = json.getString("session_id")
    }
    
    fun streamResponse(
        message: String,
        thinkingEnabled: Boolean = true,
        searchEnabled: Boolean = true
    ): Flow<StreamEvent> = flow {
        val json = JSONObject().apply {
            put("session_id", sessionId)
            put("prompt", message)
            put("thinking_enabled", thinkingEnabled)
            put("search_enabled", searchEnabled)
        }
        
        val response = httpClient.post("https://ds-flaskapi.onrender.com/api/chat") {
            setBody(json.toString())
            headers { append("Content-Type", "application/json") }
        }
        
        val reader = BufferedReader(InputStreamReader(response.bodyAsChannel().toInputStream()))
        var currentEvent = ""
        
        reader.useLines { lines ->
            lines.forEach { line ->
                when {
                    line.startsWith("event: ") -> currentEvent = line.removePrefix("event: ").trim()
                    line.startsWith("data: ") -> {
                        val data = line.removePrefix("data: ").trim()
                        if (data.isNotEmpty() && data != "\"\"") {
                            val cleaned = data.trim('"').replace(Regex("\\s*FINISHED\\s*", RegexOption.IGNORE_CASE), "")
                            if (cleaned.isEmpty()) return@forEach
                            when (currentEvent) {
                                "think" -> emit(StreamEvent.Thinking(cleaned))
                                "response" -> emit(StreamEvent.Response(cleaned))
                                "done" -> emit(StreamEvent.Done(cleaned))
                                "error" -> emit(StreamEvent.Error(cleaned))
                            }
                        }
                    }
                }
            }
        }
    }
}

sealed class StreamEvent {
    data class Thinking(val text: String) : StreamEvent()
    data class Response(val text: String) : StreamEvent()
    data class Done(val messageId: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}

sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class AI(val text: String) : ChatMessage()
}
