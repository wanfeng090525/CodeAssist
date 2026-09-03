package dev.ide.android

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ide.ui.backend.AdHost
import dev.ide.ui.backend.AdPlacement

/**
 * Android advertising bridge for the shared UI (see [AdHost]).
 *
 * Advertising was removed from the app: no ad SDK is linked. This is a no-ad host — [available] is `false`,
 * so every ad slot in the shared UI is gated off and [NativeAd] renders nothing. The constructor and shape are
 * kept for source compatibility with existing call sites; the consent hooks are inert.
 */
class AndroidAdHost(
    private val openUrl: (String) -> Unit,
    /** Kept for signature compatibility; inert since ads are removed. */
    private val privacyOptionsRequiredProvider: () -> Boolean = { false },
    /** Kept for signature compatibility; inert since ads are removed. */
    private val onShowPrivacyOptions: () -> Unit = {},
    /** Kept for signature compatibility; inert since ads are removed. */
    private val activityProvider: () -> Activity? = { null },
    /** Identity of the installed build — see [AdHost.installStamp]. */
    override val installStamp: String? = null,
) : AdHost {
    override val available: Boolean = false

    @Composable
    override fun NativeAd(placement: AdPlacement, modifier: Modifier) {
        // No-op: ads are removed, so nothing is rendered (the surrounding AdSlot collapses to no card).
    }
}