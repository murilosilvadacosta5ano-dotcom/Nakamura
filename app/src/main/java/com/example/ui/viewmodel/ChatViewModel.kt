package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.ChatMessage
import com.example.data.db.ChatSession
import com.example.data.repository.ChatRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _currentSessionId = MutableStateFlow<Long?>(null)
    val currentSessionId: StateFlow<Long?> = _currentSessionId.asStateFlow()

    private val _currentModel = MutableStateFlow("gemini-3.5-flash")
    val currentModel: StateFlow<String> = _currentModel.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _selectedImageBase64 = MutableStateFlow<String?>(null)
    val selectedImageBase64: StateFlow<String?> = _selectedImageBase64.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _importMemoryEnabled = MutableStateFlow(false)
    val importMemoryEnabled: StateFlow<Boolean> = _importMemoryEnabled.asStateFlow()

    fun setImportMemoryEnabled(enabled: Boolean) {
        _importMemoryEnabled.value = enabled
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllSessions()
            _currentSessionId.value = null
        }
    }

    // Personality configuration
    private val _selectedPersonality = MutableStateFlow("IA")
    val selectedPersonality: StateFlow<String> = _selectedPersonality.asStateFlow()

    // Search query for filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Multi-turn active session filtering
    private val _sessions = repository.allSessions
    val sessions: StateFlow<List<ChatSession>> = kotlinx.coroutines.flow.combine(_sessions, _searchQuery) { sessionList, query ->
        if (query.isBlank()) {
            sessionList
        } else {
            sessionList.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val messages: StateFlow<List<ChatMessage>> = _currentSessionId.flatMapLatest { sessionId ->
        if (sessionId != null) {
            repository.getMessagesForSession(sessionId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectSession(sessionId: Long?) {
        _currentSessionId.value = sessionId
        viewModelScope.launch {
            if (sessionId != null) {
                val session = repository.getSessionById(sessionId)
                if (session != null) {
                    _currentModel.value = session.modelName
                }
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                _currentSessionId.value = null
            }
        }
    }

    fun setModel(modelName: String) {
        _currentModel.value = modelName
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun attachImage(context: Context, uri: Uri) {
        _selectedImageUri.value = uri
        viewModelScope.launch {
            val base64 = uriToBase64(context, uri)
            _selectedImageBase64.value = base64
        }
    }

    fun removeAttachment() {
        _selectedImageUri.value = null
        _selectedImageBase64.value = null
    }

    private fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun setPersonality(personality: String) {
        _selectedPersonality.value = personality
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun renameSession(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            repository.updateSessionTitle(sessionId, newTitle)
        }
    }

    fun sendMessage(activeModeExtension: String? = null) {
        val text = _inputText.value.trim()
        val base64 = _selectedImageBase64.value
        if (text.isEmpty() && base64 == null) return

        _inputText.value = ""
        removeAttachment()

        viewModelScope.launch {
            var sessionId = _currentSessionId.value
            val modelName = _currentModel.value

            if (sessionId == null) {
                // Determine title from prompt text
                val title = if (text.isNotEmpty()) {
                    if (text.length > 30) text.take(30) + "..." else text
                } else {
                    "Multimodal Scan"
                }
                sessionId = repository.createNewSession(title, modelName)
                _currentSessionId.value = sessionId
            }

            // Save user's message
            repository.insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    role = "user",
                    content = text,
                    imageBase64 = base64
                )
            )

            // 1. IMAGE MODE / KEYWORDS SYNTHESIS DETECTOR:
            val lowerText = text.lowercase()
            val hasImageKeyword = lowerText.contains("gerar") || 
                                  lowerText.contains("crie uma imagem") || 
                                  lowerText.contains("faça uma imagem") || 
                                  lowerText.contains("redesenhe") || 
                                  lowerText.contains("mistura essas fotos") || 
                                  lowerText.contains("chibi") || 
                                  lowerText.contains("generate image") || 
                                  lowerText.contains("create image")

            if (activeModeExtension == "image" || hasImageKeyword) {
                _isGenerating.value = true
                kotlinx.coroutines.delay(1200) // Delay to let the cute thinking animation render!
                val cleanPrompt = text.replace("gerar imagem", "")
                                      .replace("gerar", "")
                                      .replace("crie uma imagem", "")
                                      .replace("faça uma imagem", "").trim()
                val promptQuery = if (cleanPrompt.isEmpty()) "futuristic cyberpunk city neon pink" else cleanPrompt
                val encodedPrompt = java.net.URLEncoder.encode(promptQuery, "UTF-8")
                val imageUrl = "https://image.pollinations.ai/prompt/$encodedPrompt?width=800&height=800&nologo=true"
                
                repository.insertMessage(
                    ChatMessage(
                        sessionId = sessionId,
                        role = "model",
                        content = imageUrl
                    )
                )
                _isGenerating.value = false
                return@launch
            }

            // 2. WIKIPÉDIA MODE SEARCH & INTEGRATION:
            var targetTextToSendOfGemini = text
            if (activeModeExtension == "wikipedia") {
                _isGenerating.value = true
                val wikiSummary = fetchWikipediaSummary(text)
                targetTextToSendOfGemini = "Com base no seguinte resumo obtido da Wikipédia: \"$wikiSummary\". Por favor, responda ou resuma inteligentemente à pergunta do usuário: $text"
            }

            // Resolve modern personality prompt instruction matching user intent perfectly
            var sysInstruction = when (_selectedPersonality.value) {
                "Modo História" -> "Você é um contador de histórias imersivo e criativo. Escreva de forma narrativa, detalhada, dramática, envolvente e artística, como se escrevesse uma jornada literária."
                "Agente" -> "Você age como um agente autônomo focado em objetivos operacionais. Divida suas respostas em etapas claras, orientadas a ações práticas, passos lógicos e resultados úteis em formato de tópicos."
                "Personagem" -> "Incorpore um personagem fictício expressivo, simpático e teatral. Fale de forma vívida, use diálogos envolventes, gírias amigáveis se apropriado e expresse reações e sentimentos entre asteriscos (ex: *sorri animado*, *olha intrigado*)."
                "Professor" -> "Você é um professor empático, altamente didático, compreensivo e paciente. Explique os conceitos passo a passo com exemplos práticos simples, analogias fáceis de visualizar e organize os tópicos de forma acadêmica e educacional."
                "Interpretador" -> "Aja como um interpretador analítico refinado e experiente. Traduza os significados ocultos nos textos pesquisados, leia as entrelinhas com profundidade, analise a semântica de forma técnica e forneça insights estruturados profundos."
                else -> "Você é a Nakamura IA, desenvolvido pelo programador Nakamura. Você é um assistente virtual inteligente de nova geração, prestativo, preciso e amigável. Responda de forma direta, clara, acolhedora e inteligente."
            }

            if (_importMemoryEnabled.value) {
                val memoryText = repository.getOtherSessionsMemoryContext(sessionId)
                if (memoryText.isNotEmpty()) {
                    sysInstruction += "\n\n[MEMÓRIA CONTEXTUAL ADICIONAL - Use este resumo curto de outras conversas passadas do usuário para responder de forma mais personalizada e contextualizada]:\n$memoryText"
                }
            }

            // Trigger Gemini generating text with contextual instruction
            _isGenerating.value = true
            
            // Custom repository wrapper to pass modified target text
            if (activeModeExtension == "wikipedia") {
                // If wikipedia, we can insert the query, fetch, and send to repository
                repository.sendWikipediaPromptToGemini(sessionId, modelName, targetTextToSendOfGemini, sysInstruction)
            } else {
                repository.sendPromptToGemini(sessionId, modelName, sysInstruction)
            }
            
            _isGenerating.value = false
        }
    }

    private suspend fun fetchWikipediaSummary(query: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val cleanQuery = query.replace("pesquisar", "")
                                  .replace("wikipedia", "")
                                  .replace("wikipédia", "").trim()
            val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val url = java.net.URL("https://pt.wikipedia.org/api/rest_v1/page/summary/$encoded")
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            
            if (conn.responseCode == 200) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val extractMarker = "\"extract\":\""
                val index = responseText.indexOf(extractMarker)
                if (index != -1) {
                    val start = index + extractMarker.length
                    val end = responseText.indexOf("\"", start)
                    if (end != -1) {
                        return@withContext responseText.substring(start, end)
                            .replace("\\u00a0", " ")
                            .replace("\\u2013", "-")
                            .replace("\\\"", "\"")
                    }
                }
            }
            "Nenhum resumo enciclopédico exato foi encontrado na Wikipédia para '$cleanQuery'."
        } catch (e: Exception) {
            "Falha técnica ao acessar a base da Wikipédia: ${e.message}"
        }
    }

    // Insert locally generated images into active Room database chat flow
    fun insertLocalGeneratedMessage(prompt: String, base64Image: String) {
        viewModelScope.launch {
            var sessionId = _currentSessionId.value
            val modelName = _currentModel.value

            if (sessionId == null) {
                val title = if (prompt.length > 30) prompt.take(30) + "..." else prompt
                sessionId = repository.createNewSession(title, modelName)
                _currentSessionId.value = sessionId
            }

            // Save user prompt message
            repository.insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    role = "user",
                    content = "Gerar imagem: $prompt"
                )
            )

            // Save model image message
            repository.insertMessage(
                ChatMessage(
                    sessionId = sessionId,
                    role = "model",
                    content = "Aqui está a sua imagem gerada com Nakamura IA!",
                    imageBase64 = base64Image
                )
            )
        }
    }

    fun startNewChat() {
        _currentSessionId.value = null
        _inputText.value = ""
        removeAttachment()
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
