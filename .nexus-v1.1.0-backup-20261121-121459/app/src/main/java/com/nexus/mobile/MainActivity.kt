package com.nexus.mobile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val prefs = remember { context.getSharedPreferences("nexus_v110", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Home") }
    var nickname by remember { mutableStateOf(prefs.getString("nick", "Player_Username") ?: "Player_Username") }
    var servers by remember { mutableStateOf(readServers(prefs.getString("servers", "[]") ?: "[]")) }
    var showAdd by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showRadial by remember { mutableStateOf(false) }
    var lowGraphics by remember { mutableStateOf(prefs.getBoolean("low", false)) }
    var showFps by remember { mutableStateOf(prefs.getBoolean("fps", true)) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var mouseEnabled by remember { mutableStateOf(prefs.getBoolean("mouse_enabled", true)) }
    var keyboardEnabled by remember { mutableStateOf(prefs.getBoolean("keyboard_enabled", true)) }
    var rightClickFire by remember { mutableStateOf(prefs.getBoolean("right_fire", true)) }
    var leftClickAim by remember { mutableStateOf(prefs.getBoolean("left_aim", true)) }
    var fullscreen by remember { mutableStateOf(prefs.getBoolean("fullscreen", false)) }
    var voiceEnabled by remember { mutableStateOf(prefs.getBoolean("voice_enabled", false)) }
    var pushToTalk by remember { mutableStateOf(prefs.getBoolean("push_to_talk", true)) }
    var chatTimestamp by remember { mutableStateOf(prefs.getBoolean("chat_timestamp", true)) }
    var chatBackground by remember { mutableStateOf(prefs.getBoolean("chat_background", true)) }
    var radialKey by remember { mutableStateOf(prefs.getString("radial_key", "F11") ?: "F11") }
    var notice by remember { mutableStateOf("NEXUS siap digunakan • Build 1.1.0") }
    var mods by remember { mutableStateOf(readMods(prefs.getString("mods", "[]") ?: "[]")) }
    var newMod by remember { mutableStateOf("") }

    fun saveBool(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun saveServers(list: List<Server>) {
        servers = list
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("name", it.name).put("host", it.host).put("port", it.port)) }
        prefs.edit().putString("servers", array.toString()).apply()
    }
    fun saveMods(list: List<String>) {
        mods = list
        prefs.edit().putString("mods", JSONArray(list).toString()).apply()
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Bg, surface = Surface1, onBackground = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                Header(onAbout = { showAbout = true })
                Box(Modifier.weight(1f)) {
                    when (page) {
                        "Home" -> HomePage(
                            nickname, servers.size, notice,
                            onConnect = { notice = "Multiplayer core belum tersedia. Belum ada koneksi game yang dijalankan." },
                            onServers = { page = "Servers" },
                            onProfile = { showProfile = true },
                            onRadial = { showRadial = true }
                        )
                        "Servers" -> ServersPage(
                            servers,
                            onAdd = { showAdd = true },
                            onSelect = { notice = "Server dipilih: ${it.name} (${it.host}:${it.port})" },
                            onDelete = { saveServers(servers - it); notice = "Server dihapus" }
                        )
                        "Settings" -> SettingsPage(
                            lowGraphics, { lowGraphics = it; saveBool("low", it) },
                            showFps, { showFps = it; saveBool("fps", it) },
                            notifications, { notifications = it; saveBool("notifications", it) },
                            onOpen = { page = it }, onAbout = { showAbout = true }
                        )
                        "Controls" -> ControlsPage(
                            keyboardEnabled, { keyboardEnabled = it; saveBool("keyboard_enabled", it) },
                            mouseEnabled, { mouseEnabled = it; saveBool("mouse_enabled", it) },
                            rightClickFire, { rightClickFire = it; saveBool("right_fire", it) },
                            leftClickAim, { leftClickAim = it; saveBool("left_aim", it) },
                            radialKey, { radialKey = it; prefs.edit().putString("radial_key", it).apply() },
                            onPreviewRadial = { showRadial = true }
                        )
                        "Mods" -> ModsPage(
                            mods, newMod, { newMod = it },
                            onAdd = {
                                val value = newMod.trim()
                                if (value.isNotEmpty() && !mods.contains(value)) saveMods(mods + value)
                                newMod = ""
                            },
                            onDelete = { saveMods(mods - it) }
                        )
                        "Chat" -> ChatPage(
                            chatTimestamp, { chatTimestamp = it; saveBool("chat_timestamp", it) },
                            chatBackground, { chatBackground = it; saveBool("chat_background", it) }
                        )
                        "Voice" -> VoicePage(
                            voiceEnabled, { voiceEnabled = it; saveBool("voice_enabled", it) },
                            pushToTalk, { pushToTalk = it; saveBool("push_to_talk", it) }
                        )
                        "Display" -> DisplayPage(fullscreen, {
                            fullscreen = it
                            saveBool("fullscreen", it)
                            notice = if (it) "Preferensi fullscreen aktif; penerapan ke game memerlukan integrasi native." else "Preferensi fullscreen nonaktif"
                        }, lowGraphics, { lowGraphics = it; saveBool("low", it) }, showFps, { showFps = it; saveBool("fps", it) })
                    }
                }
                NavigationBar(containerColor = Color(0xFF10151D), contentColor = Color.White) {
                    listOf("Home", "Servers", "Settings").forEach { item ->
                        val selected = page == item
                        NavigationBarItem(
                            selected = selected, onClick = { page = item },
                            icon = { Icon(when (item) { "Home" -> Icons.Default.Home; "Servers" -> Icons.Default.Dns; else -> Icons.Default.Tune }, null) },
                            label = { Text(item) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Red, selectedTextColor = Color.White,
                                indicatorColor = Color(0xFF382124),
                                unselectedIconColor = Muted, unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        }
    }

    if (showAdd) AddServerDialog(
        onDismiss = { showAdd = false },
        onSave = { name, host, port ->
            if (servers.any { it.host.equals(host, true) && it.port == port }) notice = "Server tersebut sudah tersimpan"
            else { saveServers(servers + Server(name, host, port)); notice = "$name berhasil disimpan" }
            showAdd = false
        }
    )
    if (showProfile) ProfileDialog(
        nickname, onDismiss = { showProfile = false },
        onSave = { nickname = it; prefs.edit().putString("nick", it).apply(); showProfile = false; notice = "Nickname disimpan" }
    )
    if (showAbout) AlertDialog(
        onDismissRequest = { showAbout = false }, containerColor = Surface1,
        title = { Text("NEXUS MOBILE", color = Color.White, fontWeight = FontWeight.Black) },
        text = { Text("Version 1.1.0 • Android client UI prototype\n\nAplikasi ini menyediakan antarmuka, profil lokal, server list, dan preferensi konfigurasi. Belum menyertakan GTA San Andreas engine, SA-MP multiplayer core, voice transport, atau loader mod native.", color = Muted) },
        confirmButton = { TextButton(onClick = { showAbout = false }) { Text("TUTUP", color = Red) } }
    )
    if (showRadial) RadialPreviewDialog(onDismiss = { showRadial = false }, onSelect = {
        notice = "Aksi radial dipilih: $it (preview UI)"
        showRadial = false
    })
}

@Composable
private fun Header(onAbout: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF211519), Bg)))
            .padding(horizontal = 20.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.nexus_logo),
            contentDescription = "NEXUS logo", modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("NEXUS MOBILE", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("MULTIPLAYER", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Surface(color = Color(0xFF2C1B20), shape = CircleShape) {
            Text(" v1.1.0 ", color = Color(0xFFFFAAA5), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
        }
        IconButton(onClick = onAbout) { Icon(Icons.Default.Info, "About", tint = Muted) }
    }
}

@Composable
private fun HomePage(nick: String, serverCount: Int, notice: String, onConnect: () -> Unit, onServers: () -> Unit, onProfile: () -> Unit, onRadial: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF482126), Color(0xFF171D27), Color(0xFF10151D)))).padding(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WELCOME BACK", color = Color(0xFFFFAAA5), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text("Ready to play?", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("Your multiplayer hub on Android.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Button(onClick = onConnect, colors = ButtonDefaults.buttonColors(containerColor = Red), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("QUICK CONNECT", fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("SAVED SERVERS", serverCount.toString(), Icons.Default.Dns, Modifier.weight(1f))
            StatCard("CLIENT STATUS", "OFFLINE", Icons.Default.CloudOff, Modifier.weight(1f))
        }
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = Red); Spacer(Modifier.width(8.dp))
                Text("PLAYER PROFILE", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f)); TextButton(onClick = onProfile) { Text("EDIT", color = Red) }
            }
            Text(nick, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Nickname tersimpan di perangkat ini", color = Muted, fontSize = 11.sp)
        }
        PanelCard {
            Text("QUICK ACCESS", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("SERVER LIST", Icons.Default.Dns, onServers, Modifier.weight(1f))
                QuickTile("PROFILE", Icons.Default.AccountCircle, onProfile, Modifier.weight(1f))
                QuickTile("RADIAL F11", Icons.Default.GridView, onRadial, Modifier.weight(1f))
            }
        }
        Text(notice, color = Muted, fontSize = 11.sp)
        Text("NEXUS IS AN INDEPENDENT COMMUNITY PROJECT", color = Color(0xFF596575), fontSize = 9.sp, letterSpacing = 1.sp)
    }
}

