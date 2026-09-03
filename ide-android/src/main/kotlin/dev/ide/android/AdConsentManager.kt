package dev.ide.android

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Advertising-consent manager.
 *
 * Advertising was removed from the app, so there is no consent flow to run: no consent SDK is linked and no
 * consent is ever gathered. The type is kept as an empty no-op so existing call sites compile unchanged.
 * [canRequestAds] is always `false`, [privacyOptionsRequired] is always `false`, and [gather]/
 * [showPrivacyOptions] do nothing (but still invoke [onResolved] so callers always proceed).
 */
class AdConsentManager(context: Context) {
    /** Always `false` — no ad network is available to request ads from. */
    var privacyOptionsRequired by mutableStateOf(false)
        private set

    /** Whether the (removed) ads SDK may request ads. Always `false`. */
    val canRequestAds: Boolean get() = false

    /** No-op. [onResolved] is invoked immediately so the caller always proceeds. */
    fun gather(activity: Activity, onResolved: () -> Unit) {
        onResolved()
    }

    /** No-op — there is no privacy-options form. */
    fun showPrivacyOptions(activity: Activity) = Unit
}