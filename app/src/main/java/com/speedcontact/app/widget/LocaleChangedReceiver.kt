package com.speedcontact.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.speedcontact.app.app
import kotlinx.coroutines.launch

/**
 * The widgets' text comes from this app, so it stays in the old language until they are redrawn.
 * Android sends this for the phone's language and for a language chosen for this app alone.
 */
class LocaleChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_LOCALE_CHANGED) return
        val pending = goAsync()
        context.app.appScope.launch {
            try {
                context.app.widgetUpdater.updateAll()
            } finally {
                pending.finish()
            }
        }
    }
}