@Composable
private fun ServersPage(servers: List<Server>, onAdd: () -> Unit, onSelect: (Server) -> Unit, onDelete: (Server) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SERVER BROWSER", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("Kelola alamat server favoritmu", color = Muted, fontSize = 12.sp)
            }
            Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Icon(Icons.Default.Add, null); Text(" ADD") }
        }
        if (servers.isEmpty()) PanelCard {
            Icon(Icons.Default.Dns, null, tint = Muted, modifier = Modifier.size(34.dp))
            Text("Belum ada server", color = Color.White, fontWeight = FontWeight.Bold)
            Text("Tambahkan IP atau hostname server untuk menyimpannya di daftar.", color = Muted, fontSize = 12.sp)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(servers, key = { "${it.host}:${it.port}" }) { server ->
                PanelCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF332126)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Dns, null, tint = Red) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(server.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("${server.host}:${server.port}", color = Muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onSelect(server) }) { Icon(Icons.Default.CheckCircleOutline, "Select", tint = Color(0xFF76D6A0)) }
                        IconButton(onClick = { onDelete(server) }) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Red) }
                    }
                }
            }
        }
        Text("Catatan: daftar ini menyimpan alamat secara lokal. Live ping dan status pemain belum tersedia.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun SettingsPage(low: Boolean, onLow: (Boolean) -> Unit, fps: Boolean, onFps: (Boolean) -> Unit, notifications: Boolean, onNotifications: (Boolean) -> Unit, onOpen: (String) -> Unit, onAbout: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SETTINGS", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        PanelCard {
            Text("PREFERENCES", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            SwitchRow("Low graphics preference", "Pilihan tersimpan; belum mengubah grafis game.", low, onLow)
            HorizontalDivider(color = Surface2)
            SwitchRow("FPS counter preference", "Preferensi tampilan untuk integrasi berikutnya.", fps, onFps)
            HorizontalDivider(color = Surface2)
            SwitchRow("Notifications", "Preferensi notifikasi aplikasi.", notifications, onNotifications)
        }
        PanelCard {
            Text("CLIENT MODULES", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
            ModuleRow("Keyboard & Mouse", "Atur input dan tombol radial", Icons.Default.Keyboard, { onOpen("Controls") })
            ModuleRow("Mod Manager", "Daftar mod lokal (belum dimuat ke game)", Icons.Default.Folder, { onOpen("Mods") })
            ModuleRow("Custom Chat", "Preferensi tampilan chat", Icons.Default.Chat, { onOpen("Chat") })
            ModuleRow("Voice", "Preferensi voice client", Icons.Default.Mic, { onOpen("Voice") })
            ModuleRow("Display", "Fullscreen dan preferensi grafis", Icons.Default.DisplaySettings, { onOpen("Display") })
        }
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Red); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("NEXUS Mobile", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Version 1.1.0 • Build 110", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = onAbout) { Text("ABOUT", color = Red) }
            }
        }
        Text("Pengaturan disimpan otomatis di perangkat.", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ControlsPage(keyboard: Boolean, onKeyboard: (Boolean) -> Unit, mouse: Boolean, onMouse: (Boolean) -> Unit, rightFire: Boolean, onRightFire: (Boolean) -> Unit, leftAim: Boolean, onLeftAim: (Boolean) -> Unit, radialKey: String, onRadialKey: (String) -> Unit, onPreviewRadial: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle("KEYBOARD & MOUSE", "Konfigurasi preferensi input client")
        PanelCard {
            SwitchRow("Keyboard support preference", "Menyimpan pilihan keyboard eksternal.", keyboard, onKeyboard)
            HorizontalDivider(color = Surface2)
            SwitchRow("Mouse support preference", "Menyimpan pilihan mouse eksternal.", mouse, onMouse)
        }
        PanelCard {
            Text("MOUSE MAPPING", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            SwitchRow("Right-click to shoot", "Binding pilihan untuk integrasi native.", rightFire, onRightFire)
            HorizontalDivider(color = Surface2)
            SwitchRow("Left-click to aim", "Binding pilihan untuk integrasi native.", leftAim, onLeftAim)
        }
        PanelCard {
            Text("RADIAL MENU", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Tombol pilihan: $radialKey", color = Color.White, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("F10", "F11", "F12").forEach { key ->
                    FilterChip(selected = radialKey == key, onClick = { onRadialKey(key) }, label = { Text(key) })
                }
            }
            Button(onClick = onPreviewRadial, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Icon(Icons.Default.GridView, null); Spacer(Modifier.width(6.dp)); Text("PREVIEW RADIAL MENU") }
        }
        InfoNote("Pengaturan ini tersimpan sebagai konfigurasi. Pengiriman input ke GTA SA-MP membutuhkan integrasi engine/native yang belum tersedia.")
    }
}

@Composable
private fun ModsPage(mods: List<String>, newMod: String, onNewMod: (String) -> Unit, onAdd: () -> Unit, onDelete: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle("MOD MANAGER", "Catat mod yang ingin dikelola")
        PanelCard {
            Text("ADD MOD ENTRY", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = newMod, onValueChange = onNewMod,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama mod / path file") },
                placeholder = { Text("contoh: modloader/skin") },
                singleLine = true
            )
            Button(onClick = onAdd, enabled = newMod.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("TAMBAH")
            }
        }
        if (mods.isEmpty()) PanelCard { Text("Belum ada entri mod.", color = Color.White); Text("Kamu bisa mencatat nama atau lokasi file di sini.", color = Muted, fontSize = 12.sp) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
            items(mods) { item ->
                PanelCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Extension, null, tint = Red)
                        Spacer(Modifier.width(10.dp))
                        Text(item, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDelete(item) }) { Icon(Icons.Default.DeleteOutline, "Hapus", tint = Red) }
                    }
                }
            }
        }
        InfoNote("Mod Manager ini baru menyimpan daftar nama/path secara lokal. Belum menyalin, memasang, atau memuat file mod ke game.")
    }
}

