package com.autotalk.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autotalk.app.data.db.ChatSessionEntity
import com.autotalk.app.ui.viewmodels.SessionListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    vm: SessionListViewModel,
    onSessionClick: (String) -> Unit
) {
    val sessions by vm.sessions.collectAsState()
    var showRenameDialog by remember { mutableStateOf<ChatSessionEntity?>(null) }
    var renameText by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("助手") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.createSession() }) {
                Icon(Icons.Filled.Add, contentDescription = "新建对话")
            }
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Message, contentDescription = null,
                     modifier = Modifier.height(48.dp),
                     tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("暂无对话", style = MaterialTheme.typography.titleMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("点击右下角 + 开始新对话",
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(
                        session = session,
                        onClick = { onSessionClick(session.id) },
                        onRename = {
                            showRenameDialog = session
                            renameText = session.title
                        },
                        onDelete = { vm.deleteSession(session.id) }
                    )
                }
            }
        }
    }

    showRenameDialog?.let { s ->
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("重命名对话") },
            text = {
                TextField(value = renameText, onValueChange = { renameText = it })
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.renameSession(s.id, renameText)
                    showRenameDialog = null
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SessionCard(
    session: ChatSessionEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(session.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            if (session.lastMessagePreview.isNotEmpty()) {
                Text(session.lastMessagePreview,
                     style = MaterialTheme.typography.bodySmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant,
                     maxLines = 1)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(session.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    TextButton(onClick = onRename) { Text("重命名", style = MaterialTheme.typography.labelSmall) }
                    TextButton(onClick = onDelete) { Text("删除", style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}