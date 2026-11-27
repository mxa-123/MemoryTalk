package com.memorytalk.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit, singleLine: Boolean = true, lines: Int = 3) {
    Column(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 14.dp, vertical = 10.dp)) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        val fieldColors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
        if (singleLine) {
            TextField(value, onChange, Modifier.fillMaxWidth().padding(top = 2.dp), singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp), colors = fieldColors)
        } else {
            TextField(value, onChange, Modifier.fillMaxWidth().heightIn(min = (lines * 24).dp).padding(top = 2.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp, lineHeight = 24.sp), colors = fieldColors)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F0F0)))
    }
}

@Composable
fun PersonEditor(vm: MainViewModel, person: Person) {
    var p by remember(person.id) { mutableStateOf(person) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(52.dp).background(BgGray).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 32.sp, modifier = Modifier.clickable { vm.closeEditor() })
            Spacer(Modifier.width(8.dp))
            Text(if (p.id.isBlank()) "新建人物" else "编辑人物", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("保存", color = WechatGreen, fontSize = 16.sp, modifier = Modifier.clickable {
                if (p.title.isBlank() && p.name.isBlank()) return@clickable
                vm.savePerson(p)
            })
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item { Field("称呼（例如：奶奶）", p.title, { p = p.copy(title = it) }) }
            item { Field("名字 / 小名", p.name, { p = p.copy(name = it) }) }
            item { Field("关系", p.relation, { p = p.copy(relation = it) }) }
            item { Field("生卒年份", p.years, { p = p.copy(years = it) }) }
            item { Field("性格特点", p.traits, { p = p.copy(traits = it) }, false, 3) }
            item { Field("说话风格 / 口头禅", p.speech, { p = p.copy(speech = it) }, false, 3) }
            item { Field("生活习惯", p.habits, { p = p.copy(habits = it) }, false, 3) }
            item { Field("共同回忆（越具体越像）", p.memories, { p = p.copy(memories = it) }, false, 4) }
            item { Field("不愿谈及", p.avoid, { p = p.copy(avoid = it) }) }
            item { Field("补充说明", p.extra, { p = p.copy(extra = it) }, false, 3) }
            if (p.id.isNotBlank()) {
                item {
                    Button(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE64340)),
                        modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(6.dp)
                    ) { Text("删除这个人物和全部聊天记录") }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("确认删除") },
            text = { Text("删除后无法恢复，包括全部聊天记录。") },
            confirmButton = { TextButton(onClick = { vm.deletePerson(p.id) }) { Text("删除", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsState()
    var draft by remember { mutableStateOf(s) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(52.dp).background(BgGray).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 32.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(8.dp))
            Text("设置", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("保存", color = WechatGreen, fontSize = 16.sp, modifier = Modifier.clickable {
                vm.updateSettings(draft); saved = true
            })
        }
        if (saved) {
            Text("已保存", color = WechatGreen, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item { Field("API 地址", draft.baseUrl, { draft = draft.copy(baseUrl = it) }) }
            item { Field("聊天模型", draft.model, { draft = draft.copy(model = it) }) }
            item { Field("API Key（只存本机）", draft.apiKey, { draft = draft.copy(apiKey = it) }) }
            item { Field("审查模型（留空同聊天模型）", draft.monitorModel, { draft = draft.copy(monitorModel = it) }) }
            item {
                Row(Modifier.fillMaxWidth().background(Color.White).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("审查 AI 安全护栏", fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(checked = draft.monitorEnabled, onCheckedChange = { draft = draft.copy(monitorEnabled = it) })
                }
            }
            item {
                Text(
                    "审查 AI 会检查每条回复，拦截身份欺骗、危险引导、编造事实等。关闭后仅保留规则层防护。\n\n云端模式下，对话内容会发送到所选服务。涉及隐私请勿使用。",
                    color = Color(0xFF7A5B00), fontSize = 13.sp, lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth().padding(14.dp).background(Color(0xFFFFF3CD), RoundedCornerShape(5.dp)).padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(52.dp).background(BgGray).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 32.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(8.dp))
            Text("关于", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text("MemoryTalk", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("基于你提供的资料生成的 AI 模拟，不是逝者本人。", fontSize = 14.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text("内置双 AI 安全护栏：聊天 AI 生成回复后，由审查 AI 审核，拦截身份欺骗、危险引导、编造事实、情感操控。审查失败则不推送。", fontSize = 14.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text("遇到轻生或自残念头，请联系身边亲友，或拨打心理援助热线 400-161-9995（北京 010-82951332），紧急拨打 110 / 120。", color = Color.Gray, fontSize = 13.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(8.dp))
            Text("本软件不能替代真实人际关系、心理咨询或医疗帮助。", color = Color.Gray, fontSize = 13.sp, lineHeight = 22.sp)
        }
    }
}
