package com.genshin.gm.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genshin.gm.proto.GameDataItem
import com.genshin.gm.ui.MainViewModel
import com.genshin.gm.ui.UiState
import com.genshin.gm.ui.component.*

@Composable
fun HomeScreen(vm: MainViewModel, state: UiState) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("物品", "武器", "角色", "任务")

    LaunchedEffect(state.isInitialized) {
        if (state.items.isEmpty() && state.isInitialized && !state.isLoading) {
            vm.loadGameData()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        // Resource sync status + online count (always visible)
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    state.resourceSyncStatus.ifEmpty { "就绪" },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassSecondaryText
                )
                Text(
                    "在线: ${state.onlinePlayerCount}人",
                    style = MaterialTheme.typography.bodySmall,
                    color = GlassPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Glass Tab Row
        GlassTabRow(
            tabs = tabs,
            selectedIndex = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading && state.items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = GlassPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("加载数据中...", color = GlassTextColor)
                    }
                }
            }
        } else if (state.items.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, null, tint = GlassSecondaryText, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("暂无数据", color = GlassTextColor, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("请检查服务器连接后重试", color = GlassSecondaryText, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(12.dp))
                        GlassGradientButton(onClick = { vm.loadGameData() }) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重新加载")
                        }
                    }
                }
            }
        } else {
            val dataList = when (selectedTab) {
                0 -> state.items
                1 -> state.weapons
                2 -> state.avatars
                3 -> state.quests
                else -> emptyList()
            }

            GameDataList(
                items = dataList,
                isQuest = selectedTab == 3,
                tabName = tabs[selectedTab],
                onGenerateGive = { id, qty -> vm.generateGiveCommand(id, qty) },
                onGenerateQuest = { id, finish -> vm.generateQuestCommand(id, finish) },
                generatedCommand = state.generatedCommand,
                onExecute = { cmd -> vm.executeCustomCommand(cmd) },
                canExecute = state.isLoggedIn && state.activeUid.isNotEmpty(),
                executeResult = state.executeResult,
                isLoading = state.isLoading
            )
        }
    }
}

@Composable
private fun GameDataList(
    items: List<GameDataItem>,
    isQuest: Boolean,
    tabName: String,
    onGenerateGive: (Int, Int) -> Unit,
    onGenerateQuest: (Int, Boolean) -> Unit,
    generatedCommand: String,
    onExecute: (String) -> Unit,
    canExecute: Boolean,
    executeResult: String,
    isLoading: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<GameDataItem?>(null) }
    var quantity by remember { mutableStateOf("1") }

    // Everything wrapped in one outer GlassCard (like web .upload-form)
    GlassCard(
        modifier = Modifier.fillMaxSize(),
        contentPadding = 16.dp
    ) {
        // Search field
        GlassFormLabel("搜索${tabName}：")
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("输入名称或ID搜索") },
            leadingIcon = { Icon(Icons.Default.Search, "搜索", modifier = Modifier.size(18.dp)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = glassTextFieldColors(),
            shape = MaterialTheme.shapes.medium
        )

        // Generated command display
        if (generatedCommand.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("指令:", style = MaterialTheme.typography.labelSmall, color = GlassSecondaryText)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            generatedCommand,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = GlassPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (canExecute) {
                        Spacer(modifier = Modifier.height(6.dp))
                        GlassGradientButton(
                            onClick = { onExecute(generatedCommand) },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("执行", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        Text("请先登录并选择活动UID后执行", style = MaterialTheme.typography.labelSmall, color = GlassWarning)
                    }
                    if (executeResult.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            executeResult,
                            color = if (executeResult.startsWith("成功")) GlassSuccess else GlassError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Item list - inner items use lightweight Surface (not standalone GlassCard)
        val filtered = items.filter {
            searchQuery.isEmpty()
                    || it.name.contains(searchQuery, ignoreCase = true)
                    || it.id.toString().contains(searchQuery)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered.size) { index ->
                val item = filtered[index]
                Surface(
                    color = Color.White.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = GlassTextColor,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "ID: ${item.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassSecondaryText
                            )
                        }

                        if (!isQuest) {
                            if (selectedItem == item) {
                                OutlinedTextField(
                                    value = quantity,
                                    onValueChange = { quantity = it },
                                    label = { Text("数量") },
                                    modifier = Modifier.width(80.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = glassTextFieldColors()
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            IconButton(onClick = {
                                selectedItem = item
                                onGenerateGive(item.id, quantity.toIntOrNull() ?: 1)
                            }) {
                                Icon(Icons.Default.Add, "给予", tint = GlassPrimary)
                            }
                        } else {
                            IconButton(onClick = { onGenerateQuest(item.id, false) }) {
                                Icon(Icons.Default.PlaylistAdd, "添加任务", tint = GlassPrimary)
                            }
                            IconButton(onClick = { onGenerateQuest(item.id, true) }) {
                                Icon(Icons.Default.CheckCircle, "完成任务", tint = GlassSuccess)
                            }
                        }
                    }
                }
            }
        }
    }
}
