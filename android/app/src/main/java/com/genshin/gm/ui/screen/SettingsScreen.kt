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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genshin.gm.ui.MainViewModel
import com.genshin.gm.ui.UiState
import com.genshin.gm.ui.component.*

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
        // Server settings card
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
            GlassInfoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "⚙️ 服务器设置",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                    color = GlassPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("连接说明：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTextColor)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• 输入GM服务器的地址和端口", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• 格式: http://IP地址:端口号", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• 修改后需要点击保存才会生效", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    "⚠️ 确保服务器地址正确，否则无法连接",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassError,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            GlassFormLabel("服务器地址：")
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                placeholder = { Text("http://110.42.109.118:8088") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Dns, null) },
                colors = glassTextFieldColors()
            )
            Spacer(modifier = Modifier.height(16.dp))
            GlassGradientButton(
                onClick = { vm.updateServerUrl(serverUrl) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Resource sync card
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
            GlassInfoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "📦 资源管理",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                    color = GlassPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("同步说明：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTextColor)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• 检查并同步服务端最新的游戏数据文件", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• 包括物品、角色、武器等数据和背景图片", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• 首次使用或数据异常时建议同步一次", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.resourceSyncStatus.isNotEmpty()) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        state.resourceSyncStatus,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            GlassGradientButton(
                onClick = { vm.syncResources() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Sync, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("同步资源")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Admin panel card
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
            GlassInfoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "🔐 管理员",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                    color = GlassPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("管理功能：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTextColor)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• 审核玩家提交的指令", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• 需要管理员Token验证身份", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GlassGradientButton(
                onClick = { showAdminDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AdminPanelSettings, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("进入管理面板")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About card
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 20.dp) {
            GlassInfoCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "ℹ️ 关于",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                    color = GlassPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• 原神GM工具 v1.0.0", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }
                Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                    Text("• Android 8.0+ | Protobuf 通信", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                }
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
        containerColor = Color.White.copy(alpha = 0.85f),
        titleContentColor = GlassTextColor,
        textContentColor = GlassTextColor,
        title = { Text("管理面板", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!authenticated) {
                    GlassFormLabel("管理员Token：")
                    OutlinedTextField(
                        value = adminToken, onValueChange = { adminToken = it },
                        placeholder = { Text("请输入管理员Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = glassTextFieldColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassGradientButton(
                        onClick = {
                            vm.loadPendingCommands(adminToken)
                            authenticated = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = adminToken.isNotEmpty()
                    ) {
                        Text("验证")
                    }
                } else {
                    Text(
                        "待审核指令 (${state.pendingCommands.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = GlassTextColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.pendingCommands.isEmpty()) {
                        Text("暂无待审核指令", color = GlassSecondaryText)
                    } else {
                        state.pendingCommands.forEach { cmd ->
                            Surface(
                                color = Color.White.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        cmd.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GlassTextColor,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        cmd.command,
                                        color = GlassPrimary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row {
                                        GlassGradientButton(onClick = {
                                            vm.approveCommand(cmd.id, adminToken)
                                        }) { Text("通过") }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                vm.rejectCommand(cmd.id, adminToken)
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GlassError)
                                        ) { Text("拒绝") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭", color = GlassPrimary) }
        }
    )
}
