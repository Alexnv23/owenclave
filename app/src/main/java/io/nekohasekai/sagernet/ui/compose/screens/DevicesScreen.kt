package io.nekohasekai.sagernet.ui.compose.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DGold = Color(0xFFD9B95C)
private val DCardBg = Color(0xFF15130F)
private val DCardBorder = Color(0x33D9B95C)
private val DDanger = Color(0xFFE5484D)

/**
 * Экран «Мои устройства» — как на сайте: список подключённых устройств и их удаление.
 * Данные: GET /api/app/stats/{token} (devices[]), удаление: POST /api/app/devices/{token}/{id}/revoke.
 */
@Composable
fun DevicesScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val token = remember { AccountApi.getToken(context) ?: "" }

    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<AccountApi.Device>>(emptyList()) }
    var limit by remember { mutableIntStateOf(2) }
    var loadError by remember { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<AccountApi.Device?>(null) }
    var confirmAll by remember { mutableStateOf(false) }

    suspend fun load() {
        loading = true
        loadError = false
        val stats = withContext(Dispatchers.IO) { if (token.isBlank()) null else AccountApi.fetch(token) }
        if (stats != null) {
            devices = stats.devices
            limit = stats.devicesLimit
        } else {
            loadError = true
        }
        loading = false
    }

    LaunchedEffect(token) { load() }

    fun removeOne(dev: AccountApi.Device) {
        scope.launch {
            busy = true
            val (ok, msg) = withContext(Dispatchers.IO) { AccountApi.revokeDevice(token, dev.id) }
            Toast.makeText(context, if (ok) "✅ $msg" else "❌ $msg", Toast.LENGTH_SHORT).show()
            if (ok) load()
            busy = false
        }
    }

    fun removeAll() {
        scope.launch {
            busy = true
            var okCount = 0
            for (d in devices) {
                val (ok, _) = withContext(Dispatchers.IO) { AccountApi.revokeDevice(token, d.id) }
                if (ok) okCount++
            }
            Toast.makeText(context, "✅ Удалено: $okCount", Toast.LENGTH_SHORT).show()
            load()
            busy = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp, bottom = 120.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Мои устройства",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { scope.launch { load() } }, enabled = !loading && !busy) {
                Icon(Icons.Filled.Refresh, contentDescription = "Обновить", tint = DGold)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            if (loading) "Загружаю…" else "Подключено: ${devices.size} из $limit",
            color = DGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp),
        )

        when {
            token.isBlank() -> InfoCard("Аккаунт не привязан", "Добавь подписку из личного кабинета кнопкой «Добавить в приложение SuperNet» — тогда здесь появятся устройства.")
            loading -> Box(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DGold)
            }
            loadError -> InfoCard("Не удалось загрузить", "Проверь интернет и нажми «Обновить» справа вверху.")
            devices.isEmpty() -> InfoCard("Нет подключённых устройств", "Как только подключишься — устройство появится здесь.")
            else -> {
                devices.forEach { dev ->
                    DeviceCard(dev, enabled = !busy) { confirmOne = dev }
                    Spacer(Modifier.height(10.dp))
                }
                if (devices.size > 1) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        onClick = { if (!busy) confirmAll = true },
                        shape = RoundedCornerShape(16.dp),
                        color = DCardBg,
                        border = BorderStroke(1.dp, DDanger.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = DDanger)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Удалить все устройства", color = DDanger, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text("Все потеряют доступ, подключишься заново", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Лимит устройств — по тарифу. Удалил лишнее — освободил место для нового. " +
                "Если удалил это устройство — обнови подписку, чтобы подключиться снова.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    confirmOne?.let { dev ->
        AlertDialog(
            onDismissRequest = { confirmOne = null },
            title = { Text("Удалить устройство?") },
            text = { Text("«${dev.name}» потеряет доступ. Чтобы подключить его снова — обнови подписку на нём.") },
            confirmButton = {
                TextButton(onClick = { confirmOne = null; removeOne(dev) }) { Text("Удалить", color = DDanger) }
            },
            dismissButton = { TextButton(onClick = { confirmOne = null }) { Text("Отмена") } },
        )
    }
    if (confirmAll) {
        AlertDialog(
            onDismissRequest = { confirmAll = false },
            title = { Text("Удалить все устройства?") },
            text = { Text("Все ${devices.size} устройств потеряют доступ. Чтобы подключиться снова — обнови подписку.") },
            confirmButton = {
                TextButton(onClick = { confirmAll = false; removeAll() }) { Text("Удалить все", color = DDanger) }
            },
            dismissButton = { TextButton(onClick = { confirmAll = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun DeviceCard(dev: AccountApi.Device, enabled: Boolean, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DCardBg,
        border = BorderStroke(1.dp, DCardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📱", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(dev.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                val sub = buildString {
                    if (dev.os.isNotBlank()) append(dev.os)
                    val seen = formatSeen(dev.lastSeen)
                    if (seen.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append("был ").append(seen)
                    }
                }
                if (sub.isNotBlank()) {
                    Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            IconButton(onClick = onDelete, enabled = enabled, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = DDanger)
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DCardBg,
        border = BorderStroke(1.dp, DCardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

/** "2026-09-04 12:34:56" -> "04.09 12:34" (сервер отдаёт UTC-строку без зоны; показываем как есть, без сдвига). */
private fun formatSeen(s: String?): String {
    if (s.isNullOrBlank()) return ""
    return try {
        val datePart = s.substring(0, 10)
        val timePart = if (s.length >= 16) s.substring(11, 16) else ""
        val (y, m, d) = datePart.split("-")
        if (y.length == 4) "$d.$m${if (timePart.isNotBlank()) " $timePart" else ""}" else s
    } catch (_: Exception) {
        s
    }
}
