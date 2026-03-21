package com.genshin.gm.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.genshin.gm.proto.PlayerCommandProto
import com.genshin.gm.ui.MainViewModel
import com.genshin.gm.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandsScreen(vm: MainViewModel, state: UiState) {
    var showSubmitDialog by remember { mutableStateOf(false) }
    var selectedSort by remember { mutableStateOf("time") }

    LaunchedEffect(Unit) {
        vm.loadApprovedCommands()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Sort & Submit row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedSort == "time",
                onClick = {
                    selectedSort = "time"
                    vm.loadApprovedCommands(sort = "time")
                },
                label = { Text("最新") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = selectedSort == "popular",
                onClick = {
                    selectedSort = "popular"
                    vm.loadApprovedCommands(sort = "popular")
                },
                label = { Text("热门") }
            )
            Spacer(modifier = Modifier.weight(1f))
            FilledTonalButton(onClick = { showSubmitDialog = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("提交指令")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Commands list
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.approvedCommands, key = { it.id }) { cmd ->
                CommandCard(
                    cmd = cmd,
                    onLike = { vm.likeCommand(cmd.id) },
                    onExecute = { vm.executePresetCommand(cmd.id) },
                    canExecute = state.isLoggedIn && state.activeUid.isNotEmpty()
                )
            }
        }

        // Execute result
        if (state.executeResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Text(
                    state.executeResult,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showSubmitDialog) {
        SubmitCommandDialog(
            onDismiss = { showSubmitDialog = false },
            onSubmit = { title, desc, cmd, cat ->
                vm.submitCommand(title, desc, cmd, cat, state.username)
                showSubmitDialog = false
            }
        )
    }
}

@Composable
private fun CommandCard(
    cmd: PlayerCommandProto,
    onLike: () -> Unit,
    onExecute: () -> Unit,
    canExecute: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(cmd.title, style = MaterialTheme.typography.titleSmall)
            if (cmd.description.isNotEmpty()) {
                Text(
                    cmd.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    cmd.command,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (cmd.category.isNotEmpty()) {
                    AssistChip(
                        onClick = {},
                        label = { Text(cmd.category, style = MaterialTheme.typography.labelSmall) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    "by ${cmd.uploaderName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onLike) {
                    Icon(Icons.Default.ThumbUp, "点赞")
                }
                Text("${cmd.likes}", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                Text(" ${cmd.views}", style = MaterialTheme.typography.labelSmall)
                if (canExecute) {
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(onClick = onExecute) {
                        Icon(Icons.Default.PlayArrow, "执行")
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmitCommandDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("提交指令") },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("标题") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = command, onValueChange = { command = it },
                    label = { Text("指令内容") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("描述 (可选)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category, onValueChange = { category = it },
                    label = { Text("分类 (可选)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(title, description, command, category) },
                enabled = title.isNotEmpty() && command.isNotEmpty()
            ) { Text("提交") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
