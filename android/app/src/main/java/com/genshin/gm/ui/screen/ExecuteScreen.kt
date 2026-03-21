package com.genshin.gm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.genshin.gm.ui.MainViewModel
import com.genshin.gm.ui.UiState

@Composable
fun ExecuteScreen(vm: MainViewModel, state: UiState) {
    var command by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!state.isLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("请先登录并绑定UID", color = MaterialTheme.colorScheme.error)
                    }
                } else if (state.activeUid.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("请在账户页选择活动UID", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("用户: ${state.username}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "活动UID: ${state.activeUid}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Command input
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("输入自定义指令") },
            placeholder = { Text("/give 223 x99") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Terminal, null) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
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
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Default.PlayArrow, null)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("执行指令")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result display
        if (state.executeResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.executeResult.startsWith("成功"))
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("执行结果", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(state.executeResult, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick commands
        Text("快捷指令", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        val quickCommands = listOf(
            "/give 201 x10000" to "摩拉 x10000",
            "/give 202 x1000" to "原石 x1000",
            "/give 203 x100" to "纠缠之缘 x100",
            "/heal" to "治疗全队",
            "/killall" to "清除周围怪物"
        )

        quickCommands.forEach { (cmd, label) ->
            OutlinedButton(
                onClick = {
                    command = cmd
                    vm.executeCustomCommand(cmd)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                enabled = state.isLoggedIn && state.activeUid.isNotEmpty()
            ) {
                Text(label, modifier = Modifier.weight(1f))
                Text(cmd, color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
