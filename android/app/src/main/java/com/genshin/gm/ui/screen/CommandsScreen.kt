package com.genshin.gm.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genshin.gm.proto.PlayerCommandProto
import com.genshin.gm.ui.MainViewModel
import com.genshin.gm.ui.UiState
import com.genshin.gm.ui.component.*

/**
 * 玩家指令广场 - matches web index.html community-section layout exactly
 *
 * Web structure:
 *   .status-indicator (top-right: 服务器/在线/UID)
 *   .sub-menu (指令广场 | 场景 | 角色 | 上传指令)
 *   .community-list (command card grid)
 *   .upload-form (upload form with instructions)
 */
@Composable
fun CommandsScreen(vm: MainViewModel, state: UiState) {
    // Sub-tab: 0=指令广场, 1=场景, 2=角色, 3=上传指令
    var selectedSubTab by remember { mutableStateOf(0) }
    var selectedCategory by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.loadApprovedCommands()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {

        // ===== Status Indicator (matches web .status-indicator) =====
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            alpha = 0.78f,
            elevation = 3.dp,
            contentPadding = 10.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 服务器状态
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("服务器: ", style = MaterialTheme.typography.labelSmall, color = GlassSecondaryText)
                    Text(
                        if (state.isInitialized) "已连接" else "连接中...",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.isInitialized) GlassSuccess else GlassWarning,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // 在线人数
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("在线: ", style = MaterialTheme.typography.labelSmall, color = GlassSecondaryText)
                    Text(
                        "${state.onlinePlayerCount}人",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                // UID
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("UID: ", style = MaterialTheme.typography.labelSmall, color = GlassSecondaryText)
                    Text(
                        state.activeUid.ifEmpty { "未选择" },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (state.activeUid.isNotEmpty()) GlassPrimary else GlassSecondaryText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // ===== Sub-Menu (matches web .sub-menu: 指令广场 | 场景 | 角色 | 上传指令) =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val subTabs = listOf("指令广场" to "", "场景" to "scene", "角色" to "avatar", "上传指令" to "upload")
            subTabs.forEachIndexed { index, (label, _) ->
                GlassChip(
                    label = label,
                    selected = selectedSubTab == index,
                    onClick = {
                        selectedSubTab = index
                        if (index < 3) {
                            selectedCategory = subTabs[index].second
                            vm.loadApprovedCommands(category = selectedCategory)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ===== Content based on sub-tab =====
        if (selectedSubTab == 3) {
            // Upload form (matches web #upload-form .upload-form)
            UploadForm(
                onSubmit = { title, desc, cmd, cat ->
                    vm.submitCommand(title, desc, cmd, cat, state.username)
                }
            )
        } else {
            // Command card list (matches web .community-list)
            if (state.approvedCommands.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(alpha = 0.75f) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, null, tint = GlassSecondaryText, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("暂无指令", color = GlassTextColor, fontWeight = FontWeight.Medium)
                            Text("点击「上传指令」分享你的指令", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.approvedCommands, key = { "${it.id}_${it.title}" }) { cmd ->
                        CommandCard(
                            cmd = cmd,
                            onLike = { vm.likeCommand(cmd.id) },
                            onExecute = { vm.executePresetCommand(cmd.id) },
                            canExecute = state.isLoggedIn && state.activeUid.isNotEmpty()
                        )
                    }
                }
            }

            // Execute result
            if (state.executeResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = if (state.executeResult.startsWith("成功")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        state.executeResult,
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.executeResult.startsWith("成功")) GlassSuccess else GlassError
                    )
                }
            }
        }
    }
}

/**
 * Command Card - matches web .command-card exactly:
 *   .command-card-header (title + category badge)
 *   .command-card-description
 *   .command-card-content (monospace, scrollable)
 *   .command-card-footer (info + actions with border-top)
 */
@Composable
private fun CommandCard(
    cmd: PlayerCommandProto,
    onLike: () -> Unit,
    onExecute: () -> Unit,
    canExecute: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        alpha = 0.78f,
        elevation = 3.dp
    ) {
        // Header: title + category badge (matches .command-card-header)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                cmd.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = GlassTextColor,
                fontWeight = FontWeight.Bold
            )
            if (cmd.category.isNotEmpty()) {
                // Category badge (matches .command-card-category: gradient bg, white text, rounded)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(GlassPrimary, GlassPrimaryDark),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 100f)
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        cmd.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Description (matches .command-card-description)
        if (cmd.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                cmd.description,
                style = MaterialTheme.typography.bodySmall,
                color = GlassSecondaryText
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Command content (matches .command-card-content: monospace, white bg, rounded)
        Surface(
            color = Color.White.copy(alpha = 0.45f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                cmd.command,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                color = GlassTextColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Footer (matches .command-card-footer: border-top, info + actions)
        HorizontalDivider(color = GlassPrimary.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info section (matches .command-card-info)
            Text("👤 ", fontSize = 12.sp)
            Text(
                cmd.uploaderName.ifEmpty { "匿名" },
                style = MaterialTheme.typography.labelSmall,
                color = GlassSecondaryText
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("👁 ", fontSize = 12.sp)
            Text("${cmd.views}", style = MaterialTheme.typography.labelSmall, color = GlassSecondaryText)
            Spacer(modifier = Modifier.width(12.dp))
            Text("❤️ ", fontSize = 12.sp)
            Text("${cmd.likes}", style = MaterialTheme.typography.labelSmall, color = GlassSecondaryText)

            Spacer(modifier = Modifier.weight(1f))

            // Actions (matches .command-card-actions: like-btn + use-btn)
            TextButton(
                onClick = onLike,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("点赞", style = MaterialTheme.typography.labelSmall, color = GlassPrimary)
            }
            if (canExecute) {
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(
                    onClick = onExecute,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("使用", style = MaterialTheme.typography.labelSmall, color = GlassPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Upload form - matches web #upload-form .upload-form exactly
 * Web: single .upload-form card with instructions + all form fields inside
 */
@Composable
private fun UploadForm(
    onSubmit: (String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("item") }
    var uploaderName by remember { mutableStateOf("") }

    // Single glass card wrapping everything (matches web .upload-form)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                alpha = 0.78f,
                contentPadding = 20.dp
            ) {
                // ===== Instructions section (matches web: bg #e3f2fd, border-left 4px #667eea) =====
                GlassInfoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "📝 指令上传说明",
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp),
                        color = GlassPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // UID占位符
                    Text("UID占位符：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTextColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Bullet: 可以使用 @ 或 @UID (with inline code styling)
                    Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                        Text("• 可以使用 ", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                        InlineCode("@")
                        Text(" 或 ", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                        InlineCode("@UID")
                        Text(" 作为UID占位符", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                    }
                    Row(modifier = Modifier.padding(start = 16.dp, top = 2.dp)) {
                        Text("• 或者直接不写UID，系统会自动添加（推荐）", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 示例
                    Text("示例：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTextColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    // Each example: ✅ + code block + description
                    ExampleRow("give 201 99", "（系统会自动添加UID）")
                    ExampleRow("give @ 201 99", null)
                    ExampleRow("give @UID 201 99", null)
                    ExampleRow("tp 2000 300 -1000", null)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Warning
                    Text(
                        "⚠️ 注意：不要包含分号(;)、双与号(&&)等特殊字符",
                        style = MaterialTheme.typography.bodySmall,
                        color = GlassError,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 分类选择 - 放在说明卡片内
                    Text("选择分类：", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = GlassTextColor)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "物品" to "item", "武器" to "weapon", "角色" to "avatar",
                            "任务" to "quest", "场景" to "scene", "其他" to "other"
                        ).forEach { (label, value) ->
                            GlassChip(
                                label = label,
                                selected = category == value,
                                onClick = { category = value }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ===== Form fields =====

                // 指令标题
                FormLabel("指令标题：")
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    placeholder = { Text("请输入指令标题（必填）") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = glassTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 指令描述
                FormLabel("指令描述：")
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    placeholder = { Text("请描述这个指令的作用（必填）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = glassTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 指令内容
                FormLabel("指令内容：")
                OutlinedTextField(
                    value = command, onValueChange = { command = it },
                    placeholder = { Text("例如: give 201 99 或 tp 2000 300 -1000（UID会自动添加）") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    colors = glassTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 上传者名称
                FormLabel("上传者名称（可选）：")
                OutlinedTextField(
                    value = uploaderName, onValueChange = { uploaderName = it },
                    placeholder = { Text("请输入您的昵称") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    colors = glassTextFieldColors()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Submit button (matches web .generate-btn)
                GlassGradientButton(
                    onClick = { onSubmit(title, description, command, category) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = title.isNotEmpty() && command.isNotEmpty()
                ) {
                    Text("提交指令")
                }
            }
        }
    }
}

// Inline code element (matches web <code> styling: white bg, padding, rounded)
@Composable
private fun InlineCode(text: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(3.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = GlassTextColor
        )
    }
}

// Example row: • ✅ code （description）
@Composable
private fun ExampleRow(code: String, desc: String?) {
    Row(
        modifier = Modifier.padding(start = 16.dp, top = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("• ✅ ", style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
        InlineCode(code)
        if (desc != null) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = GlassSecondaryText)
        }
    }
}

// Form label (matches web .form-group label)
@Composable
private fun FormLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = GlassTextColor,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
