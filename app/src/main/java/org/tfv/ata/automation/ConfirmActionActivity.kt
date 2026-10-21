package org.tfv.ata.automation

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.tfv.ata.R
import org.tfv.ata.action.ActionInfo
import org.tfv.ata.core.TriggerSource
import org.tfv.ata.data.AtaStore
import org.tfv.ata.ui.BiometricGate

/**
 * Confirmation for a critical command that an automation asked for (opened from ATA's notification only:
 * not exported). Opening it from the lock screen already requires unlocking the phone; with "Biometric
 * lock" on, ATA additionally asks for fingerprint / face / device credential.
 */
class ConfirmActionActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val request = ActionRequest.from(intent)
    val action = request?.action
    if (request == null || action == null) {
      finish()
      return
    }
    Notifier.cancelConfirmation(this, action)
    val label = getString(ActionInfo.labelRes(action))

    AlertDialog.Builder(this)
      .setTitle(getString(R.string.confirm_title, label))
      .setMessage(R.string.confirm_critical_message)
      .setPositiveButton(R.string.confirm_action) { _, _ -> authenticateAndRun(request, label) }
      .setNegativeButton(R.string.cancel) { _, _ -> finish() }
      .setOnCancelListener { finish() }
      .show()
  }

  private fun authenticateAndRun(request: ActionRequest, label: String) {
    lifecycleScope.launch {
      val settings = AtaStore.get(this@ConfirmActionActivity).settings.first()
      val run = {
        ActionLauncher.runNow(this@ConfirmActionActivity, request.copy(source = TriggerSource.CONFIRMED))
        finish()
      }
      if (settings.biometricLock) {
        BiometricGate.authenticate(this@ConfirmActionActivity, label, onSuccess = run, onFailure = { finish() })
      } else {
        run()
      }
    }
  }
}