@Composable
private fun ChatPage(timestamp: Boolean, onTimestamp: (Boolean) -> Unit, background: Boolean, onBackground: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle("CUSTOM CHAT", "Atur tampilan chat untuk rancangan client")
        PanelCard {
            SwitchRow("Show timestamps", "Tampilkan waktu pada baris chat (preferensi).", timestamp, onTimestamp)
            HorizontalDivider(color = Surface2)
            SwitchRow("Chat background", "Gunakan latar panel pada area chat (preferensi).", background, onBackground)
        }
        PanelCard {
            Text("CHAT PREVIEW", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (background) Surface2 else Bg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${if (timestamp) "[04:10] " else ""}NEXUS: Selamat datang!", color = Color.White, fontSize = 12.sp)
                Text("${if (timestamp) "[04:10] " else ""}SERVER: Chat preview lokal", color = Color(0xFF9FE0B2), fontSize = 12.sp)
            }
        }
        InfoNote("Ini hanya preview tampilan. Pesan belum dikirim atau diterima dari server SA-MP.")
    }
}

@Composable
private fun VoicePage(enabled: Boolean, onEnabled: (Boolean) -> Unit, pushToTalk: Boolean, onPushToTalk: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle("VOICE SETTINGS", "Preferensi komunikasi suara")
        PanelCard {
            SwitchRow("Enable voice preference", "Simpan pilihan untuk fitur voice.", enabled, onEnabled)
            HorizontalDivider(color = Surface2)
            SwitchRow("Push to talk", "Mode tekan-untuk-bicara sebagai preferensi.", pushToTalk, onPushToTalk)
        }
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, null, tint = Red, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(if (enabled) "VOICE PREFERENCE ON" else "VOICE PREFERENCE OFF", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Transport audio belum terhubung", color = Muted, fontSize = 11.sp)
                }
            }
        }
        InfoNote("pVoice / voice chat belum aktif. Fitur sebenarnya memerlukan audio capture, izin mikrofon, server relay, dan integrasi protokol yang kompatibel.")
    }
}

