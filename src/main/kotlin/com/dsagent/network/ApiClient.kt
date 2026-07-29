package com.dsagent.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class ApiClient {
    private val httpClient = HttpClient(CIO)
    private var sessionId: String? = null
    
    suspend fun createSession(): Result<String> {
        return try {
            val response = httpClient.post("https://ds-flaskapi.onrender.com/api/session")
            val json = JSONObject(response.bodyAsText())
            sessionId = json.getString("session_id")
            Result.success(sessionId!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getSessionId() = sessionId
    
    private fun cleanText(text: String): String {
        return text
            .replace(Regex("\\s*FINISHED\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("[\\x{1F600}-\\x{1F64F}]"), "")
            .replace(Regex("[\\x{1F300}-\\x{1F5FF}]"), "")
            .replace(Regex("[\\x{1F680}-\\x{1F6FF}]"), "")
            .replace(Regex("[\\x{1F1E0}-\\x{1F1FF}]"), "")
            .replace(Regex("[\\x{2600}-\\x{26FF}]"), "")
            .replace(Regex("[\\x{2700}-\\x{27BF}]"), "")
            .replace(Regex("[\\x{FE00}-\\x{FE0F}]"), "")
            .trim()
    }
    
    fun streamResponse(
        message: String,
        thinkingEnabled: Boolean = true,
        searchEnabled: Boolean = true,
        parentId: String? = null
    ): Flow<StreamEvent> = flow {
        try {
            val json = JSONObject().apply {
                put("session_id", sessionId)
                put("prompt", message)
                put("thinking_enabled", thinkingEnabled)
                put("search_enabled", searchEnabled)
                if (parentId != null) put("parent_message_id", parentId)
            }
            
            val response = httpClient.post("https://ds-flaskapi.onrender.com/api/chat") {
                setBody(json.toString())
                headers { append("Content-Type", "application/json") }
            }
            
            val reader = BufferedReader(InputStreamReader(response.bodyAsChannel().toInputStream()))
            var currentEvent = ""
            var isFirstResponse = true
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    when {
                        line.startsWith("event: ") -> {
                            currentEvent = line.removePrefix("event: ").trim()
                            if (currentEvent == "response") isFirstResponse = true
                        }
                        line.startsWith("data: ") -> {
                            val data = line.removePrefix("data: ").trim()
                            if (data.isNotEmpty() && data != "\"\"") {
                                var cleaned = data.trim('"')
                                cleaned = cleanText(cleaned)
                                if (cleaned.isEmpty()) return@forEach
                                
                                if (currentEvent == "response" && !isFirstResponse) {
                                    val lastChar = cleaned.firstOrNull()
                                    if (lastChar != null && !lastChar.isWhitespace() &&
                                        lastChar != '.' && lastChar != ',' &&
                                        lastChar != '!' && lastChar != '?' &&
                                        lastChar != ':' && lastChar != ';' &&
                                        lastChar != '\n') {
                                        cleaned = " $cleaned"
                                    }
                                }
                                if (currentEvent == "response") isFirstResponse = false
                                
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
        } catch (e: Exception) {
            emit(StreamEvent.Error(e.message ?: "Error desconocido"))
        }
    }
}

sealed class StreamEvent {
    data class Thinking(val text: String) : StreamEvent()
    data class Response(val text: String) : StreamEvent()
    data class Done(val messageId: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
