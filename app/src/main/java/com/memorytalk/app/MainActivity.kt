package com.memorytalk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

val WechatGreen = Color(0xFF07C160)
val BubbleMe = Color(0xFF95EC69)
val BgGray = Color(0xFFEDEDED)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = WechatGreen)) {
                val vm: MainViewModel = viewModel(factory = MainViewModel.factory(this@MainActivity))
                App(vm)
            }
        }
    }
}

@Composable
fun App(vm: MainViewModel) {
    var tab by remember { mutableStateOf("chats") }
    var screen by remember { mutableStateOf("main") }
    var chatPerson by remember { mutableStateOf<Person?>(null) }

    val editor by vm.editing.collectAsState()

    Box(Modifier.fillMaxSize().background(Color.White)) {
        when {
            editor != null -> PersonEditor(vm, editor!!)
            screen == "chat" && chatPerson != null -> ChatScreen(vm, chatPerson!!) { screen = "main" }
            screen == "settings" -> SettingsScreen(vm) { screen = "main" }
            screen == "about" -> AboutScreen { screen = "main" }
            else -> MainTabs(vm, tab, { tab = it }, { screen = "settings" }, { screen = "about" }) { p ->
                chatPerson = p
                vm.openChat(p.id)
                screen = "chat"
            }
        }
    }
}

@Composable
fun MainTabs(
    vm: MainViewModel, tab: String, onTab: (String) -> Unit,
    onSettings: () -> Unit, onAbout: () -> Unit, onChat: (Person) -> Unit
) {
    val persons by vm.persons.collectAsState()
    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color(0xFFF7F7F7), tonalElevation = 0.dp) {
                listOf("chats" to "微信", "contacts" to "通讯录", "discover" to "发现", "me" to "我").forEach { (key, label) ->
                    NavigationBarItem(
                        selected = tab == key,
                        onClick = { onTab(key) },
                        icon = { Text(if (key == "chats") "💬" else if (key == "contacts") "👥" else if (key == "discover") "🧭" else "👤", fontSize = 20.sp) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = WechatGreen, selectedTextColor = WechatGreen,
                            unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize().background(BgGray)) {
            when (tab) {
                "chats", "contacts" -> PersonList(vm, persons, tab == "contacts", onChat)
                "discover" -> Placeholder("通话、视频等功能", "后续版本加入")
                "me" -> MeScreen(vm, onSettings, onAbout)
            }
        }
    }
}

@Composable
fun TopBar(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).background(BgGray).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { Text(title, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
        action?.invoke()
    }
}

@Composable
fun PersonList(vm: MainViewModel, persons: List<Person>, isContacts: Boolean, onChat: (Person) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar(if (isContacts) "通讯录" else "微信") {
            IconButton(onClick = { vm.startNewPerson() }) { Icon(Icons.Default.Add, "新建", tint = Color.Black) }
        }
        if (persons.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("还没有人物\n点右上角 ＋ 新建一个", textAlign = TextAlign.Center, color = Color.Gray, lineHeight = 28.sp)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().background(Color.White)) {
                items(persons, key = { it.id }) { p ->
                    Row(
                        Modifier.fillMaxWidth().height(68.dp).background(Color.White).clickable {
                            if (isContacts) vm.editPerson(p.id) else onChat(p)
                        }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(46.dp).background(Color(0xFFC9C9C9), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                            Text((p.title.ifEmpty { p.name }.ifEmpty { "亲" }).take(1), color = Color.White, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(p.title.ifEmpty { p.name.ifEmpty { "未命名" } }, fontSize = 16.sp, color = Color.Black)
                            Text(p.traits.ifEmpty { p.relation.ifEmpty { "点击开始聊天" } }, fontSize = 13.sp, color = Color.Gray, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(vm: MainViewModel, person: Person, onBack: () -> Unit) {
    val messages by vm.messages.collectAsState()
    val sending by vm.sending.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) scope.launch { listState.animateScrollToItem(messages.size - 1) }
    }

    Column(Modifier.fillMaxSize().background(BgGray)) {
        Row(Modifier.fillMaxWidth().height(52.dp).background(BgGray).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", fontSize = 32.sp, modifier = Modifier.clickable(onClick = onBack))
            Spacer(Modifier.width(8.dp))
            Text(person.title.ifEmpty { person.name.ifEmpty { "聊天" } }, fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), state = listState) {
            items(messages) { m -> MessageBubble(m, person) }
            if (sending) item { Text("对方正在输入…", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(8.dp)) }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFFF7F7F7)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = input, onValueChange = { input = it },
                modifier = Modifier.weight(1f).height(52.dp).background(Color.White, RoundedCornerShape(5.dp)),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                placeholder = { Text("发消息…", fontSize = 14.sp) }
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = { if (input.isNotBlank() && !sending) { vm.send(input); input = "" } }, enabled = !sending,
                colors = ButtonDefaults.buttonColors(containerColor = WechatGreen), shape = RoundedCornerShape(5.dp)) {
                Text("发送", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun MessageBubble(m: ChatMessage, person: Person) {
    val isMe = m.role == "user"
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start) {
        if (!isMe) {
            Box(Modifier.size(36.dp).background(Color(0xFFC9C9C9), RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                Text((person.title.ifEmpty { person.name }.ifEmpty { "亲" }).take(1), color = Color.White, fontSize = 14.sp)
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            Modifier.widthIn(max = 280.dp).background(
                if (isMe) BubbleMe else Color.White, RoundedCornerShape(5.dp)
            ).padding(10.dp)
        ) { Text(m.content, fontSize = 15.sp, color = Color.Black) }
        if (isMe) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(36.dp).background(WechatGreen, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                Text("我", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun MeScreen(vm: MainViewModel, onSettings: () -> Unit, onAbout: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopBar("我")
        Column(Modifier.fillMaxSize().background(Color.White)) {
            listOf("新建人物" to { vm.startNewPerson() }, "设置" to onSettings, "关于与安全说明" to onAbout).forEach { (label, action) ->
                Row(Modifier.fillMaxWidth().height(52.dp).clickable(onClick = action).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text("›", fontSize = 22.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun Placeholder(a: String, b: String) {
    Column(Modifier.fillMaxSize()) {
        TopBar("发现")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(a, fontSize = 16.sp); Text(b, color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