@Composable
private fun DisplayPage(fullscreen: Boolean, onFullscreen: (Boolean) -> Unit, low: Boolean, onLow: (Boolean) -> Unit, fps: Boolean, onFps: (Boolean) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle("DISPLAY", "Preferensi tampilan client")
        PanelCard {
            SwitchRow("Fullscreen preference", "Simpan pilihan fullscreen untuk integrasi native.", fullscreen, onFullscreen)
            HorizontalDivider(color = Surface2)
            SwitchRow("Low graphics preference", "Pilihan grafis untuk pengembangan berikutnya.", low, onLow)
            HorizontalDivider(color = Surface2)
            SwitchRow("FPS counter preference", "Pilihan tampilan penghitung FPS.", fps, onFps)
        }
        PanelCard {
            Text("RESOLUTION PRESETS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("Auto / mengikuti layar perangkat", color = Color.White)
            Text("Preset resolusi game belum diterapkan karena renderer game belum terintegrasi.", color = Muted, fontSize = 11.sp)
        }
        InfoNote("Fullscreen dan resolusi game yang sebenarnya perlu diterapkan pada jendela renderer GTA, bukan hanya pada halaman pengaturan ini.")
    }
}

@Composable
private fun RadialPreviewDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    val actions = listOf("Inventory", "Animations", "Phone", "Vehicle", "Settings", "Close")
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("NEXUS RADIAL MENU", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Preview menu radial • tombol F11 disimpan sebagai binding pilihan", color = Muted, fontSize = 11.sp)
                actions.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { action ->
                            OutlinedButton(onClick = { onSelect(action) }, modifier = Modifier.weight(1f)) { Text(action, fontSize = 11.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("TUTUP", color = Red) } }
    )
}

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun InfoNote(text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF20202A)).padding(12.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Default.Info, null, tint = Color(0xFFFFAAA5), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ModuleRow(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Red)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, color = Muted, fontSize = 10.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = Muted)
    }
}

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Surface1).padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp), content = content)
}

