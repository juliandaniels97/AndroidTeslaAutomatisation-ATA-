package org.tfv.ata

import android.app.Application
import org.tfv.ata.shortcut.ShortcutSync

class AtaApp : Application() {
  override fun onCreate() {
    super.onCreate()
    ShortcutSync.refresh(this)
  }
}
