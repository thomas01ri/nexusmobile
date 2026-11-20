package com.nexus.mobile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF090C11)
private val Surface1 = Color(0xFF141A23)
private val Surface2 = Color(0xFF1D2633)
private val Red = Color(0xFFE53935)
private val Muted = Color(0xFF9AA7B8)
private data class Server(val name: String, val host: String, val port: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 12, 17)
        window.navigationBarColor = android.graphics.Color.rgb(9, 12, 17)
        setContent { NexusApp() }
    }
}

@Composable
private fun NexusApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("nexus_v102", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Home") }
    var nickname by remember { mutableStateOf(prefs.getString("nick", "Player_Username") ?: "Player_Username") }
    var servers by remember { mutableStateOf(readServers(prefs.getString("servers", "[]") ?: "[]")) }
    var showAdd by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var lowGraphics by remember { mutableStateOf(prefs.getBoolean("low", false)) }
    var showFps by remember { mutableStateOf(prefs.getBoolean("fps", true)) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var notice by remember { mutableStateOf("NEXUS siap digunakan • Build 1.0.2") }

    fun saveServers(list: List<Server>) {
        servers = list
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("name", it.name).put("host", it.host).put("port", it.port)) }
        prefs.edit().putString("servers", array.toString()).apply()
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Bg, surface = Surface1, onBackground = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                Header(onAbout = { showAbout = true })
                Box(Modifier.weight(1f)) {
                    when (page) {
                        "Home" -> HomePage(nickname, servers.size, notice, onConnect = { notice = "Multiplayer core belum tersedia. Belum ada koneksi game yang dijalankan." }, onServers = { page = "Servers" }, onProfile = { showProfile = true })
                        "Servers" -> ServersPage(servers, onAdd = { showAdd = true }, onSelect = { notice = "Server dipilih: ${it.name} (${it.host}:${it.port})" }, onDelete = { saveServers(servers - it); notice = "Server dihapus" })
                        "Settings" -> SettingsPage(lowGraphics, { lowGraphics = it; prefs.edit().putBoolean("low", it).apply() }, showFps, { showFps = it; prefs.edit().putBoolean("fps", it).apply() }, notifications, { notifications = it; prefs.edit().putBoolean("notifications", it).apply() }, onAbout = { showAbout = true })
                    }
                }
                NavigationBar(containerColor = Color(0xFF10151D), contentColor = Color.White) {
                    listOf("Home", "Servers", "Settings").forEach { item ->
                        val selected = page == item
                        NavigationBarItem(selected = selected, onClick = { page = item }, icon = { Icon(when(item) { "Home" -> Icons.Default.Home; "Servers" -> Icons.Default.Dns; else -> Icons.Default.Tune }, null) }, label = { Text(item) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Red, selectedTextColor = Color.White, indicatorColor = Color(0xFF382124), unselectedIconColor = Muted, unselectedTextColor = Muted))
                    }
                }
            }
        }
    }

    if (showAdd) AddServerDialog(onDismiss = { showAdd = false }, onSave = { name, host, port ->
        if (servers.any { it.host.equals(host, true) && it.port == port }) notice = "Server tersebut sudah tersimpan"
        else { saveServers(servers + Server(name, host, port)); notice = "${name} berhasil disimpan" }
        showAdd = false
    })
    if (showProfile) ProfileDialog(nickname, onDismiss = { showProfile = false }, onSave = { nickname = it; prefs.edit().putString("nick", it).apply(); showProfile = false; notice = "Nickname disimpan" })
    if (showAbout) AlertDialog(onDismissRequest = { showAbout = false }, containerColor = Surface1, title = { Text("NEXUS MOBILE", color = Color.White, fontWeight = FontWeight.Black) }, text = { Text("Version 1.0.2 • Android launcher prototype\n\nAplikasi ini menyediakan antarmuka, profil lokal, server list, dan preferensi. Belum menyertakan GTA San Andreas atau multiplayer SA-MP core.", color = Muted) }, confirmButton = { TextButton(onClick = { showAbout = false }) { Text("TUTUP", color = Red) } })
}

@Composable private fun Header(onAbout: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF211519), Bg))).padding(horizontal = 20.dp, vertical = 17.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painter = painterResource(id = R.drawable.nexus_logo), contentDescription = "NEXUS logo", modifier = Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text("NEXUS MOBILE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold); Text("MULTIPLAYER", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
        Surface(color = Color(0xFF2C1B20), shape = CircleShape) { Text(" v1.0.2 ", color = Color(0xFFFFAAA5), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) }
        IconButton(onClick = onAbout) { Icon(Icons.Default.Info, "About", tint = Muted) }
    }
}

