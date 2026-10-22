package com.fourthwardai.orbit.ui.util

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import timber.log.Timber

fun openInCustomTab(context: Context, url: String, toolbarColor: Int) {
    val colorSchemeParams = CustomTabColorSchemeParams.Builder()
        .setToolbarColor(toolbarColor)
        .build()

    val customTabsIntent = CustomTabsIntent.Builder()
        .setDefaultColorSchemeParams(colorSchemeParams)
        .setShowTitle(true)
        .setShareState(CustomTabsIntent.SHARE_STATE_ON)
        .build()

    // Pinning the intent to a browser stops Android from routing verified app links (e.g. medium.com) to their native apps.
    CustomTabsClient.getPackageName(context, emptyList())?.let { browserPackage ->
        customTabsIntent.intent.setPackage(browserPackage)
    }

    try {
        customTabsIntent.launchUrl(context, url.toUri())
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No app available to open $url")
    }
}
