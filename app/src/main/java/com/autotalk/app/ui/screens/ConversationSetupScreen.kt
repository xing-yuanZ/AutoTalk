package com.autotalk.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autotalk.app.domain.ConversationLanguage
import com.autotalk.app.domain.Participant
import com.autotalk.app.ui.viewmodels.ConversationSetupViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationSetupScreen(
    vm: ConversationSetupViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var language by remember { mutableStateOf(ConversationLanguage.ZH) }
    val participants = remember { mutableStateListOf<Participant>() }

    // 新增参与者的临时输入
    var newName by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("") }

    fun addParticipant() {
        if (newName.isBlank()) return
        participants.add(Participant(id = UUID.randomUUID().toString(), name = newName.trim(), role = newRole.trim()))
        newName = ""; newRole = ""
    }

    fun save() {
        if (title.isBlank() || goal.isBlank()) return
        val parts = if (participants.isEmpty()) listOf(Participant(name = "对方", role = "未知")) else participants.toList()
        vm.save(title.trim(), content.trim(), goal.trim(), language, parts, onSaved)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建对话") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("对话标题") },
                placeholder = { Text("如：和客户谈合同") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("对话内容") },
                placeholder = { Text("这次对话大概会聊什么？背景信息…") },
                minLines = 2,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("对话目的") },
                placeholder = { Text("如：促成签约 / 化解误会") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("对话语言", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ConversationLanguage.values().forEachIndexed { index, lang ->
                    SegmentedButton(
                        selected = language == lang,
                        onClick = { language = lang },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ConversationLanguage.values().size)
                    ) { Text(lang.displayName) }
                }
            }

            // 参与者
            Text("参与者", style = MaterialTheme.typography.titleSmall)
            participants.forEachIndexed { index, p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${p.name} · ${p.role.ifBlank { "未指定" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { participants.removeAt(index) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "移除")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("姓名") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = newRole,
                    onValueChange = { newRole = it },
                    label = { Text("角色") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedButton(onClick = { addParticipant() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.padding(end = 4.dp))
                Text("添加参与者")
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { save() },
                enabled = title.isNotBlank() && goal.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("创建并进入会话")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
