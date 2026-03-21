package com.genshin.gm.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.genshin.gm.ui.component.GlassGradient
import com.genshin.gm.ui.screen.*
import com.genshin.gm.ui.theme.GenshinGMTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GenshinGMTheme {
                MainApp()
            }
        }
    }
}

enum class Screen(val title: String, val icon: @Composable () -> Unit) {
    HOME("首页", { Icon(Icons.Default.Home, "首页") }),
    COMMANDS("指令", { Icon(Icons.Default.Terminal, "指令") }),
    EXECUTE("执行", { Icon(Icons.Default.PlayArrow, "执行") }),
    ACCOUNT("账户", { Icon(Icons.Default.Person, "账户") }),
    SETTINGS("设置", { Icon(Icons.Default.Settings, "设置") }),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        if (state.message.isNotEmpty()) {
            snackbarHostState.showSnackbar(state.message)
            vm.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background: image or gradient
        val bgPath = state.backgroundImagePath
        if (bgPath != null && File(bgPath).exists()) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current)
                        .data(File(bgPath))
                        .crossfade(true)
                        .build()
                ),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GlassGradient)
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "原神GM工具",
                            color = Color.White
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1A1B2E).copy(alpha = 0.6f)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF1A1B2E).copy(alpha = 0.85f),
                    contentColor = Color.White
                ) {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = { currentScreen = screen },
                            icon = screen.icon,
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = Color(0xFF667EEA).copy(alpha = 0.4f),
                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (currentScreen) {
                    Screen.HOME -> HomeScreen(vm, state)
                    Screen.COMMANDS -> CommandsScreen(vm, state)
                    Screen.EXECUTE -> ExecuteScreen(vm, state)
                    Screen.ACCOUNT -> AccountScreen(vm, state)
                    Screen.SETTINGS -> SettingsScreen(vm, state)
                }
            }
        }
    }
}
