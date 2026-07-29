package com.dsagent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.dsagent.data.*
import com.dsagent.network.*
import com.dsagent.ui.components.*
import com.dsagent.ui.theme.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = application {
    val client = remember { ApiClient() }
    val historyManager = remember { HistoryManager() }
    
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentMessage by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamedText by remember { mutableStateOf("") }
    var thinkingText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var thinkingEnabled by remember { mutableStateOf(true) }
    var searchEnabled by remember { mutableStateOf(true) }
    var chatTitle by remember { mutableStateOf("Nuevo Chat") }
    var darkTheme by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(historyManager.loadHistory()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scrollState = rememberLazyListState()
    
    LaunchedEffect(Unit) {
        client.createSession()
    }
    
    // Auto-scroll
    LaunchedEffect(streamedText, messages.size) {
        if (messages.isNotEmpty() || streamedText.isNotEmpty()) {
            scrollState.animateScrollToItem(scrollState.layoutInfo.totalItemsCount - 1)
        }
    }
    
    fun sendMessage() {
        val msg = currentMessage
        if (msg.isBlank() || isStreaming) return
        
        messages = messages + ChatMessage.User(msg)
        currentMessage = ""
        isStreaming = true
        isThinking = true
        thinkingText = ""
        streamedText = ""
        errorMessage = null
        
        if (chatTitle == "Nuevo Chat") {
            chatTitle = msg.take(40)
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            var fullResponse = ""
            client.streamResponse(msg, thinkingEnabled, searchEnabled).collect { event ->
                when (event) {
                    is StreamEvent.Thinking -> { thinkingText += event.text }
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
                        
                        // Guardar en historial
                        historyManager.saveChat(
                            ChatHistory(
                                id = client.getSessionId() ?: "",
                                title = chatTitle,
                                lastMessage = fullResponse.take(80)
                            )
                        )
                        history = historyManager.loadHistory()
                    }
                    is StreamEvent.Error -> {
                        errorMessage = event.message
                        isStreaming = false
                    }
                }
            }
        }
    }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "DSAgent Desktop",
        state = WindowState(width = 1000.dp, height = 700.dp, position = WindowPosition.Aligned(Alignment.Center))
    ) {
        DSAgentTheme(darkTheme = darkTheme) {
            val bg = if (darkTheme) DarkBackground else White
            
            Column(modifier = Modifier.fillMaxSize().background(bg)) {
                // Barra superior
                Surface(modifier = Modifier.fillMaxWidth(), color = if (darkTheme) DarkSurface else White, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showHistory = !showHistory }) {
                            Icon(Icons.Outlined.Menu, "Historial", if (darkTheme) DarkTextLight else DarkText)
                        }
                        
                        Text(
                            chatTitle, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (darkTheme) DarkTextLight else DarkText,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        
                        IconButton(onClick = {
                            messages = emptyList()
                            streamedText = ""
                            thinkingText = ""
                            chatTitle = "Nuevo Chat"
                            errorMessage = null
                            client.createSession()
                        }) {
                            Icon(Icons.Outlined.Edit, "Nuevo chat", LightBlue)
                        }
                        
                        IconButton(onClick = { darkTheme = !darkTheme }) {
                            Icon(
                                if (darkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                "Tema", GrayText
                            )
                        }
                    }
                }
                
                // Panel lateral + Chat
                Row(modifier = Modifier.weight(1f)) {
                    // Panel historial
                    if (showHistory) {
                        Surface(
                            modifier = Modifier.width(260.dp).fillMaxHeight(),
                            color = if (darkTheme) DarkSurface else White,
                            shadowElevation = 2.dp
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Historial", fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = if (darkTheme) DarkTextLight else DarkText)
                                Spacer(Modifier.height(8.dp))
                                Divider()
                                
                                if (history.isEmpty()) {
                                    Text("Sin conversaciones", color = GrayText, fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 16.dp))
                                } else {
                                    LazyColumn {
                                        items(history) { chat ->
                                            Surface(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                                                    .clickable { /* Cargar chat */ },
                                                color = Color.Transparent
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text(chat.title, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                                        color = if (darkTheme) DarkTextLight else DarkText,
                                                        maxLines = 1)
                                                    Text(chat.lastMessage, fontSize = 11.sp, color = GrayText, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Area chat
                    Column(modifier = Modifier.weight(1f)) {
                        if (errorMessage != null) {
                            Surface(modifier = Modifier.fillMaxWidth(), color = ErrorRed.copy(alpha = 0.1f)) {
                                Text(errorMessage!!, modifier = Modifier.padding(8.dp), color = ErrorRed, fontSize = 12.sp)
                            }
                        }
                        
                        if (messages.isEmpty() && !isStreaming) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Outlined.Forum, null, Modifier.size(64.dp), LightBlue.copy(alpha = 0.5f))
                                    Spacer(Modifier.height(16.dp))
                                    Text("En que puedo ayudarte?", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                        color = if (darkTheme) DarkTextLight else DarkText)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Escribe tu pregunta", fontSize = 14.sp, color = GrayText)
                                }
                            }
                        } else {
                            LazyColumn(
                                state = scrollState,
                                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(messages) { msg ->
                                    when (msg) {
                                        is ChatMessage.User -> UserBubble(msg.text)
                                        is ChatMessage.AI -> AIResponse(msg.text)
                                    }
                                }
                                
                                if (isThinking) item { ThinkingIndicator() }
                                if (streamedText.isNotEmpty()) item { AIResponse(streamedText) }
                                item { Spacer(Modifier.height(8.dp)) }
                            }
                        }
                        
                        ChatInput(
                            messageText = currentMessage,
                            onMessageChange = { currentMessage = it },
                            onSend = { sendMessage() },
                            isStreaming = isStreaming,
                            thinkingEnabled = thinkingEnabled,
                            onToggleThinking = { thinkingEnabled = !thinkingEnabled },
                            searchEnabled = searchEnabled,
                            onToggleSearch = { searchEnabled = !searchEnabled }
                        )
                    }
                }
            }
        }
    }
}

sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class AI(val text: String) : ChatMessage()
}
