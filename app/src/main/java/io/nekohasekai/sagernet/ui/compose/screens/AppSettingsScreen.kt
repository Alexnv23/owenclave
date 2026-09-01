package io.nekohasekai.sagernet.ui.compose.screens

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nekohasekai.sagernet.database.DataStore

private val SGold = Color(0xFFD9B95C)
private val SCardBg = Color(0xFF15130F)
private val SCardBorder = Color(0x33D9B95C)

@Composable
fun AppSettingsScreen() {
    val context = LocalContext.current
    var proxyApps by remember { mutableStateOf(DataStore.proxyApps) }
    var bypassLan by remember { mutableStateOf(DataStore.bypassLan) }
    var routeMode by remember { mutableIntStateOf(DataStore.routeMode) }

    val version = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 24.dp, bottom = 120.dp),
    ) {
        Text(
            "Настройки",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            fontFamily = FontFamily.Serif,
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel("ПОДКЛЮЧЕНИЕ")

        // Обход приложений (per-app)
        SettingRow(
            title = "Обход приложений",
            subtitle = "Выбрать, какие приложения идут через сервис",
            onClick = {
                DataStore.proxyApps = true
                proxyApps = true
                try {
                    context.startActivity(
                        Intent(context, io.nekohasekai.sagernet.ui.compose.ComposeAppListActivity::class.java)
                    )
                } catch (_: Exception) {
                }
            },
        )

        Spacer(Modifier.height(10.dp))

        // Локальная сеть напрямую
        SwitchRow(
            title = "Локальная сеть напрямую",
            subtitle = "Роутер, принтеры и локальные адреса — мимо сервиса",
            checked = bypassLan,
            onCheckedChange = { bypassLan = it; DataStore.bypassLan = it },
        )

        Spacer(Modifier.height(18.dp))
        SectionLabel("МАРШРУТИЗАЦИЯ")

        // Режим маршрутизации: сегменты
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, SCardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Как пускать трафик",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Segment("По правилам", routeMode == 0, Modifier.weight(1f)) {
                        DataStore.routeMode = 0; routeMode = 0
                    }
                    Segment("Всё через сервис", routeMode == 1, Modifier.weight(1f)) {
                        DataStore.routeMode = 1; routeMode = 1
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "«По правилам» — рекомендуется: РФ-сайты напрямую, остальное через SuperNet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionLabel("О ПРИЛОЖЕНИИ")

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, SCardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SuperNet", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text("премиум-доступ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                if (version.isNotEmpty()) {
                    Text("v$version", color = SGold, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = SGold,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = SCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, SCardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, SCardBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Spacer(Modifier.height(0.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF0A0908),
                    checkedTrackColor = SGold,
                ),
            )
        }
    }
}

@Composable
private fun Segment(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SGold else Color.Transparent)
            .border(1.dp, if (selected) SGold else SCardBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color(0xFF0A0908) else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
