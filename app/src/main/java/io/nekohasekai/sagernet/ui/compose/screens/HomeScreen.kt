package io.nekohasekai.sagernet.ui.compose.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sagernet.bg.BaseService
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.SagerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Бренд ──
private val Gold = Color(0xFFD9B95C)
private val GoldDim = Color(0xFF8A6E2A)
private val GreenOk = Color(0xFF6FCF97)
private val CardBg = Color(0xFF15130F)
private val CardBorder = Color(0x33D9B95C)

// ── Ссылки (боевые) ──
private const val URL_LK = "https://lk.supernet-tech.ru"
private const val URL_FAQ = "https://lk.supernet-tech.ru/?open=faq"
private const val TG_CHANNEL = "supernet_vpn_access"     // канал
private const val TG_SUPPORT = "SuperNetConnect_bot"     // бот-поддержка

private data class HomeData(
    val location: String = "",
    val ping: Int = 0,
    val pingOk: Boolean = false,
    val days: Int = -1,
    val expireLabel: String = "",
    val usedGb: Double = -1.0,
    val totalGb: Double = -1.0,
)

@Composable
fun HomeScreen(
    serviceState: BaseService.State,
    onServiceToggle: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var data by remember { mutableStateOf(HomeData()) }
    var acct by remember { mutableStateOf<AccountApi.Stats?>(null) }

    // Живые цифры из ЛК по app-токену (белый лимит, дни, друзья, баланс) — если токен есть
    LaunchedEffect(serviceState) {
        val t = AccountApi.getToken(context)
        if (!t.isNullOrBlank()) {
            val s = withContext(Dispatchers.IO) { AccountApi.fetch(t) }
            if (s != null) acct = s
        }
    }

    LaunchedEffect(serviceState) {
        val loaded = withContext(Dispatchers.IO) {
            try {
                val proxy = SagerDatabase.proxyDao.getById(DataStore.selectedProxy)
                val locName = proxy?.displayName() ?: ""
                val p = proxy?.ping ?: 0
                val ok = (proxy?.status ?: 0) == 1

                val group = SagerDatabase.groupDao.getById(DataStore.currentGroupId())
                val sub = group?.subscription
                var days = -1
                var expLabel = ""
                val expRaw = sub?.expiryDate ?: 0L
                if (expRaw > 0L) {
                    val expMs = if (expRaw < 100000000000L) expRaw * 1000L else expRaw
                    val now = System.currentTimeMillis()
                    val d = ((expMs - now) / 86400000L).toInt()
                    days = if (d < 0) 0 else d
                    expLabel = formatDate(expMs)
                }
                var usedGb = -1.0
                var totalGb = -1.0
                val used = sub?.bytesUsed ?: 0L
                val remain = sub?.bytesRemaining ?: 0L
                if (used > 0L || remain > 0L) {
                    val total = used + remain
                    usedGb = used / 1073741824.0
                    totalGb = total / 1073741824.0
                }
                HomeData(locName, p, ok, days, expLabel, usedGb, totalGb)
            } catch (_: Exception) {
                HomeData()
            }
        }
        data = loaded
    }

    val connected = serviceState == BaseService.State.Connected
    val connecting = serviceState == BaseService.State.Connecting || serviceState == BaseService.State.Stopping

    val statusText = when {
        connected -> "Подключено"
        connecting -> "Подключение…"
        else -> "Отключено"
    }
    val statusColor = when {
        connected -> GreenOk
        connecting -> Gold
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusSub = when {
        connected -> (if (data.location.isNotEmpty()) data.location else "SuperNet") + " · защищено"
        connecting -> "устанавливаем соединение"
        else -> "нажмите, чтобы подключиться"
    }

    fun openUrl(u: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    // Открываем САМО приложение Telegram (tg://) — минуя Chrome и блокировку t.me.
    // Если Telegram не установлен — падаем на https-ссылку.
    fun openTelegram(domain: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$domain"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            openUrl("https://t.me/$domain")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 20.dp, bottom = 120.dp),
    ) {
        // ── Шапка ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, Gold, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("SN", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Serif)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "SuperNet",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    fontFamily = FontFamily.Serif,
                )
                Text(
                    "премиум-доступ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            Surface(
                onClick = onOpenSettings,
                shape = CircleShape,
                color = CardBg,
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = Gold, modifier = Modifier.size(22.dp))
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Большая кнопка ──
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0E0C09))
                    .border(if (connected) 4.dp else 2.dp, if (connected) Gold else GoldDim, CircleShape)
                    .clickable(enabled = !connecting) { onServiceToggle() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (connected) Icons.Filled.Bolt else Icons.Filled.PowerSettingsNew,
                    contentDescription = statusText,
                    tint = Gold,
                    modifier = Modifier.size(84.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            statusText,
            color = statusColor,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            statusSub,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        // ── Текущая локация ──
        Surface(
            onClick = onOpenLocations,
            shape = RoundedCornerShape(18.dp),
            color = CardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (data.location.isNotEmpty()) data.location else "Выбрать локацию",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                    )
                    Text(
                        if (data.pingOk && data.ping > 0) "локации · ${data.ping} ms" else "нажмите, чтобы выбрать",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // ── Цифры: дни + белый лимит (живые из ЛК по токену, fallback на подписку) ──
        run {
            val a = acct
            val daysShow: Int? = a?.days ?: (if (data.days >= 0) data.days else null)
            val whiteOk = a != null && a.hasWhiteLimit && a.whiteLimitGb > 0
            if (daysShow != null || whiteOk || data.totalGb > 0) {
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (daysShow != null) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "ДОСТУП",
                            big = daysShow.toString(),
                            unit = "дней",
                            sub = if (data.expireLabel.isNotEmpty()) "до ${data.expireLabel}" else "",
                        )
                    }
                    if (whiteOk && a != null) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "БЕЛЫЙ ЛИМИТ",
                            big = fmtGb(a.whiteUsedGb),
                            unit = "из ${fmtGb(a.whiteLimitGb)} ГБ",
                            sub = "",
                            progress = (a.whitePercent / 100.0).toFloat().coerceIn(0f, 1f),
                        )
                    } else if (data.totalGb > 0) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "ТРАФИК",
                            big = fmtGb(data.usedGb),
                            unit = "из ${fmtGb(data.totalGb)} ГБ",
                            sub = "",
                            progress = (data.usedGb / data.totalGb).toFloat().coerceIn(0f, 1f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Быстрые кнопки ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickButton("👤", "Кабинет", Modifier.weight(1f)) { openUrl(URL_LK) }
            QuickButton("✈️", "Telegram", Modifier.weight(1f)) { openTelegram(TG_CHANNEL) }
            QuickButton("❓", "Вопросы", Modifier.weight(1f)) { openUrl(URL_FAQ) }
            QuickButton("💬", "Поддержка", Modifier.weight(1f)) { openTelegram(TG_SUPPORT) }
        }

        Spacer(Modifier.height(14.dp))

        // ── Запасной канал ──
        Surface(
            onClick = { Toast.makeText(context, "Запасной канал (мессенджеры) — скоро", Toast.LENGTH_SHORT).show() },
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🆘  ", fontSize = 15.sp)
                Text(
                    "Запасной канал (мессенджеры)",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    big: String,
    unit: String,
    sub: String,
    progress: Float = -1f,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(big, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                Spacer(Modifier.size(6.dp))
                Text(unit, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
            }
            if (progress >= 0f) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x33FFFFFF)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Gold),
                    )
                }
            } else if (sub.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QuickButton(emoji: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
        }
    }
}

private fun fmtGb(v: Double): String {
    if (v < 0) return "0"
    return if (v >= 10) v.toInt().toString() else String.format("%.1f", v)
}

private fun formatDate(ms: Long): String {
    return try {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = ms
        val months = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val m = months[cal.get(java.util.Calendar.MONTH)]
        "$day $m"
    } catch (_: Exception) {
        ""
    }
}
