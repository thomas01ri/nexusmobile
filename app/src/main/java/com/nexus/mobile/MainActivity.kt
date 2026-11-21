package com.nexus.mobile

import android.app.Activity
import android.content.Context
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Bg = Color(0xFF090C11)
private val Surface1 = Color(0xFF141A23)
private val Surface2 = Color(0xFF1D2633)
private val Red = Color(0xFFE53935)
private val Muted = Color(0xFF9AA7B8)
private data class Server(val name: String, val host: String, val port: Int, val ucpApi: String = "")

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
    var page by remember { mutableStateOf(if (prefs.getBoolean("setup_complete", false)) "Home" else "Installer") }
    val activity = context as? Activity
    var lastExitPress by remember { mutableLongStateOf(0L) }
    var gtaInstalled by remember { mutableStateOf(GameInstallDetector.isInstalled(context)) }

    fun requestExit() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (lastExitPress != 0L && now - lastExitPress <= 2200L) {
            activity?.finish()
        } else {
            lastExitPress = now
            Toast.makeText(context, "Tekan sekali lagi untuk keluar dari NEXUS", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        if (page != "Home") {
            page = "Home"
            lastExitPress = 0L
        } else {
            requestExit()
        }
    }
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
    var notice by remember { mutableStateOf("NEXUS siap digunakan • Build 1.3.0") }
    var mods by remember { mutableStateOf(readMods(prefs.getString("mods", "[]") ?: "[]")) }
    var newMod by remember { mutableStateOf("") }

    fun saveBool(key: String, value: Boolean) { prefs.edit().putBoolean(key, value).apply() }
    fun saveServers(list: List<Server>) {
        servers = list
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("name", it.name).put("host", it.host).put("port", it.port).put("ucpApi", it.ucpApi)) }
        prefs.edit().putString("servers", array.toString()).apply()
    }
    fun saveMods(list: List<String>) {
        mods = list
        prefs.edit().putString("mods", JSONArray(list).toString()).apply()
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Bg, surface = Surface1, onBackground = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                Header(onAbout = { showAbout = true }, onExit = { requestExit() })
                Box(Modifier.weight(1f)) {
                    when (page) {
                        "Installer" -> InstallerPage(
                            context = context,
                            onGameDetected = { gtaInstalled = it },
                            onContinue = {
                                prefs.edit().putBoolean("setup_complete", true).apply()
                                page = "Home"
                                notice = if (gtaInstalled) "GTA San Andreas terdeteksi di perangkat." else "GTA SA belum terdeteksi. Pasang game resmi sebelum bermain."
                            }
                        )
                        "Home" -> HomePage()
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
        onSave = { name, host, port, ucpApi ->
            if (servers.any { it.host.equals(host, true) && it.port == port }) notice = "Server tersebut sudah tersimpan"
            else { saveServers(servers + Server(name, host, port, ucpApi)); notice = "$name berhasil disimpan" }
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
        text = { Text("Version 1.3.0 • Android client foundation\n\nAplikasi ini menyediakan antarmuka, profil lokal, server list, dan preferensi konfigurasi. Belum menyertakan GTA San Andreas engine, SA-MP multiplayer core, voice transport, atau loader mod native.", color = Muted) },
        confirmButton = { TextButton(onClick = { showAbout = false }) { Text("TUTUP", color = Red) } }
    )
    if (showRadial) RadialPreviewDialog(onDismiss = { showRadial = false }, onSelect = {
        notice = "Aksi radial dipilih: $it (preview UI)"
        showRadial = false
    })
}

@Composable
private fun Header(onAbout: () -> Unit, onExit: () -> Unit) {
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
            Text("SA-MP HUB", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
        Surface(color = Color(0xFF2C1B20), shape = CircleShape) {
            Text(" v1.3.0 ", color = Color(0xFFFFAAA5), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
        }
        IconButton(onClick = onExit) { Icon(Icons.Default.ExitToApp, "Keluar", tint = Muted) }
        IconButton(onClick = onAbout) { Icon(Icons.Default.Info, "About", tint = Muted) }
    }
}

@Composable
private fun HomePage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("NEXUS COMMUNITY", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("Gabung Discord resmi NEXUS", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://discord.gg/rtfc9x4qv")
                )
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Red),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("BUKA DISCORD", fontWeight = FontWeight.Bold)
        }
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
private fun SettingsPage(
    low: Boolean,
    onLow: (Boolean) -> Unit,
    fps: Boolean,
    onFps: (Boolean) -> Unit,
    notifications: Boolean,
    onNotifications: (Boolean) -> Unit,
    onOpen: (String) -> Unit,
    onAbout: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("SETTINGS", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text("Atur preferensi aplikasi NEXUS Mobile", color = Muted, fontSize = 12.sp)
        }

        PanelCard {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF332126)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Red, modifier = Modifier.size(27.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("NEXUS CONTROL CENTER", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Preferensi aplikasi dan modul client", color = Muted, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = Color(0xFF263A31), shape = RoundedCornerShape(8.dp)) {
                    Text("VERSION 1.3.0", color = Color(0xFF8DE0AF), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
                Surface(color = Surface2, shape = RoundedCornerShape(8.dp)) {
                    Text("BUILD 130", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                }
            }
        }

        PanelCard {
            Text("DISPLAY", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            SwitchRow("Low graphics preference", "Preferensi kualitas grafis untuk integrasi game.", low, onLow)
            HorizontalDivider(color = Surface2)
            SwitchRow("FPS counter preference", "Simpan pilihan untuk penghitung FPS.", fps, onFps)
            ModuleRow("Display options", "Buka preferensi tampilan tambahan", Icons.Default.DisplaySettings, { onOpen("Display") })
        }

        PanelCard {
            Text("KEYBOARD & MOUSE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            ModuleRow("Input configuration", "Keyboard, mouse, klik, dan radial menu", Icons.Default.Keyboard, { onOpen("Controls") })
            InfoNote("Pengaturan input tersedia sebagai konfigurasi. Integrasi input langsung ke game belum aktif.")
        }

        PanelCard {
            Text("CLIENT & MODULES", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            ModuleRow("Game Installer", "Deteksi game dan unduh komponen resmi", Icons.Default.Download, { onOpen("Installer") })
            HorizontalDivider(color = Surface2)
            ModuleRow("Mod Manager", "Catatan mod lokal", Icons.Default.Folder, { onOpen("Mods") })
            HorizontalDivider(color = Surface2)
            ModuleRow("Chat & Voice", "Preferensi chat dan audio", Icons.Default.Chat, { onOpen("Chat") })
        }

        PanelCard {
            Text("NOTIFICATIONS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            SwitchRow("App notifications preference", "Simpan preferensi notifikasi NEXUS.", notifications, onNotifications)
        }

        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("ABOUT NEXUS", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Informasi aplikasi, versi, dan lisensi", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = onAbout) {
                    Text("OPEN", color = Red, fontWeight = FontWeight.Bold)
                }
            }
        }
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
private fun InstallerPage(context: Context, onGameDetected: (Boolean) -> Unit, onContinue: () -> Unit) {
    var gameInstalled by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf("Memeriksa instalasi dan komponen NEXUS...") }
    val scope = rememberCoroutineScope()

    fun runInstallerCheck() {
        scope.launch {
            checking = true
            progress = 0
            gameInstalled = GameInstallDetector.isInstalled(context)
            onGameDetected(gameInstalled)
            message = if (gameInstalled) "GTA San Andreas terdeteksi." else "GTA San Andreas belum terdeteksi."
            try {
                val result = NexusInstaller.sync(context) { update ->
                    progress = update.percent
                    message = update.message
                }
                message = if (result.total == 0) {
                    "Pemeriksaan selesai. Belum ada komponen NEXUS yang dipublikasikan untuk diunduh."
                } else {
                    "Pemeriksaan selesai. ${result.completed}/${result.total} komponen berhasil diverifikasi."
                }
            } catch (_: Exception) {
                message = "Komponen NEXUS belum bisa diperiksa. Periksa koneksi lalu coba lagi."
            } finally {
                checking = false
            }
        }
    }

    LaunchedEffect(Unit) { runInstallerCheck() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("PERSIAPAN NEXUS", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Text("Pemeriksaan awal sebelum masuk ke launcher.", color = Muted, fontSize = 12.sp)
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (gameInstalled) Icons.Default.CheckCircle else Icons.Default.Warning, null, tint = if (gameInstalled) Color(0xFF76D6A0) else Color(0xFFFFC36B))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("GTA SAN ANDREAS", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(if (gameInstalled) "Terpasang pada perangkat" else "Belum ditemukan", color = Muted, fontSize = 11.sp)
                }
            }
            Text("NEXUS hanya mendeteksi game yang sudah dipasang. Data GTA tidak diunduh atau disertakan oleh NEXUS.", color = Muted, fontSize = 11.sp)
        }
        PanelCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, null, tint = Red)
                Spacer(Modifier.width(10.dp))
                Text("KOMPONEN NEXUS", color = Color.White, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = progress / 100f, modifier = Modifier.fillMaxWidth(), color = Red, trackColor = Surface2)
            Text(message, color = Muted, fontSize = 11.sp)
        }
        Button(onClick = { runInstallerCheck() }, enabled = !checking, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Surface2)) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(6.dp))
            Text("PERIKSA ULANG")
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red)) {
            Text("LANJUT KE NEXUS", fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ArrowForward, null)
        }
        Text("Pasang GTA San Andreas dari toko resmi secara terpisah. Fitur masuk server akan dibuka setelah client multiplayer yang kompatibel dan berlisensi berhasil diintegrasikan.", color = Muted, fontSize = 10.sp)
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
private fun AddServerDialog(onDismiss: () -> Unit, onSave: (String, String, Int, String) -> Unit) {
    var name by remember { mutableStateOf("My Server") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("7777") }
    var ucpApi by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Surface1,
        title = { Text("ADD SERVER", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Server name") }, singleLine = true)
                OutlinedTextField(host, { host = it }, label = { Text("IP / Hostname") }, singleLine = true)
                OutlinedTextField(port, { port = it.filter(Char::isDigit).take(5) }, label = { Text("Port") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(ucpApi, { ucpApi = it.trim() }, label = { Text("UCP API HTTPS (opsional)") }, placeholder = { Text("https://servermu.id/api/login") }, singleLine = true)
                Text("Endpoint UCP harus disediakan pemilik server. Gunakan HTTPS; format API tiap server bisa berbeda.", color = Muted, fontSize = 10.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { val p = port.toIntOrNull(); if (host.isNotBlank() && p != null && p in 1..65535 && (ucpApi.isBlank() || ucpApi.startsWith("https://"))) onSave(name.ifBlank { host }, host.trim(), p, ucpApi.trim()) }) { Text("SAVE", color = Red) }
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
        if (host.isBlank() || port !in 1..65535) null else Server(obj.optString("name", host), host, port, obj.optString("ucpApi", ""))
    }
} catch (_: Exception) { emptyList() }

private fun readMods(raw: String): List<String> = try {
    val array = JSONArray(raw)
    (0 until array.length()).mapNotNull { index -> array.optString(index).takeIf { it.isNotBlank() } }
} catch (_: Exception) { emptyList() }
