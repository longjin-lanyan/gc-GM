package com.genshin.gm.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.genshin.gm.ui.screen.*
import com.genshin.gm.ui.theme.GenshinGMTheme

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

    // Show message as snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        if (state.message.isNotEmpty()) {
            snackbarHostState.showSnackbar(state.message)
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("原神GM工具") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = screen.icon,
                        label = { Text(screen.title) }
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
