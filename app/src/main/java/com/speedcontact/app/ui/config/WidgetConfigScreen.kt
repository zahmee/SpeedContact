@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.speedcontact.app.ui.config

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.speedcontact.app.R
import com.speedcontact.app.data.ResolvedPerson
import com.speedcontact.app.model.ContactSource
import com.speedcontact.app.model.FolderIcon
import com.speedcontact.app.model.ListOptions
import com.speedcontact.app.model.ListSpacing
import com.speedcontact.app.model.NameOptions
import com.speedcontact.app.model.NumberPlacement
import com.speedcontact.app.model.PhotoShape
import com.speedcontact.app.model.PhotoSize
import com.speedcontact.app.model.PlaceholderStyle
import com.speedcontact.app.model.SortOrder
import com.speedcontact.app.model.TapAction
import com.speedcontact.app.model.WidgetBackground
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.ui.AppIcon
import com.speedcontact.app.ui.asLeftToRight
import com.speedcontact.app.ui.drawableRes
import com.speedcontact.app.ui.labelRes
import com.speedcontact.app.widget.WidgetContent
import com.speedcontact.app.widget.WidgetKind
import kotlin.math.roundToInt
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableLazyListState
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun WidgetConfigScreen(
    kind: WidgetKind,
    state: ConfigUiState,
    preview: WidgetContent?,
    listState: LazyListState,
    canAskForPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onAddPeople: () -> Unit,
    onRemovePerson: (String) -> Unit,
    onMovePerson: (String, String) -> Unit,
    onPickFolderIcon: () -> Unit,
    loadCustomIcon: (String) -> Bitmap?,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    val config = state.config
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        onMovePerson(from.key as String, to.key as String)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(if (kind.isFolder) R.string.folder_settings else R.string.config_title),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            painterResource(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close),
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = onSave,
                        enabled = state.isLoaded,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding(),
        ) {
            WidgetPreview(kind, config, preview)

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                peopleSection(
                    state = state,
                    canAskForPermission = canAskForPermission,
                    onRequestPermission = onRequestPermission,
                    onOpenAppSettings = onOpenAppSettings,
                    onUpdate = onUpdate,
                    onAddPeople = onAddPeople,
                    onRemovePerson = onRemovePerson,
                    reorderState = reorderState,
                )
                tapActionSection(kind, config, onUpdate)
                sortSection(config, onUpdate)
                if (kind.isList) {
                    listAppearanceSection(kind, config, onUpdate)
                } else {
                    appearanceSection(kind, config, onUpdate)
                }
                titleSection(kind, config, onUpdate)
                if (kind.isFolder) folderIconSection(config, onUpdate, onPickFolderIcon, loadCustomIcon)
                nameSection(kind, config, onUpdate)
            }
        }
    }
}

private fun LazyListScope.peopleSection(
    state: ConfigUiState,
    canAskForPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onAddPeople: () -> Unit,
    onRemovePerson: (String) -> Unit,
    reorderState: ReorderableLazyListState,
) {
    val config = state.config
    val isManual = config.source == ContactSource.MANUAL
    val canReorder = config.sort == SortOrder.CUSTOM

    item(key = "people_header") {
        SectionHeader(R.string.section_people)
        ChoiceRow(
            options = ContactSource.entries,
            selected = config.source,
            label = { it.labelRes },
            onSelect = { source -> onUpdate { it.copy(source = source) } },
        )
    }

    if (!state.hasContactsPermission) {
        item(key = "people_permission") {
            PermissionCard(canAskForPermission, onRequestPermission, onOpenAppSettings)
        }
        return
    }

    if (!isManual) {
        item(key = "favorites_hint") {
            HelperText(
                if (state.people.isEmpty()) R.string.favorites_empty else R.string.favorites_hint,
            )
        }
    }

    // Favorites in a fixed sort have no order to manage, so the list adds nothing.
    if (isManual || canReorder) {
        items(state.people, key = { it.contact.lookupKey }) { person ->
            ReorderableItem(reorderState, key = person.contact.lookupKey) {
                PersonRow(
                    person = person,
                    canRemove = isManual,
                    canReorder = canReorder && state.people.size > 1,
                    onRemove = { onRemovePerson(person.contact.lookupKey) },
                )
            }
        }
    }

    if (isManual) {
        item(key = "add_people") {
            OutlinedButton(
                onClick = onAddPeople,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                Text(stringResource(R.string.add_people), Modifier.padding(start = 8.dp))
            }
        }
    }
}

