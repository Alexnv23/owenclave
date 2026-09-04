package io.nekohasekai.sagernet.ui.compose.screens

import android.content.Context
import android.widget.Toast
import io.nekohasekai.sagernet.GroupType
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.GroupManager
import io.nekohasekai.sagernet.database.ProxyGroup
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.group.GroupUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Действия с подпиской SuperNet, общие для экранов «Локации» и «Настройки».
 *
 * refresh(): раньше кнопка обновляла «текущую» группу и глотала ошибки — если выбрана не та группа
 * или прошлое обновление зависло (группа осталась в GroupUpdater.updating), нажатие молча ничего не делало.
 * Теперь: ищем подписочную группу явно, сбрасываем зависший лок, показываем результат.
 *
 * deleteAll(): сносит подписочные группы с их локациями, останавливает подключение, забывает токен кабинета.
 */
object SubscriptionActions {

    private fun toast(ctx: Context, msg: String, long: Boolean = false) {
        Toast.makeText(ctx, msg, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    /** Все подписочные группы с непустой ссылкой (обычно одна). Вызывать с IO. */
    fun subscriptionGroups(): List<ProxyGroup> = try {
        SagerDatabase.groupDao.allGroups().filter {
            it.type == GroupType.SUBSCRIPTION && it.subscription?.link?.isNotEmpty() == true
        }
    } catch (_: Exception) {
        emptyList()
    }

    /** Есть ли подписка вообще (для показа/скрытия кнопок). Вызывать с IO. */
    fun hasSubscription(): Boolean = subscriptionGroups().isNotEmpty()

    /**
     * Обновить подписку. Вызывать с Dispatchers.IO.
     * Возвращает true, если обновление прошло (детали пользователь видит в тостах).
     */
    suspend fun refresh(ctx: Context): Boolean {
        val groups = subscriptionGroups()
        if (groups.isEmpty()) {
            withContext(Dispatchers.Main) { toast(ctx, "Подписка не добавлена. Добавь её из личного кабинета.") }
            return false
        }
        // Предпочитаем текущую группу, если она подписочная; иначе первую подписочную.
        val current = DataStore.selectedGroup
        val group = groups.firstOrNull { it.id == current } ?: groups.first()

        // Сброс зависшего лока: если прошлое обновление не завершилось, executeUpdate молча отменяется.
        GroupUpdater.updating.remove(group.id)
        GroupUpdater.progress.remove(group.id)

        withContext(Dispatchers.Main) { toast(ctx, "Обновляю подписку…") }
        val before = try { SagerDatabase.proxyDao.countByGroup(group.id) } catch (_: Exception) { -1L }
        val ok = try {
            GroupUpdater.executeUpdate(group, true)
        } catch (e: Throwable) {
            // CancellationException и прочее — покажем причину, а не проглотим.
            withContext(Dispatchers.Main) {
                toast(ctx, "Не удалось обновить: ${e.message ?: e.javaClass.simpleName}", long = true)
            }
            GroupUpdater.updating.remove(group.id)
            return false
        }
        if (ok) {
            val after = try { SagerDatabase.proxyDao.countByGroup(group.id) } catch (_: Exception) { -1L }
            if (DataStore.selectedGroup != group.id) DataStore.selectedGroup = group.id
            withContext(Dispatchers.Main) {
                val n = if (after >= 0) after else before
                toast(ctx, if (n > 0) "Подписка обновлена: $n локаций" else "Подписка обновлена")
            }
        }
        return ok
    }

    /**
     * Удалить подписку целиком. Вызывать с Dispatchers.IO.
     * Останавливает подключение, удаляет подписочные группы (с локациями), забывает токен кабинета.
     */
    suspend fun deleteAll(ctx: Context): Boolean {
        val groups = subscriptionGroups()
        if (groups.isEmpty()) {
            withContext(Dispatchers.Main) { toast(ctx, "Подписки нет — удалять нечего") }
            return false
        }
        try {
            if (SagerNet.started) SagerNet.stopService()
        } catch (_: Exception) {
        }
        var removed = 0
        for (g in groups) {
            try {
                GroupManager.deleteGroup(g.id)
                removed++
            } catch (_: Exception) {
            }
        }
        try {
            DataStore.selectedProxy = 0L
            DataStore.selectedGroup = 0L
            DataStore.currentGroupId() // создаст/выберет группу по умолчанию, чтобы экраны не падали
        } catch (_: Exception) {
        }
        AccountApi.clearToken(ctx)
        withContext(Dispatchers.Main) {
            toast(ctx, if (removed > 0) "Подписка удалена. Добавь новую из личного кабинета." else "Не удалось удалить подписку", long = true)
        }
        return removed > 0
    }
}
