package org.tfv.ata.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricGate {
  private const val AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
      BiometricManager.Authenticators.DEVICE_CREDENTIAL

  fun available(activity: FragmentActivity): Boolean =
    BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) ==
      BiometricManager.BIOMETRIC_SUCCESS

  fun authenticate(activity: FragmentActivity, title: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
    if (!available(activity)) {
      onSuccess()
      return
    }
    val prompt = BiometricPrompt(
      activity,
      ContextCompat.getMainExecutor(activity),
      object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFailure()
      },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setAllowedAuthenticators(AUTHENTICATORS)
      .build()
    prompt.authenticate(info)
  }
}