private fun LazyListScope.tapActionSection(
    kind: WidgetKind,
    config: WidgetConfig,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
) {
    item(key = "tap_action") {
        SectionHeader(if (kind.isList) R.string.section_tap_action_name else R.string.section_tap_action)
        FieldLabel(R.string.quick_tap)
        Column(Modifier.selectableGroup()) {
            TapAction.entries.forEach { action ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = config.tapAction == action,
                            role = Role.RadioButton,
                            onClick = { onUpdate { it.copy(tapAction = action) } },
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    RadioButton(selected = config.tapAction == action, onClick = null)
                    Text(
                        text = stringResource(action.labelRes),
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }

        // A home screen keeps long presses for moving widgets; only a folder's own screen gets them.
        FieldLabel(if (kind.isFolder) R.string.hold_options else R.string.double_tap_options)
        TapAction.entries.filterNot { it == config.tapAction }.forEach { action ->
            val checked = action in config.secondaryActions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = checked,
                        role = Role.Checkbox,
                        onValueChange = { on ->
                            onUpdate {
                                it.copy(
                                    secondaryActions = if (on) {
                                        it.secondaryActions + action
                                    } else {
                                        it.secondaryActions - action
                                    },
                                )
                            }
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Text(
                    text = stringResource(action.labelRes),
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
        if (!kind.isFolder) HelperText(R.string.double_tap_hint)
    }
}

private fun LazyListScope.sortSection(
    config: WidgetConfig,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
) {
    item(key = "sort") {
        SectionHeader(R.string.section_sort)
        ChoiceRow(
            options = SortOrder.entries,
            selected = config.sort,
            label = { it.labelRes },
            onSelect = { sort -> onUpdate { it.copy(sort = sort) } },
        )
        if (config.sort == SortOrder.RECENT) HelperText(R.string.sort_recent_hint)
    }
}

private fun LazyListScope.appearanceSection(
    kind: WidgetKind,
    config: WidgetConfig,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
) {
    item(key = "appearance") {
        SectionHeader(R.string.section_appearance)

        FieldLabel(R.string.photo_size)
        ChoiceRow(
            options = PhotoSize.entries,
            selected = config.size,
            label = { it.labelRes },
            onSelect = { size -> onUpdate { it.copy(size = size) } },
        )

        FieldLabel(R.string.photo_shape)
        ChoiceRow(
            options = PhotoShape.entries,
            selected = config.shape,
            label = { it.labelRes },
            onSelect = { shape -> onUpdate { it.copy(shape = shape) } },
        )

        FieldLabel(R.string.placeholder_image)
        ChoiceRow(
            options = PlaceholderStyle.entries,
            selected = config.placeholder,
            label = { it.labelRes },
            onSelect = { style -> onUpdate { it.copy(placeholder = style) } },
        )

        BackgroundChooser(config, onUpdate)
        if (!kind.isFolder) PageBarOption(config, onUpdate)
    }
}

/** A names list has no photos to size or shape; its text is what can be adjusted. */
private fun LazyListScope.listAppearanceSection(
    kind: WidgetKind,
    config: WidgetConfig,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
) {
    fun updateList(transform: (ListOptions) -> ListOptions) =
        onUpdate { it.copy(list = transform(it.list)) }

    item(key = "appearance") {
        SectionHeader(R.string.section_appearance)

        FieldLabel(R.string.list_font_size)
        FontSizeRow(config.list) { size -> updateList { it.withFontSize(size) } }

        SwitchRow(
            label = R.string.list_bold,
            checked = config.list.boldName,
            onCheckedChange = { value -> updateList { it.copy(boldName = value) } },
        )
        SwitchRow(
            label = R.string.list_show_number,
            checked = config.list.showNumber,
            onCheckedChange = { value -> updateList { it.copy(showNumber = value) } },
        )
        if (config.list.showNumber) {
            FieldLabel(R.string.number_placement)
            ChoiceRow(
                options = NumberPlacement.entries,
                selected = config.list.numberPlacement,
                label = { it.labelRes },
                onSelect = { placement -> updateList { it.copy(numberPlacement = placement) } },
            )
        }
        SwitchRow(
            label = R.string.list_show_dividers,
            checked = config.list.showDividers,
            onCheckedChange = { value -> updateList { it.copy(showDividers = value) } },
        )

        FieldLabel(R.string.list_spacing)
        ChoiceRow(
            options = ListSpacing.entries,
            selected = config.list.spacing,
            label = { it.labelRes },
            onSelect = { spacing -> updateList { it.copy(spacing = spacing) } },
        )

        BackgroundChooser(config, onUpdate)
        if (!kind.isFolder) PageBarOption(config, onUpdate)
    }
}

/** Only for widgets on the home screen; a folder's own screen scrolls by itself. */
@Composable
private fun PageBarOption(
    config: WidgetConfig,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        SwitchRow(
            label = R.string.show_page_bar,
            checked = config.showPageBar,
            onCheckedChange = { value -> onUpdate { it.copy(showPageBar = value) } },
        )
        HelperText(R.string.page_bar_hint)
    }
}

private fun LazyListScope.folderIconSection(
    config: WidgetConfig,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onPickFromGallery: () -> Unit,
    loadCustomIcon: (String) -> Bitmap?,
) {
    item(key = "folder_icon") {
        val customFile = config.customIconFile
        val customIcon = remember(customFile) {
            customFile?.let(loadCustomIcon)?.asImageBitmap()
        }

        SectionHeader(R.string.section_folder_icon)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .selectableGroup(),
        ) {
            FolderIcon.entries.forEach { icon ->
                // A gallery picture only becomes a choice here once there is one.
                if (icon == FolderIcon.CUSTOM && customIcon == null) return@forEach
                FolderIconOption(
                    selected = config.folderIcon == icon,
                    label = stringResource(icon.labelRes),
                    onClick = { onUpdate { it.copy(folderIcon = icon) } },
                ) {
                    val imageModifier = Modifier.fillMaxSize()
                    when {
                        icon == FolderIcon.APP -> AppIcon(imageModifier)
                        icon == FolderIcon.CUSTOM && customIcon != null ->
                            Image(customIcon, contentDescription = null, modifier = imageModifier)
                        else -> Image(painterResource(icon.drawableRes), contentDescription = null, modifier = imageModifier)
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onPickFromGallery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(painterResource(R.drawable.ic_image), contentDescription = null)
            Text(stringResource(R.string.folder_icon_gallery), Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun FolderIconOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val ring = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .border(3.dp, ring, CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(6.dp),
    ) {
        content()
    }
}

@Composable
private fun FontSizeRow(options: ListOptions, onChange: (Int) -> Unit) {
    val range = ListOptions.MIN_FONT_SP.toFloat()..ListOptions.MAX_FONT_SP.toFloat()
    val stops = (ListOptions.MAX_FONT_SP - ListOptions.MIN_FONT_SP) / ListOptions.FONT_STEP_SP - 1

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        IconButton(
            onClick = { onChange(options.withFontStep(-1).fontSizeSp) },
            enabled = options.fontSizeSp > ListOptions.MIN_FONT_SP,
        ) {
            Icon(
                painterResource(R.drawable.ic_text_decrease),
                contentDescription = stringResource(R.string.cd_font_smaller),
            )
        }
        Slider(
            value = options.fontSizeSp.toFloat(),
            onValueChange = { value ->
                val size = value.roundToInt()
                if (size != options.fontSizeSp) onChange(size)
            },
            valueRange = range,
            steps = stops,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onChange(options.withFontStep(1).fontSizeSp) },
            enabled = options.fontSizeSp < ListOptions.MAX_FONT_SP,
        ) {
            Icon(
                painterResource(R.drawable.ic_text_increase),
                contentDescription = stringResource(R.string.cd_font_larger),
            )
        }
        Text(
            text = stringResource(R.string.list_font_size_value, options.fontSizeSp),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}

@Composable
private fun BackgroundChooser(
    config: WidgetConfig,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
) {
    FieldLabel(R.string.background)
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        WidgetBackground.entries.forEach { background ->
            FilterChip(
                selected = config.background == background,
                onClick = { onUpdate { it.copy(background = background) } },
                label = { Text(stringResource(background.labelRes)) },
                leadingIcon = { BackgroundSwatch(background) },
            )
        }
    }
}

private fun LazyListScope.titleSection(
    kind: WidgetKind,
    config: WidgetConfig,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
) {
    val isFolder = kind.isFolder
    item(key = "title") {
        SectionHeader(if (isFolder) R.string.section_folder_name else R.string.section_title)
        SwitchRow(
            label = if (isFolder) R.string.show_folder_name else R.string.show_title,
            checked = config.showTitle,
            onCheckedChange = { show -> onUpdate { it.copy(showTitle = show) } },
        )
        if (config.showTitle) {
            OutlinedTextField(
                value = config.title,
                onValueChange = { title -> onUpdate { it.copy(title = title) } },
                label = {
                    Text(stringResource(if (isFolder) R.string.folder_name_label else R.string.title_label))
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

private fun LazyListScope.nameSection(
    kind: WidgetKind,
    config: WidgetConfig,
    onUpdate: ((WidgetConfig) -> WidgetConfig) -> Unit,
) {
    fun updateName(transform: (NameOptions) -> NameOptions) =
        onUpdate { it.copy(name = transform(it.name)) }

    item(key = "name") {
        SectionHeader(R.string.section_name)
        // A names list is its names, so hiding them is not an option there.
        if (!kind.isList) {
            SwitchRow(
                label = R.string.show_name,
                checked = config.name.showName,
                onCheckedChange = { value -> updateName { it.copy(showName = value) } },
            )
            if (!config.name.showName) {
                SwitchRow(
                    label = R.string.full_name_without_photo,
                    checked = config.name.fullNameWithoutPhoto,
                    onCheckedChange = { value -> updateName { it.copy(fullNameWithoutPhoto = value) } },
                )
            }
        }
        if (kind.isList || config.name.showName) {
            SwitchRow(
                label = R.string.prefer_nickname,
                checked = config.name.preferNickname,
                onCheckedChange = { value -> updateName { it.copy(preferNickname = value) } },
            )
            SwitchRow(
                label = R.string.full_name,
                checked = config.name.fullName,
                onCheckedChange = { value -> updateName { it.copy(fullName = value) } },
            )
            if (config.name.fullName) {
                SwitchRow(
                    label = R.string.last_name_first,
                    checked = config.name.lastNameFirst,
                    onCheckedChange = { value -> updateName { it.copy(lastNameFirst = value) } },
                )
            }
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.PersonRow(
    person: ResolvedPerson,
    canRemove: Boolean,
    canReorder: Boolean,
    onRemove: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        ) {
            ContactPhoto(person.contact.thumbnailUri)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(
                    text = person.contact.displayName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = person.number.asLeftToRight(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(
                            R.string.cd_remove_person,
                            person.contact.displayName,
                        ),
                    )
                }
            }
            if (canReorder) {
                Icon(
                    painterResource(R.drawable.ic_drag_handle),
                    contentDescription = stringResource(R.string.cd_drag_person),
                    modifier = Modifier
                        .draggableHandle()
                        .padding(12.dp),
                )
            }
        }
    }
}

@Composable
fun ContactPhoto(thumbnailUri: String?, modifier: Modifier = Modifier) {
    val placeholder = painterResource(R.drawable.ic_person)
    AsyncImage(
        model = thumbnailUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun PermissionCard(
    canAskForPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.permission_contacts_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.permission_contacts_body),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            FilledTonalButton(
                onClick = if (canAskForPermission) onRequestPermission else onOpenAppSettings,
            ) {
                Text(
                    stringResource(
                        if (canAskForPermission) R.string.permission_allow else R.string.permission_open_settings,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BackgroundSwatch(background: WidgetBackground) {
    val color = when (background) {
        WidgetBackground.DYNAMIC -> MaterialTheme.colorScheme.primary
        else -> Color(background.argb)
    }
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

@Composable
private fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> Int,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                label = {
                    // A long word in some languages shrinks to fit a narrow phone instead of losing its end.
                    Text(
                        text = stringResource(label(option)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            minFontSize = 10.sp,
                            maxFontSize = LocalTextStyle.current.fontSize,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun SwitchRow(label: Int, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(stringResource(label), Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SectionHeader(title: Int) {
    Column {
        HorizontalDivider(Modifier.padding(top = 8.dp))
        Text(
            text = stringResource(title),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        )
    }
}

@Composable
private fun FieldLabel(label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun HelperText(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
