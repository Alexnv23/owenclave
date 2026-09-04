package io.nekohasekai.sagernet.ui.compose.screens

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Клиент «кабинета в приложении»: тянет живые цифры юзера с ЛК по app-токену.
 * Эндпоинты:
 *   GET  https://lk.supernet-tech.ru/api/app/stats/{token}                    — статистика + устройства (READ-ONLY)
 *   POST https://lk.supernet-tech.ru/api/app/devices/{token}/{id}/revoke       — удалить устройство
 * Токен приложение получает из deeplink (supernet://subscription?...&token=XXX) и хранит в SharedPreferences.
 */
object AccountApi {
    private const val BASE = "https://lk.supernet-tech.ru"
    private const val PREFS = "supernet_account"
    private const val KEY_TOKEN = "account_token"

    fun saveToken(ctx: Context, token: String) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_TOKEN, token).apply()
        } catch (_: Exception) {
        }
    }

    fun getToken(ctx: Context): String? = try {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)
    } catch (_: Exception) {
        null
    }

    /** Забыть токен кабинета (при удалении подписки). */
    fun clearToken(ctx: Context) {
        try {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_TOKEN).apply()
        } catch (_: Exception) {
        }
    }

    /** Устройство из учёта ЛК (marzban_devices, сгруппировано по hwid на сервере). */
    data class Device(
        val id: Long,
        val name: String,
        val os: String,
        val lastSeen: String?,
    )

    data class Stats(
        val displayName: String?,
        val days: Int?,
        val whiteUsedGb: Double,
        val whiteLimitGb: Double,
        val whitePercent: Double,
        val whiteBlocked: Boolean,
        val hasWhiteLimit: Boolean,
        val friendsInvited: Int,
        val friendsPaid: Int,
        val balance: Int,
        val refLink: String?,
        val devicesCount: Int,
        val devicesLimit: Int,
        val devices: List<Device> = emptyList(),
    )

    /** Синхронный сетевой вызов — дёргать ТОЛЬКО с Dispatchers.IO. Возвращает null при любой ошибке. */
    fun fetch(token: String): Stats? {
        if (token.isBlank()) return null
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL("$BASE/api/app/stats/$token").openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
            }
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val j = JSONObject(body)
            val devs = ArrayList<Device>()
            try {
                val arr = j.optJSONArray("devices")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val d = arr.optJSONObject(i) ?: continue
                        devs.add(
                            Device(
                                id = d.optLong("id", 0L),
                                name = d.optString("name", "Устройство").ifBlank { "Устройство" },
                                os = d.optString("os", ""),
                                lastSeen = if (d.isNull("last_seen")) null else d.optString("last_seen").takeIf { it.isNotBlank() },
                            )
                        )
                    }
                }
            } catch (_: Exception) {
            }
            Stats(
                displayName = if (j.isNull("display_name")) null else j.optString("display_name").takeIf { it.isNotBlank() },
                days = if (j.isNull("days_remaining")) null else j.optInt("days_remaining"),
                whiteUsedGb = j.optDouble("white_used_gb", 0.0),
                whiteLimitGb = j.optDouble("white_limit_gb", 0.0),
                whitePercent = j.optDouble("white_percent", 0.0),
                whiteBlocked = j.optBoolean("white_blocked", false),
                hasWhiteLimit = j.optBoolean("has_white_limit", false),
                friendsInvited = j.optInt("friends_invited", 0),
                friendsPaid = j.optInt("friends_paid", 0),
                balance = j.optInt("balance", 0),
                refLink = if (j.isNull("ref_link")) null else j.optString("ref_link"),
                devicesCount = j.optInt("devices_count", devs.size),
                devicesLimit = j.optInt("devices_limit", 2),
                devices = devs,
            )
        } catch (_: Exception) {
            null
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Удалить устройство. Синхронно — ТОЛЬКО с Dispatchers.IO.
     * Возвращает Pair(успех, сообщение сервера/ошибки).
     */
    fun revokeDevice(token: String, deviceId: Long): Pair<Boolean, String> {
        if (token.isBlank() || deviceId <= 0L) return false to "Нет токена или устройства"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL("$BASE/api/app/devices/$token/$deviceId/revoke").openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Length", "0")
            }
            conn.outputStream.use { }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = try { stream?.bufferedReader()?.use { it.readText() } ?: "" } catch (_: Exception) { "" }
            val msg = try { JSONObject(body).let { it.optString("message", it.optString("detail", "")) } } catch (_: Exception) { "" }
            if (code in 200..299) true to (msg.ifBlank { "Устройство удалено" })
            else false to (msg.ifBlank { "Ошибка сервера ($code)" })
        } catch (e: Exception) {
            false to ("Ошибка сети: " + (e.message ?: e.javaClass.simpleName))
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {
            }
        }
    }
}
