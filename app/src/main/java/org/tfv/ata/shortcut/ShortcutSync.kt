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

object ShortcutSync {
  fun refresh(context: Context) {
    val store = AtaStore.get(context)
    CoroutineScope(Dispatchers.Default).launch {
      val settings = store.settings.first()
      val token = store.triggerToken()
      val actions = settings.launcherMenuActionIds.mapNotNull { AtaAction.fromId(it) }.take(4)
      val shortcuts = actions.map { build(context, "dyn_${it.id}", it, token) }
      runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }
  }

  fun pinToHome(context: Context, action: AtaAction) {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return
    val store = AtaStore.get(context)
    CoroutineScope(Dispatchers.Default).launch {
      val token = store.triggerToken()
      val info = build(context, "pin_${action.id}_${System.currentTimeMillis()}", action, token)
      runCatching { ShortcutManagerCompat.requestPinShortcut(context, info, null) }
    }
  }

  private fun build(context: Context, id: String, action: AtaAction, token: String): ShortcutInfoCompat {
    val label = context.getString(ActionInfo.labelRes(action))
    val intent = Intent(context, RunActionActivity::class.java).apply {
      this.action = IntentContract.ACTION_RUN
      putExtra(IntentContract.EXTRA_ACTION_ID, action.id)
      putExtra(IntentContract.EXTRA_TOKEN, token)
    }
    return ShortcutInfoCompat.Builder(context, id)
      .setShortLabel(label)
      .setLongLabel(label)
      .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
      .setIntent(intent)
      .build()
  }
}
