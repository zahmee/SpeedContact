package com.speedcontact.app.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.speedcontact.app.model.AppState
import com.speedcontact.app.model.WidgetConfig
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}

private object AppStateSerializer : Serializer<AppState> {
    override val defaultValue = AppState()

    override suspend fun readFrom(input: InputStream): AppState = try {
        AppJson.decodeFromString(AppState.serializer(), input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Cannot read app state", e)
    }

    override suspend fun writeTo(t: AppState, output: OutputStream) {
        output.write(AppJson.encodeToString(AppState.serializer(), t).encodeToByteArray())
    }
}

private val Context.appStateDataStore by dataStore(
    fileName = "app_state.json",
    serializer = AppStateSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler { AppState() },
)

class AppStateRepository(context: Context) {
    private val store = context.applicationContext.appStateDataStore

    val state: Flow<AppState> = store.data

    suspend fun saveWidget(appWidgetId: Int, config: WidgetConfig) {
        store.updateData { it.copy(widgets = it.widgets + (appWidgetId to config)) }
    }

    /** Changes a saved widget in one step, so quick repeated taps each count. */
    suspend fun updateWidget(appWidgetId: Int, transform: (WidgetConfig) -> WidgetConfig) {
        store.updateData { state ->
            val current = state.widgets[appWidgetId] ?: return@updateData state
            state.copy(widgets = state.widgets + (appWidgetId to transform(current)))
        }
    }

    suspend fun removeWidget(appWidgetId: Int) {
        store.updateData { it.copy(widgets = it.widgets - appWidgetId) }
    }

    suspend fun recordCall(lookupKey: String, timeMillis: Long) {
        store.updateData { it.copy(lastCalled = it.lastCalled + (lookupKey to timeMillis)) }
    }
}
