package com.genshin.gm.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.genshin.gm.ui.MainViewModel
import com.genshin.gm.ui.UiState
import com.genshin.gm.ui.component.*

@Composable
fun AccountScreen(vm: MainViewModel, state: UiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        if (!state.isLoggedIn) {
            LoginSection(vm, state)
        } else {
            UserInfoSection(vm, state)
            Spacer(modifier = Modifier.height(16.dp))
            UidManagementSection(vm, state)
            Spacer(modifier = Modifier.height(16.dp))
            VerificationSection(vm, state)
        }
    }
}

@Composable
private fun LoginSection(vm: MainViewModel, state: UiState) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegister by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth(), alpha = 0.82f) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Person,
                null,
                modifier = Modifier.size(64.dp),
                tint = GlassPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                if (isRegister) "注册账户" else "登录",
                style = MaterialTheme.typography.headlineSmall,
                color = GlassTextColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Person, null) },
                colors = glassTextFieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                colors = glassTextFieldColors()
            )

            Spacer(modifier = Modifier.height(16.dp))

            GlassGradientButton(
                onClick = {
                    if (isRegister) vm.register(username, password)
                    else vm.login(username, password)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotEmpty() && password.isNotEmpty() && !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(if (isRegister) "注册" else "登录")
                }
            }

            TextButton(onClick = { isRegister = !isRegister }) {
                Text(
                    if (isRegister) "已有账户? 登录" else "没有账户? 注册",
                    color = GlassPrimary
                )
            }
        }
    }
}

@Composable
private fun UserInfoSection(vm: MainViewModel, state: UiState) {
    GlassCard(modifier = Modifier.fillMaxWidth(), alpha = 0.80f) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, null, tint = GlassPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                state.username,
                style = MaterialTheme.typography.titleMedium,
                color = GlassTextColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = { vm.logout() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GlassError)
            ) {
                Text("登出")
            }
        }
    }
}

@Composable
private fun UidManagementSection(vm: MainViewModel, state: UiState) {
    GlassCard(modifier = Modifier.fillMaxWidth(), alpha = 0.80f) {
        Text(
            "已绑定的UID",
            style = MaterialTheme.typography.titleSmall,
            color = GlassTextColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (state.verifiedUids.isEmpty()) {
            Text(
                "暂无绑定的UID，请先验证并绑定",
                style = MaterialTheme.typography.bodySmall,
                color = GlassSecondaryText
            )
        } else {
            state.verifiedUids.forEach { uid ->
                // Matches web .uid-item styling
                Surface(
                    color = if (state.activeUid == uid) GlassPrimary.copy(alpha = 0.1f)
                    else Color.White.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium,
                    border = if (state.activeUid == uid)
                        BorderStroke(1.dp, GlassPrimary.copy(alpha = 0.3f))
                    else null,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = state.activeUid == uid,
                            onClick = { vm.setActiveUid(uid) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = GlassPrimary,
                                unselectedColor = GlassSecondaryText
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(uid, modifier = Modifier.weight(1f), color = GlassTextColor)
                        if (state.activeUid == uid) {
                            Surface(
                                color = GlassPrimary.copy(alpha = 0.15f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    "活动",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    color = GlassPrimary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { vm.unbindUid(uid) }) {
                            Icon(Icons.Default.Delete, "解绑", tint = GlassError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerificationSection(vm: MainViewModel, state: UiState) {
    var uid by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth(), alpha = 0.80f) {
        Text(
            "验证并绑定新UID",
            style = MaterialTheme.typography.titleSmall,
            color = GlassTextColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = uid, onValueChange = { uid = it },
            label = { Text("UID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = glassTextFieldColors()
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (!codeSent) {
            GlassGradientButton(
                onClick = {
                    vm.sendVerificationCode(uid)
                    codeSent = true
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uid.isNotEmpty() && !state.isLoading
            ) {
                Text("发送验证码到游戏内")
            }
        } else {
            OutlinedTextField(
                value = code, onValueChange = { code = it },
                label = { Text("验证码") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = glassTextFieldColors()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                OutlinedButton(
                    onClick = { vm.sendVerificationCode(uid) },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GlassPrimary)
                ) {
                    Text("重新发送")
                }
                Spacer(modifier = Modifier.width(8.dp))
                GlassGradientButton(
                    onClick = { vm.verifyCode(uid, code) },
                    modifier = Modifier.weight(1f),
                    enabled = code.isNotEmpty() && !state.isLoading
                ) {
                    Text("验证并绑定")
                }
            }
        }
    }
}
