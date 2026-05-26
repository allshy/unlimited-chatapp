package com.wuxianzhi.chat.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.wuxianzhi.chat.App
import com.wuxianzhi.chat.data.DefaultModels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    firstRun: Boolean,
    onDone: () -> Unit,
) {
    val app = App.instance
    val settings = app.settings

    val apiKey by settings.apiKey.collectAsState()
    val baseUrl by settings.baseUrl.collectAsState()
    val defaultModel by settings.defaultModel.collectAsState()
    val temperature by settings.temperature.collectAsState()
    val streaming by settings.streaming.collectAsState()

    var apiKeyDraft by remember { mutableStateOf(apiKey) }
    var baseUrlDraft by remember { mutableStateOf(baseUrl) }
    var modelDraft by remember { mutableStateOf(defaultModel) }
    var showKey by remember { mutableStateOf(false) }
    var modelMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (firstRun) "欢迎 · 配置" else "设置") },
                navigationIcon = {
                    if (!firstRun) {
                        IconButton(onClick = onDone) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (firstRun) {
                Text(
                    "首次使用：填入 API Key 即可开始聊天。所有配置加密保存在本机，不会上传。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("接口", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = apiKeyDraft,
                        onValueChange = { apiKeyDraft = it },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(
                                    if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    )

                    OutlinedTextField(
                        value = baseUrlDraft,
                        onValueChange = { baseUrlDraft = it },
                        label = { Text("Base URL（OpenAI 兼容接口）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Text(
                        "默认接入 NVIDIA NIM。OpenAI / DeepSeek / OpenRouter 等同样兼容，替换为它们的 v1 地址即可。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("模型 & 生成", style = MaterialTheme.typography.titleMedium)

                    ExposedDropdownMenuBox(
                        expanded = modelMenuOpen,
                        onExpandedChange = { modelMenuOpen = !modelMenuOpen },
                    ) {
                        OutlinedTextField(
                            value = modelDraft,
                            onValueChange = { modelDraft = it },
                            label = { Text("默认模型 ID") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelMenuOpen) },
                        )
                        androidx.compose.material3.ExposedDropdownMenu(
                            expanded = modelMenuOpen,
                            onDismissRequest = { modelMenuOpen = false },
                        ) {
                            DefaultModels.list.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("${m.label}  ·  ${m.id}") },
                                    onClick = {
                                        modelDraft = m.id
                                        modelMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Column {
                        Text("Temperature: ${"%.2f".format(temperature)}")
                        Slider(
                            value = temperature,
                            onValueChange = { settings.setTemperature(it) },
                            valueRange = 0f..1.5f,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = streaming, onCheckedChange = { settings.setStreaming(it) })
                        Text("  流式输出", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "基于开源项目 unlimited-ai 的安卓重写版。聊天记录与配置仅存于本机。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (!firstRun) {
                    TextButton(onClick = onDone) { Text("取消") }
                }
                Button(
                    onClick = {
                        settings.setApiKey(apiKeyDraft)
                        settings.setBaseUrl(baseUrlDraft.ifBlank { DefaultModels.DEFAULT_BASE_URL })
                        settings.setDefaultModel(modelDraft.ifBlank { DefaultModels.DEFAULT_ID })
                        onDone()
                    },
                    enabled = apiKeyDraft.isNotBlank(),
                ) {
                    Text(if (firstRun) "开始聊天" else "保存")
                }
            }
        }
    }
}
