package com.autotalk.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.autotalk.app.domain.Speaker
import com.autotalk.app.domain.Suggestion
import com.autotalk.app.service.ConversationEngine
import com.autotalk.app.ui.components.SuggestionCard
import com.autotalk.app.ui.components.TranscriptBubble
import com.autotalk.app.ui.viewmodels.LiveSessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveSessionScreen(vm: LiveSessionViewModel, onBack: () -> Unit) {
    val engine by vm.engine.collectAsState()
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasMicPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val conversation = engine?.conversation

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conversation?.title ?: "会话") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        val eng = engine
        if (eng == null || conversation == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val partial by eng.partialTranscript.collectAsState()
        val finals by eng.finalTranscripts.collectAsState()
        val suggestions by eng.suggestions.collectAsState()
        val isThinking by eng.isThinking.collectAsState()
        val isListening by eng.isListening.collectAsState()
        val errorMsg by eng.error.collectAsState()

        var manualInput by remember { mutableStateOf("") }
        val transcriptListState = rememberLazyListState()
        val suggestionListState = rememberLazyListState()

        // 新转录到来时自动滚到底部。
        LaunchedEffect(finals.size, partial) {
            if (finals.isNotEmpty()) transcriptListState.animateScrollToItem(finals.size - 1)
        }
        LaunchedEffect(suggestions.size) {
            if (suggestions.isNotEmpty()) suggestionListState.animateScrollToItem(0)
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 语境条
            ContextBar(
                goal = conversation.goal,
                participants = conversation.participants,
                isListening = isListening,
                isThinking = isThinking
            )

            // 转录区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 4.dp)
            ) {
                if (finals.isEmpty() && partial.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "点击下方「开始监听」，对方说话后将在此显示并生成建议。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = transcriptListState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(finals) { text ->
                            TranscriptBubble(speaker = Speaker.OTHER, text = text)
                        }
                        if (partial.isNotEmpty()) {
                            item {
                                TranscriptBubble(speaker = Speaker.OTHER, text = partial, isPartial = true)
                            }
                        }
                    }
                }
            }

            // 建议区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 4.dp)
            ) {
                if (isThinking && suggestions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                            Spacer(Modifier.padding(end = 8.dp))
                            Text("正在生成建议…")
                        }
                    }
                } else if (suggestions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "建议会在这里出现",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        state = suggestionListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                    ) {
                        items(suggestions, key = { it.id }) { s ->
                            SuggestionCard(
                                suggestion = s,
                                onSpeak = { vm.speak(s) },
                                onAdopt = { vm.adopt(s) },
                                onRemove = { vm.remove(s) }
                            )
                        }
                    }
                }
            }

            errorMsg?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }

            // 底部控制
            ControlBar(
                isListening = isListening,
                canStart = hasMicPermission,
                onToggleListen = {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else if (isListening) vm.pause() else vm.start()
                },
                manualInput = manualInput,
                onManualChange = { manualInput = it },
                onManualSubmit = {
                    if (manualInput.isNotBlank()) {
                        vm.handleManualInput(manualInput.trim())
                        manualInput = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun ContextBar(
    goal: String,
    participants: List<com.autotalk.app.domain.Participant>,
    isListening: Boolean,
    isThinking: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "目的：$goal",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                when {
                    isListening -> Text("● 监听中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    isThinking -> Text("✦ 思考中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (participants.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = participants.joinToString("、") { "${it.name}·${it.role}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ControlBar(
    isListening: Boolean,
    canStart: Boolean,
    onToggleListen: () -> Unit,
    manualInput: String,
    onManualChange: (String) -> Unit,
    onManualSubmit: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Button(
            onClick = onToggleListen,
            enabled = canStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.Pause else Icons.Filled.Mic,
                contentDescription = null
            )
            Spacer(Modifier.padding(end = 6.dp))
            Text(if (isListening) "暂停监听" else "开始监听")
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = manualInput,
                onValueChange = onManualChange,
                placeholder = { Text("或手动输入对方说的话") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onManualSubmit, enabled = manualInput.isNotBlank()) {
                Icon(Icons.Filled.Send, contentDescription = "发送")
            }
        }
    }
}
