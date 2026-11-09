package com.speedcontact.app.ui.folder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.speedcontact.app.R
import com.speedcontact.app.domain.ContactSearch
import com.speedcontact.app.domain.GridMetrics
import com.speedcontact.app.model.ListOptions
import com.speedcontact.app.model.TapAction
import com.speedcontact.app.model.WidgetBackground
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.ui.ContactNameRow
import com.speedcontact.app.ui.ContactOptionsDialog
import com.speedcontact.app.ui.ContactTile
import com.speedcontact.app.ui.widgetBackgroundColor
import com.speedcontact.app.ui.widgetForegroundColor
import com.speedcontact.app.widget.WidgetContent
import com.speedcontact.app.widget.WidgetKind
import com.speedcontact.app.widget.WidgetPerson
import com.speedcontact.app.widget.WidgetTile
import com.speedcontact.app.widget.WidgetUiState

private val GridPadding = 8.dp
private const val FOLDER_FONT_SIZE_BOOST = 2

// "Transparent" alone would put names over a busy home screen, so the folder dims it.
private val TransparentScrim = Color(0xCC000000)

@Composable
fun FolderScreen(
    kind: WidgetKind,
    state: WidgetUiState,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
    onPersonAction: (TapAction, WidgetPerson) -> Unit,
    onFontStep: (Int) -> Unit,
) {
    val config = state.config
    val foreground = widgetForegroundColor(config.background)
    val background = if (config.background == WidgetBackground.TRANSPARENT) {
        TransparentScrim
    } else {
        widgetBackgroundColor(config.background)
    }

    var searching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var optionsFor by remember { mutableStateOf<WidgetPerson?>(null) }
    val haptics = LocalHapticFeedback.current

    val closeSearch = {
        searching = false
        query = ""
    }
    BackHandler(enabled = searching, onBack = closeSearch)

    val onPersonTap: (WidgetPerson) -> Unit = { onPersonAction(config.tapAction, it) }
    // With nothing to offer, a long press does nothing rather than opening an empty menu.
    val onPersonHold: ((WidgetPerson) -> Unit)? = { person: WidgetPerson ->
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        optionsFor = person
    }.takeIf { config.menuActions.isNotEmpty() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            FolderIconButton(R.drawable.ic_close, R.string.close, foreground, onClick = onClose)
            Text(
                text = config.title.takeIf { config.showTitle && it.isNotBlank() }
                    ?: stringResource(kind.untitledLabel),
                color = foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
            if (state.content is WidgetContent.Names) {
                FolderIconButton(
                    icon = R.drawable.ic_search,
                    description = R.string.cd_search,
                    tint = foreground,
                    onClick = { if (searching) closeSearch() else searching = true },
                )
                FolderIconButton(
                    icon = R.drawable.ic_text_decrease,
                    description = R.string.cd_font_smaller,
                    tint = foreground,
                    enabled = config.list.fontSizeSp > ListOptions.MIN_FONT_SP,
                    onClick = { onFontStep(-1) },
                )
                FolderIconButton(
                    icon = R.drawable.ic_text_increase,
                    description = R.string.cd_font_larger,
                    tint = foreground,
                    enabled = config.list.fontSizeSp < ListOptions.MAX_FONT_SP,
                    onClick = { onFontStep(1) },
                )
            }
            FolderIconButton(R.drawable.ic_settings, R.string.folder_settings, foreground, onClick = onOpenSettings)
        }

        if (searching) FolderSearchField(query, { query = it }, foreground)

        when (val content = state.content) {
            is WidgetContent.Tiles -> FolderGrid(content.items, config, onPersonTap, onPersonHold)
            is WidgetContent.Names -> FolderNames(content.items, config, foreground, query, onPersonTap, onPersonHold)
            WidgetContent.NeedsPermission -> FolderMessage(
                if (kind.isList) R.string.preview_needs_permission_names else R.string.preview_needs_permission,
                foreground,
                onOpenSettings,
            )
            WidgetContent.NoFavorites ->
                FolderMessage(R.string.preview_no_favorites, foreground, onOpenSettings)
            WidgetContent.NoPeople ->
                FolderMessage(R.string.folder_empty, foreground, onOpenSettings)
        }
    }

    optionsFor?.let { person ->
        ContactOptionsDialog(
            name = person.name,
            actions = config.menuActions,
            onChoose = { action -> onPersonAction(action, person) },
            onDismiss = { optionsFor = null },
        )
    }
}

@Composable
private fun FolderIconButton(
    icon: Int,
    description: Int,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painterResource(icon),
            contentDescription = stringResource(description),
            tint = if (enabled) tint else tint.copy(alpha = 0.38f),
        )
    }
}

@Composable
private fun FolderSearchField(query: String, onQueryChange: (String) -> Unit, foreground: Color) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.picker_search)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = foreground,
            unfocusedTextColor = foreground,
            cursorColor = foreground,
            focusedBorderColor = foreground,
            unfocusedBorderColor = foreground.copy(alpha = 0.5f),
            focusedPlaceholderColor = foreground.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = foreground.copy(alpha = 0.7f),
            focusedLeadingIconColor = foreground,
            unfocusedLeadingIconColor = foreground,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .focusRequester(focus),
    )
}

@Composable
private fun FolderNames(
    people: List<WidgetPerson>,
    config: WidgetConfig,
    foreground: Color,
    query: String,
    onPersonTap: (WidgetPerson) -> Unit,
    onPersonHold: ((WidgetPerson) -> Unit)?,
) {
    val visible = remember(people, query) {
        val search = ContactSearch(query)
        people.filter { search.matches(it.name, listOf(it.number)) }
    }
    val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (visible.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
        ) {
            Text(
                text = stringResource(R.string.folder_search_empty),
                color = foreground,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    // Same as the home screen widgets: lines must not stretch when dragged past the edge.
    CompositionLocalProvider(LocalOverscrollFactory provides null) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = GridPadding,
                end = GridPadding,
                bottom = GridPadding + navigationBar,
            ),
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            itemsIndexed(visible, key = { _, person -> person.contactId }) { index, person ->
                ContactNameRow(
                    person = person,
                    config = config,
                    foreground = foreground,
                    isLast = index == visible.lastIndex,
                    onClick = { onPersonTap(person) },
                    onLongClick = onPersonHold?.let { hold -> { hold(person) } },
                )
            }
        }
    }
}

@Composable
private fun FolderGrid(
    tiles: List<WidgetTile>,
    config: WidgetConfig,
    onPersonTap: (WidgetPerson) -> Unit,
    onPersonHold: ((WidgetPerson) -> Unit)?,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val available = maxWidth - GridPadding * 2
        val columns = GridMetrics.folderColumns(available.value, config.size)
        val navigationBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        // Same as the home screen grid: photos must not stretch when dragged past the edge.
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(
                    start = GridPadding,
                    end = GridPadding,
                    top = 4.dp,
                    bottom = GridPadding + navigationBar,
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(tiles, key = { it.person.contactId }) { tile ->
                    ContactTile(
                        tile = tile,
                        config = config,
                        cell = available / columns,
                        fontSizeBoost = FOLDER_FONT_SIZE_BOOST,
                        onClick = { onPersonTap(tile.person) },
                        onLongClick = onPersonHold?.let { hold -> { hold(tile.person) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderMessage(messageRes: Int, color: Color, onOpenSettings: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(messageRes),
                color = color,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.folder_choose_people))
            }
        }
    }
}
