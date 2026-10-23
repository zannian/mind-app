package com.lyulf.mind.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.seenStore by preferencesDataStore("mind_seen_reminders")

/** 已见过的提醒 id（有序，新的在末尾）；后台只对未出现过的 id 弹通知 */
class SeenReminderStore(private val context: Context) {
    private val idsKey = stringPreferencesKey("seen_ids_csv")
    private val primedKey = booleanPreferencesKey("primed")

    suspend fun isPrimed(): Boolean =
        context.seenStore.data.map { it[primedKey] == true }.first()

    suspend fun knownIds(): Set<String> =
        context.seenStore.data.map { parse(it[idsKey]) }.first().toSet()

    suspend fun markSeen(ids: Collection<String>, prime: Boolean = true) {
        if (ids.isEmpty() && !prime) return
        context.seenStore.edit { p ->
            val cur = parse(p[idsKey]).toMutableList()
            for (id in ids) {
                if (id.isBlank()) continue
                cur.remove(id)
                cur.add(id)
            }
            while (cur.size > 200) cur.removeAt(0)
            p[idsKey] = cur.joinToString(",")
            if (prime) p[primedKey] = true
        }
    }

    suspend fun clear() {
        context.seenStore.edit { it.clear() }
    }

    private fun parse(raw: String?): List<String> =
        raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
}
