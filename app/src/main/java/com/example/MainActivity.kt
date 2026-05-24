package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.db.GeminiDatabase
import com.example.data.repository.ChatRepository
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.WelcomeSplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory

class MainActivity : ComponentActivity() {

    // Initialize Room Database lazily to conserve memory and execute cleanly on JVM
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            GeminiDatabase::class.java,
            "gemini_chat_store.db"
        ).build()
    }

    // Initialize Repository holding the DAOs to respect clean architectural boundaries
    private val repository by lazy {
        ChatRepository(database.chatDao)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    WelcomeSplashScreen(
                        onTimeout = { showSplash = false },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Instantiates the ViewModel using standard constructor injection pattern
                    val chatViewModel: ChatViewModel = viewModel(
                        factory = ChatViewModelFactory(repository)
                    )

                    ChatScreen(
                        viewModel = chatViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
