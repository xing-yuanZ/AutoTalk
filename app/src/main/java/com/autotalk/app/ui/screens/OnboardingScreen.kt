package com.autotalk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.autotalk.app.domain.AIBackend
import com.autotalk.app.ui.viewmodels.OnboardingViewModel

/** 引导页：介绍功能 + 选择 AI 后端 +（可选）配置云端 API。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val supportsOnDevice = settings.supportsOnDevice

    Scaffold(topBar = { TopAppBar(title = { Text("欢迎使用 AutoTalk") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // 介绍
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "AutoTalk · 实时对话助手",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "提前告诉它你即将进行的对话（人物、内容、目的），对话开始后它会监听对方说话，并实时生成回复建议；还能学习你的说话风格，让建议更像你。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            // 选择 AI 后端
            Text("选择 AI 后端", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = settings.backend == AIBackend.ON_DEVICE,
                    onClick = { vm.setBackend(AIBackend.ON_DEVICE) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    enabled = supportsOnDevice
                ) { Text("端侧模型") }
                SegmentedButton(
                    selected = settings.backend == AIBackend.CLOUD,
                    onClick = { vm.setBackend(AIBackend.CLOUD) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("云端 API") }
            }
            if (!supportsOnDevice) {
                Text(
                    text = "当前设备不支持端侧模型（需 Gemini Nano），已自动使用云端。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // 云端配置
            if (settings.backend == AIBackend.CLOUD) {
                Text("云端 API 配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "兼容 OpenAI 接口格式。可先用默认值跳过，稍后在设置中填写。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.complete(); onDone() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("开始使用")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