@Composable
private fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Surface1).padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Icon(icon, null, tint = Red)
        Text(label, color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp)
    }
}

@Composable
private fun QuickTile(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Surface2).clickable(onClick = onClick).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Red)
        Spacer(Modifier.height(7.dp))
        Text(title, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Muted, fontSize = 10.sp)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red))
    }
}

@Composable
private fun AddServerDialog(onDismiss: () -> Unit, onSave: (String, String, Int) -> Unit) {
    var name by remember { mutableStateOf("My Server") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("7777") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Surface1,
        title = { Text("ADD SERVER", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Server name") }, singleLine = true)
                OutlinedTextField(host, { host = it }, label = { Text("IP / Hostname") }, singleLine = true)
                OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5) }, label = { Text("Port") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { val p = port.toIntOrNull(); if (host.isNotBlank() && p != null && p in 1..65535) onSave(name.ifBlank { host }, host.trim(), p) }) { Text("SAVE", color = Red) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Muted) } }
    )
}

@Composable
private fun ProfileDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var nick by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Surface1,
        title = { Text("EDIT PROFILE", color = Color.White) },
        text = { OutlinedTextField(nick, { nick = it.take(24) }, label = { Text("Nickname") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (nick.isNotBlank()) onSave(nick.trim()) }) { Text("SAVE", color = Red) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = Muted) } }
    )
}

private fun readServers(raw: String): List<Server> = try {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { index ->
        val obj = array.optJSONObject(index) ?: return@mapNotNull null
        val host = obj.optString("host")
        val port = obj.optInt("port", 7777)
        if (host.isBlank() || port !in 1..65535) null else Server(obj.optString("name", host), host, port)
    }
} catch (_: Exception) { emptyList() }

private fun readMods(raw: String): List<String> = try {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
} catch (_: Exception) { emptyList() }
