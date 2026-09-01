package io.nekohasekai.sagernet.ui.compose.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val Gold = Color(0xFFD9B95C)
private val FCardBg = Color(0xFF15130F)
private val FCardBorder = Color(0x33D9B95C)
private const val URL_LK = "https://lk.supernet-tech.ru"
private const val FREE_MONTH_AT = 5   // оплативших друзей до «+1 месяц»

private data class Tier(val n: String, val label: String, val reward: String)

private val TIERS = listOf(
    Tier("3", "3 друга", "+2 недели"),
    Tier("5", "5 друзей", "+1 месяц"),
    Tier("10", "10 друзей", "+2 месяца"),
    Tier("25", "25 друзей", "+3 месяца"),
)

@Composable
fun FriendsScreen() {
    val context = LocalContext.current
    var acct by remember { mutableStateOf<AccountApi.Stats?>(null) }

    LaunchedEffect(Unit) {
        val t = AccountApi.getToken(context)
        if (!t.isNullOrBlank()) {
            val s = withContext(Dispatchers.IO) { AccountApi.fetch(t) }
            if (s != null) acct = s
        }
    }

    fun openLk() {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(URL_LK)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }

    fun share(text: String) {
        try {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(
                Intent.createChooser(i, "Поделиться").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
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
            "Друзья",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            fontFamily = FontFamily.Serif,
        )

        Spacer(Modifier.height(18.dp))

        val a = acct

        // ── Прогресс до бесплатного месяца (живой) ──
        if (a != null) {
            val paid = a.friendsPaid
            val frac = (paid.toFloat() / FREE_MONTH_AT).coerceIn(0f, 1f)
            val left = (FREE_MONTH_AT - paid).coerceAtLeast(0)
            Surface(
                shape = RoundedCornerShape(20.dp), color = FCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, FCardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🎁", fontSize = 30.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (left == 0) "Месяц бесплатно — открыт!" else "Следующий месяц — бесплатно",
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                        fontSize = 19.sp, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (left == 0) "Ты добил планку" else "Приведи ещё $left ${friendWord(left)} с подпиской",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth().height(10.dp)
                            .clip(RoundedCornerShape(5.dp)).background(Color(0x33FFFFFF)),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(frac).height(10.dp)
                                .clip(RoundedCornerShape(5.dp)).background(Gold),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("приглашено ${a.friendsInvited} · оплатило $paid",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Text("нужно $FREE_MONTH_AT",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Ссылка + Поделиться ──
            val link = a.refLink
            if (!link.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(18.dp), color = FCardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, FCardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Твоя ссылка для друзей", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(link, color = Gold, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            onClick = { share("Пользуйся SuperNet — быстрый доступ без ограничений. Заходи по моей ссылке: $link") },
                            shape = RoundedCornerShape(14.dp), color = Gold, modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 13.dp),
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Поделиться ссылкой", color = Color(0xFF0A0908), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Баланс ──
            Surface(
                onClick = { openLk() },
                shape = RoundedCornerShape(18.dp), color = FCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, FCardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💰  ", fontSize = 20.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Твой баланс (можно на подписку)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text("${a.balance} ₽", color = Gold, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                    Text("›", color = Gold, fontSize = 22.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
        } else {
            // Нет токена/данных — статичный крючок
            Surface(
                shape = RoundedCornerShape(20.dp), color = FCardBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, FCardBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎁", fontSize = 34.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Приводи друзей — плати меньше",
                        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold,
                        fontSize = 20.sp, fontFamily = FontFamily.Serif, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "За каждого друга с подпиской — бонусные дни и 30% с его оплат на баланс. Накопишь на месяц — и он бесплатный.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Лестница наград ──
        Surface(
            shape = RoundedCornerShape(20.dp), color = FCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, FCardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                TIERS.forEachIndexed { i, t ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(Color(0x22D9B95C)).border(1.dp, Gold, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(t.n, color = Gold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.size(14.dp))
                        Text(t.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(t.reward, color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    if (i < TIERS.size - 1) {
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Color(0x14FFFFFF)))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Полный кабинет (вывод/детали) ──
        Surface(
            onClick = { openLk() },
            shape = RoundedCornerShape(16.dp), color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, FCardBorder),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Все детали и вывод — в кабинете →", color = Gold, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            }
        }
    }
}

private fun friendWord(n: Int): String {
    val n10 = n % 10; val n100 = n % 100
    return when {
        n10 == 1 && n100 != 11 -> "друга"
        n10 in 2..4 && n100 !in 12..14 -> "друга"
        else -> "друзей"
    }
}
