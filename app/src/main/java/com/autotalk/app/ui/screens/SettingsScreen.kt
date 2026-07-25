package com.autotalk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.autotalk.app.domain.AIBackend
import com.autotalk.app.domain.ASRBackend
import com.autotalk.app.domain.RecognitionMode
import com.autotalk.app.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val settings by vm.settings.collectAsState()
    var showAPIKey by remember { mutableStateOf(false) }
    var showDoubaoToken by remember { mutableStateOf(false) }
    var showClearAlert by remember { mutableStateOf(false) }
    var showReonboardConfirm by remember { mutableStateOf(false) }

    val (backend, available) = vm.backendAvailability()

    androidx.compose.material3.Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // AI 后端
            SectionCard(title = "AI 后端") {
                Text("选择生成建议使用的模型后端", style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = backend == AIBackend.ON_DEVICE,
                        onClick = { vm.setBackend(AIBackend.ON_DEVICE) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        enabled = settings.supportsOnDevice
                    ) { Text("端侧") }
                    SegmentedButton(
                        selected = backend == AIBackend.CLOUD,
                        onClick = { vm.setBackend(AIBackend.CLOUD) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("云端") }
                }
                if (!settings.supportsOnDevice) {
                    Text("当前设备不支持端侧模型", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.error)
                }
                if (!available) {
                    Text(
                        text = if (backend == AIBackend.CLOUD) "云端未配置 API Key" else "端侧模型不可用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 云端配置
            SectionCard(title = "云端 API 配置") {
                OutlinedTextField(
                    value = settings.cloudBaseURL,
                    onValueChange = vm::setCloudBaseURL,
                    label = { Text("Base URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = settings.cloudModel,
                    onValueChange = vm::setCloudModel,
                    label = { Text("模型名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = settings.cloudAPIKey,
                    onValueChange = vm::setCloudAPIKey,
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (showAPIKey) VisualTransformation.None
                                           else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showAPIKey = !showAPIKey }) {
                            Icon(
                                imageVector = if (showAPIKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 语音识别
            val (asrBackend, asrAvailable) = vm.asrAvailability()
            SectionCard(title = "语音识别") {
                Text("识别后端", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ASRBackend.values().forEachIndexed { index, b ->
                        SegmentedButton(
                            selected = settings.asrBackend == b,
                            onClick = { vm.setAsrBackend(b) },
                            shape = SegmentedButtonDefaults.itemShape(index, ASRBackend.values().size)
                        ) { Text(b.displayName) }
                    }
                }
                if (!asrAvailable && asrBackend == ASRBackend.DOUBAO) {
                    Text("豆包后端需填写 AppID 和 AccessToken", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(12.dp))
                Text("识别模式", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RecognitionMode.values().forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.recognitionMode == mode,
                            onClick = { vm.setRecognitionMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, RecognitionMode.values().size),
                            enabled = settings.asrBackend == ASRBackend.SYSTEM
                        ) { Text(mode.displayName) }
                    }
                }
            }

            // 豆包 ASR 配置
            if (settings.asrBackend == ASRBackend.DOUBAO) {
                SectionCard(title = "豆包 ASR 配置") {
                    OutlinedTextField(
                        value = settings.doubaoAppID,
                        onValueChange = vm::setDoubaoAppID,
                        label = { Text("App ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = settings.doubaoCluster,
                        onValueChange = vm::setDoubaoCluster,
                        label = { Text("Cluster") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = settings.doubaoAccessToken,
                        onValueChange = vm::setDoubaoAccessToken,
                        label = { Text("Access Token") },
                        singleLine = true,
                        visualTransformation = if (showDoubaoToken) VisualTransformation.None
                                               else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showDoubaoToken = !showDoubaoToken }) {
                                Icon(
                                    imageVector = if (showDoubaoToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 自动播报
            SectionCard(title = "语音播报") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("自动播报建议", modifier = Modifier.weight(1f))
                    Switch(
                        checked = settings.autoSpeakSuggestions,
                        onCheckedChange = vm::setAutoSpeak
                    )
                }
            }

            // 界面语言
            SectionCard(title = "语言") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf<Pair<String, String?>>("跟随系统" to null, "中文" to "zh-Hans", "English" to "en")
                    options.forEachIndexed { index, (label, value) ->
                        SegmentedButton(
                            selected = settings.appLanguage == value,
                            onClick = { vm.setAppLanguage(value) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size)
                        ) { Text(label) }
                    }
                }
            }

            // 风格画像
            SectionCard(title = "风格画像") {
                val profile = com.autotalk.app.LocalAppContainer.current.styleProfile.collectAsState().value
                Text(
                    text = if (profile.isEmpty) "尚未学习风格画像。"
                           else if (profile.summary.isEmpty())
                               "正式度 ${(profile.formality * 100).toInt()}%"
                           else profile.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { vm.clearStyle() }, modifier = Modifier.fillMaxWidth()) {
                    Text("清除风格画像")
                }
            }

            // 数据
            SectionCard(title = "数据") {
                Button(
                    onClick = { showClearAlert = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("清除所有对话与聊天记录")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showReonboardConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("重新查看引导")
                }
            }

            // 关于
            SectionCard(title = "关于") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("版本", style = MaterialTheme.typography.bodySmall)
                    Text("1.0", style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showClearAlert) {
        AlertDialog(
            onDismissRequest = { showClearAlert = false },
            title = { Text("清除所有数据？") },
            text = { Text("将删除全部对话、转录与教练聊天记录，不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAll()
                    showClearAlert = false
                }) { Text("清除") }
            },
            dismissButton = { TextButton(onClick = { showClearAlert = false }) { Text("取消") } }
        )
    }

    if (showReonboardConfirm) {
        AlertDialog(
            onDismissRequest = { showReonboardConfirm = false },
            title = { Text("重新查看引导？") },
            text = { Text("下次启动应用时将显示引导页。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.setOnboardingDone(false)
                    showReonboardConfirm = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showReonboardConfirm = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
