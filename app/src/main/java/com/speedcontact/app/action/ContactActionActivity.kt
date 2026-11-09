package com.speedcontact.app.action

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.speedcontact.app.R
import com.speedcontact.app.app
import com.speedcontact.app.model.TapAction
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.ui.ContactOptionsDialog
import com.speedcontact.app.ui.SpeedContactTheme
import com.speedcontact.app.widget.WidgetPerson
import kotlinx.coroutines.launch

/**
 * Invisible step between a widget tap and the outcome. It exists so a first tap can still ask for
 * the call permission, so failures such as a missing WhatsApp show a message, and to tell one tap
 * from two: Android keeps long presses on the home screen for moving widgets, so a double tap is
 * what opens a person's other options there. Being see-through and full screen, this window is
 * what the second tap lands on.
 */
class ContactActionActivity : ComponentActivity() {
    private enum class Stage { WAITING_FOR_SECOND_TAP, SHOWING_MENU, ACTING }

    private val intents by lazy { ContactIntents(this) }
    private val handler = Handler(Looper.getMainLooper())
    private var number: String = ""
    private var lookupKey: String = ""
    private var contactId: Long = 0L
    private var name: String = ""
    private var quickAction: TapAction = TapAction.CALL
    private var menuActions: List<TapAction> = emptyList()
    private var stage = Stage.ACTING
    private var menuVisible by mutableStateOf(false)

    private val performQuickAction = Runnable { perform(quickAction) }

    private val callPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                placeCall()
            } else {
                toast(R.string.call_permission_denied)
                launch(intents.dial(number))
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val extras = intent.extras
        val action = extras?.getString(EXTRA_ACTION)?.toTapAction()
        number = extras?.getString(EXTRA_NUMBER).orEmpty()
        lookupKey = extras?.getString(EXTRA_LOOKUP_KEY).orEmpty()
        contactId = extras?.getLong(EXTRA_CONTACT_ID) ?: 0L
        name = extras?.getString(EXTRA_NAME).orEmpty()
        menuActions = extras?.getStringArray(EXTRA_MENU_ACTIONS).orEmpty().mapNotNull { it.toTapAction() }

        if (action == null || number.isBlank()) {
            finish()
            return
        }
        quickAction = action

        val savedStage = savedInstanceState?.getString(KEY_STAGE)
            ?.let { saved -> Stage.entries.firstOrNull { it.name == saved } }
        when (savedStage) {
            // The permission result is still on its way to callPermission.
            Stage.ACTING -> stage = Stage.ACTING
            Stage.SHOWING_MENU -> showMenu()
            Stage.WAITING_FOR_SECOND_TAP -> perform(action)
            null -> if (menuActions.isEmpty()) perform(action) else waitForSecondTap()
        }
    }

    /** The window may not be up yet for a very quick second tap, which then arrives here instead. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (stage == Stage.WAITING_FOR_SECOND_TAP) showMenu()
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (stage == Stage.WAITING_FOR_SECOND_TAP) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) showMenu()
            return true
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_STAGE, stage.name)
    }

    override fun onDestroy() {
        handler.removeCallbacks(performQuickAction)
        super.onDestroy()
    }

    private fun waitForSecondTap() {
        stage = Stage.WAITING_FOR_SECOND_TAP
        handler.postDelayed(performQuickAction, SECOND_TAP_WINDOW_MILLIS)
    }

    private fun showMenu() {
        handler.removeCallbacks(performQuickAction)
        stage = Stage.SHOWING_MENU
        menuVisible = true
        setContent {
            SpeedContactTheme {
                if (menuVisible) {
                    ContactOptionsDialog(
                        name = name,
                        actions = menuActions,
                        onChoose = ::perform,
                        onDismiss = ::finish,
                    )
                }
            }
        }
    }

    private fun perform(action: TapAction) {
        handler.removeCallbacks(performQuickAction)
        stage = Stage.ACTING
        menuVisible = false

        when (action) {
            TapAction.CALL -> {
                if (hasCallPermission()) {
                    placeCall()
                } else {
                    callPermission.launch(Manifest.permission.CALL_PHONE)
                    return
                }
            }

            TapAction.SMS -> launch(intents.sms(number))
            TapAction.CONTACT_CARD -> launch(intents.contactCard(contactId, lookupKey))
            TapAction.WHATSAPP_CHAT -> openWhatsAppChat()
            TapAction.WHATSAPP_CALL -> {
                val call = intents.whatsAppCall(contactId, number)
                if (call != null && launch(call)) {
                    recordCall()
                } else {
                    toast(R.string.whatsapp_call_unavailable)
                    openWhatsAppChat()
                }
            }
        }
        finish()
    }

    private fun hasCallPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    private fun placeCall() {
        if (launch(intents.call(number), reportFailure = false) || launch(intents.dial(number))) {
            recordCall()
        }
    }

    private fun openWhatsAppChat() {
        val chat = intents.whatsAppChat(number)
        if (chat == null) toast(R.string.whatsapp_not_installed) else launch(chat)
    }

    private fun recordCall() {
        val app = applicationContext.app
        val key = lookupKey
        app.appScope.launch {
            app.store.recordCall(key, System.currentTimeMillis())
            app.widgetUpdater.updateRecentlySorted()
        }
    }

    /**
     * Always a new task: an activity left inside this one would survive it, and the next widget
     * tap or a finished call would bring that old screen back.
     */
    private fun launch(intent: Intent, reportFailure: Boolean = true): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        if (reportFailure) toast(R.string.action_failed)
        false
    } catch (_: SecurityException) {
        if (reportFailure) toast(R.string.action_failed)
        false
    }

    private fun toast(@StringRes message: Int) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val EXTRA_ACTION = "action"
        private const val EXTRA_NUMBER = "number"
        private const val EXTRA_LOOKUP_KEY = "lookup_key"
        private const val EXTRA_CONTACT_ID = "contact_id"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_MENU_ACTIONS = "menu_actions"
        private const val KEY_STAGE = "stage"

        /** Long enough for a natural double tap, short enough that a single tap still feels direct. */
        private const val SECOND_TAP_WINDOW_MILLIS = 300L

        private fun String.toTapAction(): TapAction? = TapAction.entries.firstOrNull { it.name == this }

        private fun personIntent(action: TapAction, person: WidgetPerson): Intent = Intent()
            .putExtra(EXTRA_ACTION, action.name)
            .putExtra(EXTRA_NUMBER, person.number)
            .putExtra(EXTRA_LOOKUP_KEY, person.lookupKey)
            .putExtra(EXTRA_CONTACT_ID, person.contactId)
            .putExtra(EXTRA_NAME, person.name)

        /** The person a widget's tile or row adds to its widget's shared tap template. */
        fun fillInIntent(config: WidgetConfig, person: WidgetPerson): Intent =
            personIntent(config.tapAction, person)
                .putExtra(EXTRA_MENU_ACTIONS, config.menuActions.map { it.name }.toTypedArray())

        /** For screens inside this app, which already know exactly what was asked for. */
        fun intent(context: Context, action: TapAction, person: WidgetPerson): Intent =
            personIntent(action, person)
                .setClass(context, ContactActionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
