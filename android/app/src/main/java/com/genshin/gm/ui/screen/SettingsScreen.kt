package com.genshin.gm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun SettingsScreen(vm: MainViewModel, state: UiState) {
    var serverUrl by remember { mutableStateOf(state.serverUrl) }
    var showAdminDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Server URL
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("服务器设置", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("服务器地址") },
                    placeholder = { Text("http://127.0.0.1:8080") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Dns, null) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { vm.updateServerUrl(serverUrl) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("保存")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resource sync
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("资源管理", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "检查并同步服务端最新的游戏数据文件（物品、角色、武器等）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.resourceSyncStatus.isNotEmpty()) {
                    Text(
                        state.resourceSyncStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Button(onClick = { vm.syncResources() }) {
                    Icon(Icons.Default.Sync, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("同步资源")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin panel
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("管理员", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showAdminDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("进入管理面板")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // App info
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("关于", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("原神GM工具 v1.0.0", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Android 8.0+ | Protobuf 通信",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showAdminDialog) {
        AdminPanel(
            vm = vm,
            state = state,
            onDismiss = { showAdminDialog = false }
        )
    }
}

@Composable
private fun AdminPanel(vm: MainViewModel, state: UiState, onDismiss: () -> Unit) {
    var adminToken by remember { mutableStateOf("") }
    var authenticated by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理面板") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!authenticated) {
                    OutlinedTextField(
                        value = adminToken, onValueChange = { adminToken = it },
                        label = { Text("管理员Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            vm.loadPendingCommands(adminToken)
                            authenticated = true
                        },
                        enabled = adminToken.isNotEmpty()
                    ) {
                        Text("验证")
                    }
                } else {
                    Text("待审核指令 (${state.pendingCommands.size})",
                        style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.pendingCommands.isEmpty()) {
                        Text("暂无待审核指令", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        state.pendingCommands.forEach { cmd ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(cmd.title, style = MaterialTheme.typography.bodyMedium)
                                    Text(cmd.command, color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodySmall)
                                    Row {
                                        Button(onClick = {
                                            vm.approveCommand(cmd.id, adminToken)
                                        }) { Text("通过") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(onClick = {
                                            vm.rejectCommand(cmd.id, adminToken)
                                        }) { Text("拒绝") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}