@Composable private fun HomePage(nick: String, serverCount: Int, notice: String, onConnect: () -> Unit, onServers: () -> Unit, onProfile: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF482126), Color(0xFF171D27), Color(0xFF10151D)))).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WELCOME BACK", color = Color(0xFFFFAAA5), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Ready to play?", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("Your multiplayer hub on Android.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onConnect, colors = ButtonDefaults.buttonColors(containerColor = Red), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("QUICK CONNECT", fontWeight = FontWeight.Bold) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("SAVED SERVERS", serverCount.toString(), Icons.Default.Dns, Modifier.weight(1f))
            StatCard("CLIENT STATUS", "OFFLINE", Icons.Default.CloudOff, Modifier.weight(1f))
        }
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Person, null, tint = Red); Spacer(Modifier.width(8.dp)); Text("PLAYER PROFILE", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); TextButton(onClick = onProfile) { Text("EDIT", color = Red) } }
            Text(nick, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Nickname tersimpan di perangkat ini", color = Muted, fontSize = 11.sp)
        }
        PanelCard {
            Text("QUICK ACCESS", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("SERVER LIST", Icons.Default.Dns, onServers, Modifier.weight(1f))
                QuickTile("PROFILE", Icons.Default.AccountCircle, onProfile, Modifier.weight(1f))
            }
        }
        Text(notice, color = Muted, fontSize = 11.sp)
        Text("NEXUS IS AN INDEPENDENT COMMUNITY PROJECT", color = Color(0xFF596575), fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable private fun ServersPage(servers: List<Server>, onAdd: () -> Unit, onSelect: (Server) -> Unit, onDelete: (Server) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("SERVER BROWSER", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black); Text("Kelola alamat server favoritmu", color = Muted, fontSize = 12.sp) }; Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Icon(Icons.Default.Add, null); Text(" ADD") } }
        if (servers.isEmpty()) PanelCard { Icon(Icons.Default.Dns, null, tint = Muted, modifier = Modifier.size(34.dp)); Text("Belum ada server", color = Color.White, fontWeight = FontWeight.Bold); Text("Tambahkan IP atau hostname server untuk menyimpannya di daftar.", color = Muted, fontSize = 12.sp) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(servers, key = { "${it.host}:${it.port}" }) { server ->
                PanelCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF332126)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Dns, null, tint = Red) }
                        Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(server.name, color = Color.White, fontWeight = FontWeight.Bold); Text("${server.host}:${server.port}", color = Muted, fontSize = 11.sp) }
                        IconButton(onClick = { onSelect(server) }) { Icon(Icons.Default.CheckCircleOutline, "Select", tint = Color(0xFF76D6A0)) }
                        IconButton(onClick = { onDelete(server) }) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Red) }
                    }
                }
            }
        }
        Text("Catatan: daftar ini menyimpan alamat secara lokal. Live ping dan status pemain belum tersedia.", color = Muted, fontSize = 11.sp)
    }
}

@Composable private fun SettingsPage(low: Boolean, onLow: (Boolean) -> Unit, fps: Boolean, onFps: (Boolean) -> Unit, notifications: Boolean, onNotifications: (Boolean) -> Unit, onAbout: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SETTINGS", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        PanelCard {
            Text("PREFERENCES", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            SwitchRow("Low graphics preference", "Pilihan tersimpan; belum mengubah grafis game.", low, onLow)
            HorizontalDivider(color = Surface2)
            SwitchRow("FPS counter preference", "Pengaturan tampilan untuk pengembangan berikutnya.", fps, onFps)
            HorizontalDivider(color = Surface2)
            SwitchRow("Notifications", "Preferensi notifikasi aplikasi.", notifications, onNotifications)
        }
        PanelCard {
            Text("APPLICATION", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Info, null, tint = Red); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("NEXUS Mobile", color = Color.White, fontWeight = FontWeight.Bold); Text("Version 1.0.2 • Build 102", color = Muted, fontSize = 11.sp) }; TextButton(onClick = onAbout) { Text("ABOUT", color = Red) } }
        }
        Text("Pengaturan disimpan otomatis di perangkat.", color = Muted, fontSize = 11.sp)
    }
}

@Composable private fun PanelCard(content: @Composable ColumnScope.() -> Unit) { Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Surface1).padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content) }
@Composable private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) { Column(modifier.clip(RoundedCornerShape(16.dp)).background(Surface1).padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Icon(icon, null, tint = Red); Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp); Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp) } }
@Composable private fun QuickTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) { Column(modifier.clip(RoundedCornerShape(13.dp)).background(Surface2).clickable(onClick = onClick).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, tint = Red); Spacer(Modifier.height(7.dp)); Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(desc, color = Muted, fontSize = 10.sp) }; Switch(checked, onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red)) } }

@Composable private fun AddServerDialog(onDismiss: () -> Unit, onSave: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("My Server") }; var host by remember { mutableStateOf("") }; var port by remember { mutableStateOf("7777") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = Surface1, title = { Text("ADD SERVER", color = Color.White, fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Server name") }, singleLine = true); OutlinedTextField(host, { host = it }, label = { Text("IP / Hostname") }, singleLine = true); OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5) }, label = { Text("Port") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true) } }, confirmButton = { TextButton(onClick = { val p = port.toIntOrNull(); if (host.isNotBlank() && p != null && p in 1..65535) onSave(name.ifBlank { host }, host.trim(), p) }) { Text("SAVE", color = Red) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Muted) } })
}

@Composable private fun ProfileDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) { var nick by remember { mutableStateOf(current) }; AlertDialog(onDismissRequest = onDismiss, containerColor = Surface1, title = { Text("EDIT PROFILE", color = Color.White) }, text = { OutlinedTextField(nick, { nick = it.take(24) }, label = { Text("Nickname") }, singleLine = true) }, confirmButton = { TextButton(onClick = { if (nick.isNotBlank()) onSave(nick.trim()) }) { Text("SAVE", color = Red) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Muted) } }) }

private fun readServers(raw: String): List<Server> = try { val a = JSONArray(raw); (0 until a.length()).mapNotNull { i -> val o = a.optJSONObject(i) ?: return@mapNotNull null; val h = o.optString("host"); val p = o.optInt("port", 7777); if (h.isBlank() || p !in 1..65535) null else Server(o.optString("name", h), h, p) } } catch (_: Exception) { emptyList() }
