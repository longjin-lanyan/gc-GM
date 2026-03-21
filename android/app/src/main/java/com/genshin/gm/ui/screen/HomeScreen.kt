package com.genshin.gm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.genshin.gm.proto.GameDataItem
import com.genshin.gm.ui.MainViewModel
import com.genshin.gm.ui.UiState

@Composable
fun HomeScreen(vm: MainViewModel, state: UiState) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("物品", "武器", "角色", "任务")

    LaunchedEffect(Unit) {
        if (state.items.isEmpty()) vm.loadGameData()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Resource sync status
        if (state.resourceSyncStatus.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    state.resourceSyncStatus,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Tab row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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
                onGenerateGive = { id, qty -> vm.generateGiveCommand(id, qty) },
                onGenerateQuest = { id, finish -> vm.generateQuestCommand(id, finish) },
                generatedCommand = state.generatedCommand,
                onExecute = { cmd -> vm.executeCustomCommand(cmd) },
                canExecute = state.isLoggedIn && state.activeUid.isNotEmpty()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameDataList(
    items: List<GameDataItem>,
    isQuest: Boolean,
    onGenerateGive: (Int, Int) -> Unit,
    onGenerateQuest: (Int, Boolean) -> Unit,
    generatedCommand: String,
    onExecute: (String) -> Unit,
    canExecute: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<GameDataItem?>(null) }
    var quantity by remember { mutableStateOf("1") }

    Column {
        // Search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("搜索") },
            leadingIcon = { Icon(Icons.Default.Search, "搜索") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Generated command display
        if (generatedCommand.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("生成的指令:", style = MaterialTheme.typography.labelMedium)
                    Text(
                        generatedCommand,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (canExecute) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onExecute(generatedCommand) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("执行")
                        }
                    }
                }
            }
        }

        val filtered = items.filter {
            searchQuery.isEmpty()
                    || it.name.contains(searchQuery, ignoreCase = true)
                    || it.id.toString().contains(searchQuery)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    onClick = { selectedItem = item }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "ID: ${item.id}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            IconButton(onClick = {
                                onGenerateGive(item.id, quantity.toIntOrNull() ?: 1)
                            }) {
                                Icon(Icons.Default.Add, "给予")
                            }
                        } else {
                            IconButton(onClick = { onGenerateQuest(item.id, false) }) {
                                Icon(Icons.Default.AddTask, "添加任务")
                            }
                            IconButton(onClick = { onGenerateQuest(item.id, true) }) {
                                Icon(Icons.Default.CheckCircle, "完成任务")
                            }
                        }
                    }
                }
            }
        }
    }
}
