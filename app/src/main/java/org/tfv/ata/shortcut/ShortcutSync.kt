package org.tfv.ata.shortcut

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.automation.RunActionActivity
import org.tfv.ata.core.AtaAction
import org.tfv.ata.core.IntentContract
import org.tfv.ata.data.AtaStore

/**
 * Launcher shortcuts. They carry ATA's token, which is what makes Samsung Routines (which starts these
 * shortcuts) a trusted trigger. After the token is renewed, [refresh] re-issues dynamic and pinned shortcuts.
 */
object ShortcutSync {
  private const val PIN_PREFIX = "pin_"

  fun refresh(context: Context) {
    val app = context.applicationContext
    val store = AtaStore.get(app)
    CoroutineScope(Dispatchers.Default).launch {
      val settings = store.settings.first()
      val token = store.triggerToken()
      // Icon-menu actions first (rank 0–3: the launcher shows these), then the extra routine actions.
      val ids = publishedActionIds(app, settings.launcherMenuActionIds, settings.routineActionIds)
      val dynamic = ids.mapIndexed { rank, id -> build(app, "dyn_$id", id, null, token, rank) }
      runCatching { ShortcutManagerCompat.setDynamicShortcuts(app, dynamic) }
      runCatching {
        val pinned = ShortcutManagerCompat.getShortcuts(app, ShortcutManagerCompat.FLAG_MATCH_PINNED)
          .filter { it.id.startsWith(PIN_PREFIX) }
          .mapNotNull { info ->
            val intent = info.intent
            val actionId = intent.getStringExtra(IntentContract.EXTRA_ACTION_ID) ?: return@mapNotNull null
            build(app, info.id, actionId, intent.getStringExtra(IntentContract.EXTRA_VALUE), token)
          }
        if (pinned.isNotEmpty()) ShortcutManagerCompat.updateShortcuts(app, pinned)
      }
    }
  }

  /** Max number of app shortcuts this launcher accepts (usually 15); Samsung Routines lists these. */
  fun maxPublished(context: Context): Int =
    runCatching { ShortcutManagerCompat.getMaxShortcutCountPerActivity(context) }.getOrDefault(5).coerceIn(4, 15)

  fun publishedActionIds(context: Context, menu: List<String>, routine: List<String>): List<String> {
    val valid = { id: String -> AtaAction.fromId(id)?.let { !it.needsValue } == true }
    val first = menu.filter(valid).distinct().take(4)
    val extra = routine.filter { valid(it) && it !in first }.distinct()
    return (first + extra).take(maxPublished(context))
  }

  fun pinToHome(context: Context, action: AtaAction, value: String? = null) = pinToHome(context, action.id, value)

  fun pinToHome(context: Context, actionId: String, value: String? = null) {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return
    val store = AtaStore.get(context)
    CoroutineScope(Dispatchers.Default).launch {
      val info = build(context, "$PIN_PREFIX${actionId}_${System.currentTimeMillis()}", actionId, value, store.triggerToken())
      runCatching { ShortcutManagerCompat.requestPinShortcut(context, info, null) }
    }
  }

  /** A shortcut for the CREATE_SHORTCUT flow (Tasker, launchers, possibly Samsung Routines). */
  suspend fun forCreateShortcut(context: Context, actionId: String, value: String?): ShortcutInfoCompat =
    build(context, "$PIN_PREFIX${actionId}_${System.currentTimeMillis()}", actionId, value, AtaStore.get(context).triggerToken())

  fun label(context: Context, actionId: String, value: String?): String {
    val base = when (actionId) {
      IntentContract.ACTION_ID_SET_ACTIVE -> context.getString(R.string.shortcut_set_active)
      IntentContract.ACTION_ID_SET_PASSIVE -> context.getString(R.string.shortcut_set_passive)
      else -> AtaAction.fromId(actionId)?.let { context.getString(ActionInfo.labelRes(it)) } ?: actionId
    }
    return if (value.isNullOrBlank()) base else "$base: $value"
  }

  private fun build(context: Context, id: String, actionId: String, value: String?, token: String, rank: Int = 0): ShortcutInfoCompat {
    val label = label(context, actionId, value)
    val intent = Intent(context, RunActionActivity::class.java).apply {
      action = IntentContract.ACTION_RUN
      putExtra(IntentContract.EXTRA_ACTION_ID, actionId)
      if (!value.isNullOrBlank()) putExtra(IntentContract.EXTRA_VALUE, value)
      putExtra(IntentContract.EXTRA_TOKEN, token)
    }
    return ShortcutInfoCompat.Builder(context, id)
      .setShortLabel(label.take(24))
      .setLongLabel(label)
      .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
      .setIntent(intent)
      .setRank(rank)
      .build()
  }
}
