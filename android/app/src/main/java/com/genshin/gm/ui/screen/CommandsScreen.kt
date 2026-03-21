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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.genshin.gm.proto.PlayerCommandProto
import com.genshin.gm.ui.MainViewModel
import com.genshin.gm.ui.UiState
import com.genshin.gm.ui.component.*

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
            GlassChip(
                label = "最新",
                selected = selectedSort == "time",
                onClick = {
                    selectedSort = "time"
                    vm.loadApprovedCommands(sort = "time")
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassChip(
                label = "热门",
                selected = selectedSort == "popular",
                onClick = {
                    selectedSort = "popular"
                    vm.loadApprovedCommands(sort = "popular")
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            GlassGradientButton(onClick = { showSubmitDialog = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("提交指令")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                alpha = 0.85f
            ) {
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
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        alpha = 0.5f
    ) {
        Text(
            cmd.title,
            style = MaterialTheme.typography.titleSmall,
            color = GlassTextColor,
            fontWeight = FontWeight.Bold
        )
        if (cmd.description.isNotEmpty()) {
            Text(
                cmd.description,
                style = MaterialTheme.typography.bodySmall,
                color = GlassSecondaryText
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = Color(0xFFF5F5F5),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                cmd.command,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = GlassPrimary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (cmd.category.isNotEmpty()) {
                GlassChip(label = cmd.category)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                "by ${cmd.uploaderName}",
                style = MaterialTheme.typography.labelSmall,
                color = GlassSecondaryText
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onLike) {
                Icon(Icons.Default.ThumbUp, "点赞", tint = GlassPrimary)
            }
            Text("${cmd.likes}", style = MaterialTheme.typography.labelSmall, color = GlassTextColor)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp), tint = GlassSecondaryText)
            Text(" ${cmd.views}", style = MaterialTheme.typography.labelSmall, color = GlassTextColor)
            if (canExecute) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onExecute) {
                    Icon(Icons.Default.PlayArrow, "执行", tint = GlassPrimary)
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
        containerColor = Color.White.copy(alpha = 0.95f),
        titleContentColor = GlassTextColor,
        textContentColor = GlassTextColor,
        title = { Text("提交指令", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                // Upload instructions card (matching the web UI style)
                Surface(
                    color = Color(0xFFF8F9FA),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "指令上传说明",
                            style = MaterialTheme.typography.titleSmall,
                            color = GlassPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "UID占位符：",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = GlassTextColor
                        )
                        Text(
                            "  - 可以使用 @ 或 @UID 作为UID占位符\n  - 或者直接不写UID，系统会自动添加（推荐）",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassSecondaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "示例: give 201 99",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassSecondaryText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("指令标题") },
                    placeholder = { Text("请输入指令标题（必填）") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = glassTextFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = command, onValueChange = { command = it },
                    label = { Text("指令内容") },
                    placeholder = { Text("例如: give 201 99") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = glassTextFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("指令描述 (可选)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = glassTextFieldColors()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category, onValueChange = { category = it },
                    label = { Text("分类 (可选)") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = glassTextFieldColors()
                )
            }
        },
        confirmButton = {
            GlassGradientButton(
                onClick = { onSubmit(title, description, command, category) },
                enabled = title.isNotEmpty() && command.isNotEmpty()
            ) { Text("提交") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = GlassSecondaryText) }
        }
    )
}
