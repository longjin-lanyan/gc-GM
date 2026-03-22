package com.genshin.gm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genshin.gm.ui.MainViewModel
import com.genshin.gm.ui.UiState
import com.genshin.gm.ui.component.*

@Composable
fun ExecuteScreen(vm: MainViewModel, state: UiState) {
    var command by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Status card
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            if (!state.isLoggedIn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = GlassError)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("请先登录并绑定UID", color = GlassError)
                }
            } else if (state.activeUid.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = GlassWarning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("请在账户页选择活动UID", color = GlassWarning)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = GlassSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "用户: ${state.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GlassTextColor,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "活动UID: ${state.activeUid}",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassSecondaryText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick commands + custom command form
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
            Text("快捷指令", style = MaterialTheme.typography.titleSmall, color = GlassPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            val quickCommands = listOf(
                "/give 201 x10000" to "摩拉 x10000",
                "/give 202 x1000" to "原石 x1000",
                "/give 203 x100" to "纠缠之缘 x100",
                "/heal" to "治疗全队",
                "/killall" to "清除周围怪物"
            )

            quickCommands.forEach { (cmd, label) ->
                Surface(
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    onClick = {
                        command = cmd
                        if (state.isLoggedIn && state.activeUid.isNotEmpty()) {
                            vm.executeCustomCommand(cmd)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, modifier = Modifier.weight(1f), color = GlassTextColor, style = MaterialTheme.typography.bodySmall)
                        Surface(
                            color = Color.White,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                cmd,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                color = GlassPrimary,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.PlayArrow, null, tint = GlassPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom command input
            GlassFormLabel("自定义指令：")
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                placeholder = { Text("/give 223 x99") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Terminal, null) },
                colors = glassTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassGradientButton(
                onClick = {
                    if (command.isNotEmpty()) vm.executeCustomCommand(command)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.isLoggedIn && state.activeUid.isNotEmpty() && command.isNotEmpty() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Send, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("向服务器执行指令")
            }
        }

        // Result display
        if (state.executeResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "执行结果",
                    style = MaterialTheme.typography.titleSmall,
                    color = GlassTextColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = if (state.executeResult.startsWith("成功")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        state.executeResult,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.executeResult.startsWith("成功")) GlassSuccess else GlassError
                    )
                }
            }
        }
    }
}
