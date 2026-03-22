package com.genshin.gm.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import android.content.Intent
import android.net.Uri
import com.genshin.gm.ui.component.GlassGradient
import com.genshin.gm.ui.screen.*
import com.genshin.gm.ui.theme.GenshinGMTheme
import kotlinx.coroutines.delay
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

    // Splash animation state
    var splashPhase by remember { mutableStateOf(0) }
    // 0 = waiting (2s), 1 = shrinking bar, 2 = shrinking both, 3 = done

    LaunchedEffect(Unit) {
        delay(2000L) // Wait 2 seconds
        splashPhase = 1 // Start shrinking bar
        delay(600L)
        splashPhase = 2 // Bar reached text, shrink both
        delay(500L)
        splashPhase = 3 // Done, hide title bar
    }

    // Animation for bar shrink (phase 1): bar width fraction from 1.0 to ~0.35 (text width ratio)
    val barFraction by animateFloatAsState(
        targetValue = when (splashPhase) {
            0 -> 1f
            1 -> 0.38f  // Shrink to roughly text width
            else -> 0f
        },
        animationSpec = tween(
            durationMillis = if (splashPhase == 1) 600 else 400,
            easing = FastOutSlowInEasing
        ),
        label = "barFraction"
    )

    // Animation for overall scale + alpha (phase 2): both shrink to nothing
    val overallScale by animateFloatAsState(
        targetValue = if (splashPhase >= 2) 0f else 1f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "overallScale"
    )
    val overallAlpha by animateFloatAsState(
        targetValue = if (splashPhase >= 2) 0f else 1f,
        animationSpec = tween(durationMillis = 400),
        label = "overallAlpha"
    )

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

        // Update dialog
        if (state.showUpdateDialog) {
            val context = LocalContext.current
            AlertDialog(
                onDismissRequest = { vm.dismissUpdateDialog() },
                title = { Text("发现新版本") },
                text = { Text("服务端版本: ${state.updateVersion}\n当前版本与服务端不一致，是否下载最新版本？") },
                confirmButton = {
                    TextButton(onClick = {
                        vm.dismissUpdateDialog()
                        val downloadUrl = if (state.updateDownloadUrl.startsWith("http")) {
                            state.updateDownloadUrl
                        } else {
                            "${state.serverUrl}${state.updateDownloadUrl}"
                        }
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        context.startActivity(intent)
                    }) {
                        Text("下载更新")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { vm.dismissUpdateDialog() }) {
                        Text("稍后再说")
                    }
                }
            )
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            topBar = {
                // Splash title bar - animates away
                if (splashPhase < 3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .graphicsLayer(
                                scaleX = overallScale,
                                scaleY = overallScale,
                                alpha = overallAlpha
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        // The gray bar that shrinks
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1A1B2E).copy(alpha = 0.6f))
                        )
                        // Title text
                        Text(
                            "原神GM工具",
                            modifier = Modifier.padding(start = 12.dp),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // After animation: no top bar, more content space
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
