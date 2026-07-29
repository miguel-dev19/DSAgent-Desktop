package com.dsagent.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class HistoryManager {
    private val historyFile = File(System.getProperty("user.home"), ".dsagent_history.json")
    
    fun loadHistory(): List<ChatHistory> {
        if (!historyFile.exists()) return emptyList()
        val json = JSONArray(historyFile.readText())
        return (0 until json.length()).map { i ->
            val obj = json.getJSONObject(i)
            ChatHistory(
                id = obj.getString("id"),
                title = obj.getString("title"),
                lastMessage = obj.optString("lastMessage", ""),
                timestamp = obj.optLong("timestamp", 0)
            )
        }
    }
    
    fun saveChat(chat: ChatHistory) {
        val history = loadHistory().toMutableList()
        history.removeAll { it.id == chat.id }
        history.add(0, chat)
        if (history.size > 50) history.removeAt(history.lastIndex)
        
        val json = JSONArray()
        history.forEach { h ->
            json.put(JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("lastMessage", h.lastMessage)
                put("timestamp", h.timestamp)
            })
        }
        historyFile.writeText(json.toString(2))
    }
    
    fun deleteChat(id: String) {
        val history = loadHistory().filter { it.id != id }
        val json = JSONArray()
        history.forEach { h ->
            json.put(JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("lastMessage", h.lastMessage)
                put("timestamp", h.timestamp)
            })
        }
        historyFile.writeText(json.toString(2))
    }
}

data class ChatHistory(
    val id: String,
    val title: String,
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
