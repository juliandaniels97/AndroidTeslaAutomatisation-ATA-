package org.tfv.ata

import android.app.Application
import org.tfv.ata.shortcut.ShortcutSync

class AtaApp : Application() {
  override fun onCreate() {
    super.onCreate()
    org.tfv.ata.automation.Notifier.ensureChannels(this)
    ShortcutSync.refresh(this)
    org.tfv.ata.nfc.KeyCardRegistration.start(this)
  }
}
