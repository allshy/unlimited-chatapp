package com.wuxianzhi.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wuxianzhi.chat.ui.screen.ChatScreen
import com.wuxianzhi.chat.ui.screen.ConversationListScreen
import com.wuxianzhi.chat.ui.screen.SettingsScreen
import com.wuxianzhi.chat.ui.theme.WuxianzhiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WuxianzhiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AppNav()
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppNav() {
    val nav = rememberNavController()
    val app = App.instance
    val apiKey by app.settings.apiKey.collectAsState()
    val startRoute = if (apiKey.isBlank()) "settings?firstRun=true" else "list"

    NavHost(navController = nav, startDestination = startRoute) {
        composable("list") {
            ConversationListScreen(
                onOpen = { id -> nav.navigate("chat/$id") },
                onNew = { id -> nav.navigate("chat/$id") },
                onSettings = { nav.navigate("settings?firstRun=false") },
            )
        }
        composable("chat/{id}") { backStack ->
            val id = backStack.arguments?.getString("id") ?: return@composable
            ChatScreen(
                conversationId = id,
                onBack = { nav.popBackStack() },
                onSettings = { nav.navigate("settings?firstRun=false") },
            )
        }
        composable("settings?firstRun={firstRun}") { backStack ->
            val firstRun = backStack.arguments?.getString("firstRun") == "true"
            SettingsScreen(
                firstRun = firstRun,
                onDone = {
                    if (firstRun) {
                        nav.navigate("list") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        nav.popBackStack()
                    }
                },
            )
        }
    }
}
