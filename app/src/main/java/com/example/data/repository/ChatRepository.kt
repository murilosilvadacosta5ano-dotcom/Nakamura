package com.example.data.repository

import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.InlineData
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.ChatDao
import com.example.data.db.ChatMessage
import com.example.data.db.ChatSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ChatRepository(private val chatDao: ChatDao) {

    val allSessions: Flow<List<ChatSession>> = chatDao.getAllSessions()

    fun getMessagesForSession(sessionId: Long): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForSession(sessionId)
    }

    suspend fun createNewSession(title: String, modelName: String): Long = withContext(Dispatchers.IO) {
        val session = ChatSession(
            title = title,
            lastModified = System.currentTimeMillis(),
            modelName = modelName
        )
        chatDao.insertSession(session)
    }

    suspend fun updateSessionTitle(sessionId: Long, title: String) = withContext(Dispatchers.IO) {
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(title = title, lastModified = System.currentTimeMillis()))
        }
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        chatDao.deleteSessionById(sessionId)
    }

    suspend fun insertMessage(message: ChatMessage): Long = withContext(Dispatchers.IO) {
        // Update session's last modified timestamp
        val session = chatDao.getSessionById(message.sessionId)
        if (session != null) {
            chatDao.updateSession(session.copy(lastModified = System.currentTimeMillis()))
        }
        chatDao.insertMessage(message)
    }

    suspend fun getSessionById(sessionId: Long): ChatSession? = withContext(Dispatchers.IO) {
        chatDao.getSessionById(sessionId)
    }

    suspend fun sendPromptToGemini(sessionId: Long, modelName: String, systemInstruction: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.startsWith("MY_")) {
            val configError = "Gemini API Key is not configured. Please add your API key in the Secrets panel in Google AI Studio to chat with Gemini."
            // Save as an error message in chat history so the user receives a localized visual cue
            insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    role = "error",
                    content = configError
                )
            )
            return@withContext Result.failure(Exception(configError))
        }

        // Retrieve existing history excluding preceding "error" role messages to prevent model injection errors
        val messagesSummary = chatDao.getMessagesForSessionSync(sessionId)
            .filter { it.role == "user" || it.role == "model" }

        val contents = messagesSummary.map { msg ->
            val parts = mutableListOf<Part>()
            if (!msg.imageBase64.isNullOrEmpty()) {
                parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = msg.imageBase64)))
            }
            if (msg.content.isNotEmpty()) {
                parts.add(Part(text = msg.content))
            }
            Content(
                parts = parts,
                role = when (msg.role) {
                    "user" -> "user"
                    "model" -> "model"
                    else -> "user"
                }
            )
        }

        if (contents.isEmpty()) {
            return@withContext Result.failure(Exception("Cannot send empty chat content."))
        }

        val systemInstructionContent = systemInstruction?.let {
            Content(parts = listOf(Part(text = it)))
        }

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = systemInstructionContent
        )
        try {
            val response = RetrofitClient.service.generateContent(modelName, apiKey, request)
            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (replyText != null) {
                // Save model's reply
                insertMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        role = "model",
                        content = replyText
                    )
                )
                Result.success(replyText)
            } else {
                val errorMsg = "Received empty response from the model. Please check constraints."
                insertMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        role = "error",
                        content = errorMsg
                    )
                )
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown API or Network error"
            insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    role = "error",
                    content = "Error: $errorMsg"
                )
            )
            Result.failure(e)
        }
    }

    suspend fun clearAllSessions() = withContext(Dispatchers.IO) {
        chatDao.deleteAllSessionsSync()
    }

    suspend fun getOtherSessionsMemoryContext(excludeSessionId: Long): String = withContext(Dispatchers.IO) {
        val allOtherSessions = chatDao.getOtherSessionsSync(excludeSessionId).take(3)
        val sb = java.lang.StringBuilder()
        for (session in allOtherSessions) {
            val msgs = chatDao.getMessagesForSessionSync(session.id)
                .filter { it.role == "user" || it.role == "model" }
                .takeLast(6)
            if (msgs.isNotEmpty()) {
                sb.append("Conversa anterior: \"${session.title}\":\n")
                msgs.forEach { m ->
                    val roleName = if (m.role == "user") "Usuário" else "Nakamura IA"
                    sb.append("  - $roleName: ${if (m.content.length > 100) m.content.take(100) + "..." else m.content}\n")
                }
                sb.append("\n")
            }
        }
        sb.toString()
    }

    suspend fun sendWikipediaPromptToGemini(sessionId: Long, modelName: String, queryWithWikiText: String, systemInstruction: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey.startsWith("MY_")) {
            val configError = "Gemini API Key is not configured."
            return@withContext Result.failure(Exception(configError))
        }

        val messagesSummary = chatDao.getMessagesForSessionSync(sessionId)
            .filter { it.role == "user" || it.role == "model" }
            
        val contents = messagesSummary.mapIndexed { idx, msg ->
            val parts = mutableListOf<Part>()
            if (msg.role == "user" && idx == messagesSummary.lastIndex) {
                parts.add(Part(text = queryWithWikiText))
            } else {
                if (!msg.imageBase64.isNullOrEmpty()) {
                    parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = msg.imageBase64)))
                }
                parts.add(Part(text = msg.content))
            }
            Content(
                parts = parts,
                role = when (msg.role) {
                    "user" -> "user"
                    "model" -> "model"
                    else -> "user"
                }
            )
        }

        if (contents.isEmpty()) {
            return@withContext Result.failure(Exception("Cannot send empty content."))
        }

        val systemInstructionContent = systemInstruction?.let {
            Content(parts = listOf(Part(text = it)))
        }

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = systemInstructionContent
        )
        try {
            val response = RetrofitClient.service.generateContent(modelName, apiKey, request)
            val replyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (replyText != null) {
                insertMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        role = "model",
                        content = replyText
                    )
                )
                Result.success(replyText)
            } else {
                val errorMsg = "Received empty response of model."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown API or Network error"
            insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    role = "error",
                    content = "Error: $errorMsg"
                )
            )
            Result.failure(e)
        }
    }
}
