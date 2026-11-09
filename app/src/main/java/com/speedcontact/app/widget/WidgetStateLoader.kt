package com.speedcontact.app.widget

import android.content.Context
import android.graphics.Bitmap
import com.speedcontact.app.data.AppStateRepository
import com.speedcontact.app.data.ContactsRepository
import com.speedcontact.app.data.ResolvedPerson
import com.speedcontact.app.domain.ContactSorter
import com.speedcontact.app.domain.GridMetrics
import com.speedcontact.app.domain.NameFormatter
import com.speedcontact.app.domain.NameParts
import com.speedcontact.app.model.ContactSource
import com.speedcontact.app.model.SortOrder
import com.speedcontact.app.model.WidgetConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Someone a widget shows, and everything a tap on them needs. */
data class WidgetPerson(
    val contactId: Long,
    val lookupKey: String,
    val number: String,
    val name: String,
)

data class WidgetTile(
    val person: WidgetPerson,
    val image: Bitmap,
    /** This person has no photo and the widget was set to show such a person's full name. */
    val showsFullName: Boolean = false,
)

sealed interface WidgetContent {
    data object NeedsPermission : WidgetContent
    data object NoPeople : WidgetContent
    data object NoFavorites : WidgetContent
    data class Tiles(val items: List<WidgetTile>) : WidgetContent
    data class Names(val items: List<WidgetPerson>) : WidgetContent
}

data class WidgetUiState(
    val config: WidgetConfig,
    val content: WidgetContent,
)

/** Everything a widget's appearance depends on; a new value means it must reload. */
data class WidgetLoadKey(
    val config: WidgetConfig?,
    val lastCalled: Map<String, Long>,
    val refreshTick: Long,
)

private data class NamedPerson(
    val person: ResolvedPerson,
    val name: String,
    val fullName: String,
) {
    fun toWidgetPerson(shownName: String = name) = WidgetPerson(
        contactId = person.contact.id,
        lookupKey = person.contact.lookupKey,
        number = person.number,
        name = shownName,
    )
}

class WidgetStateLoader(
    context: Context,
    private val store: AppStateRepository,
    private val contacts: ContactsRepository,
) {
    private val density = context.resources.displayMetrics.density
    private val renderer = ContactImageRenderer(context.applicationContext)
    private val refreshTicks = MutableStateFlow(0L)

    /** Asks live widgets to read contacts again, for example after a contact was edited. */
    fun requestRefresh() = refreshTicks.update { it + 1 }

    fun loadKeys(appWidgetId: Int): Flow<WidgetLoadKey> =
        combine(store.state, refreshTicks) { state, tick ->
            val config = state.widgets[appWidgetId]
            WidgetLoadKey(
                config = config,
                lastCalled = if (config?.sort == SortOrder.RECENT) state.lastCalled else emptyMap(),
                refreshTick = tick,
            )
        }.distinctUntilChanged()

    suspend fun load(key: WidgetLoadKey): WidgetUiState {
        val config = key.config ?: WidgetConfig()
        return WidgetUiState(config, loadContent(config, key.lastCalled))
    }

    /** The full screen of a folder: the same people as a widget, in larger photos. */
    suspend fun loadFolderScreen(key: WidgetLoadKey): WidgetUiState {
        val config = key.config ?: WidgetConfig()
        val content = loadContent(
            config = config,
            lastCalled = key.lastCalled,
            sizePx = GridMetrics.folderBitmapPx(config.size, density),
        )
        return WidgetUiState(config, content)
    }

    suspend fun loadNames(key: WidgetLoadKey): WidgetUiState {
        val config = key.config ?: WidgetConfig()
        return WidgetUiState(config, loadNameContent(config, key.lastCalled))
    }

    suspend fun loadContent(
        config: WidgetConfig,
        lastCalled: Map<String, Long>,
        sizePx: Int = GridMetrics.bitmapPx(config.size, density),
    ): WidgetContent = loadPeople(config, lastCalled) { sorted ->
        val limiter = Semaphore(PHOTO_DECODE_CONCURRENCY)
        val tiles = coroutineScope {
            sorted.map { named ->
                async {
                    limiter.withPermit { renderTile(named, config, sizePx) }
                }
            }.awaitAll()
        }
        WidgetContent.Tiles(tiles)
    }

    /** The same people in the same order as a photo widget, but no photo is ever read. */
    suspend fun loadNameContent(
        config: WidgetConfig,
        lastCalled: Map<String, Long>,
    ): WidgetContent = loadPeople(config, lastCalled) { sorted ->
        WidgetContent.Names(sorted.map { it.toWidgetPerson() })
    }

    private suspend fun renderTile(named: NamedPerson, config: WidgetConfig, sizePx: Int): WidgetTile {
        val photo = contacts.loadPhoto(named.person.contact.id, sizePx)
        val showsFullName = photo == null && config.name.showsFullNameWithoutPhoto
        val image = withContext(Dispatchers.Default) {
            renderer.render(
                photo = photo,
                shape = config.shape,
                placeholder = config.placeholder,
                sizePx = sizePx,
                withScrim = config.name.showName,
                withPersonIcon = !showsFullName,
            )
        }
        return WidgetTile(
            person = named.toWidgetPerson(if (showsFullName) named.fullName else named.name),
            image = image,
            showsFullName = showsFullName,
        )
    }

    private suspend fun loadPeople(
        config: WidgetConfig,
        lastCalled: Map<String, Long>,
        build: suspend (List<NamedPerson>) -> WidgetContent,
    ): WidgetContent {
        if (!contacts.hasPermission()) return WidgetContent.NeedsPermission

        val people = contacts.resolvePeople(config)
        if (people.isEmpty()) {
            return if (config.source == ContactSource.FAVORITES) {
                WidgetContent.NoFavorites
            } else {
                WidgetContent.NoPeople
            }
        }

        val names = contacts.nameParts(people.map { it.contact.id })
        val fullNameOptions = config.name.copy(preferNickname = false, fullName = true)
        val named = people.map { person ->
            val parts = names[person.contact.id]
                ?.let { if (it.display.isBlank()) it.copy(display = person.contact.displayName) else it }
                ?: NameParts(person.contact.displayName)
            NamedPerson(
                person = person,
                name = NameFormatter.format(parts, config.name),
                fullName = NameFormatter.format(parts, fullNameOptions),
            )
        }
        return build(
            ContactSorter.sort(
                items = named,
                order = config.sort,
                key = { it.person.contact.lookupKey },
                name = { it.name },
                lastCalled = lastCalled,
            ),
        )
    }

    private companion object {
        const val PHOTO_DECODE_CONCURRENCY = 4
    }
}
