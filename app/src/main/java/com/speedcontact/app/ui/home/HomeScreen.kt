@file:OptIn(ExperimentalMaterial3Api::class)

package com.speedcontact.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.speedcontact.app.R
import com.speedcontact.app.model.ContactSource
import com.speedcontact.app.ui.labelRes
import com.speedcontact.app.widget.WidgetKind

@Composable
fun HomeScreen(
    items: List<HomeItem>,
    onAddWidget: (WidgetKind) -> Unit,
    onOpenWidget: (Int) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenLanguage: (() -> Unit)?,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    if (onOpenLanguage != null) {
                        IconButton(onClick = onOpenLanguage) {
                            Icon(
                                painterResource(R.drawable.ic_language),
                                contentDescription = stringResource(R.string.language_title),
                            )
                        }
                    }
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            painterResource(R.drawable.ic_info),
                            contentDescription = stringResource(R.string.about_title),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (items.isEmpty()) {
            EmptyHome(onAddWidget, Modifier.padding(padding))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.home_my_widgets),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(items, key = { it.appWidgetId }) { item ->
                    WidgetCard(item, onClick = { onOpenWidget(item.appWidgetId) })
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetKind.entries.forEach { kind ->
                            OutlinedButton(
                                onClick = { onAddWidget(kind) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                                Text(stringResource(kind.addLabel), Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHome(onAddWidget: (WidgetKind) -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        ) {
            Icon(
                painterResource(R.drawable.ic_person),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(
            text = stringResource(R.string.home_intro_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = stringResource(R.string.home_intro_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        Button(
            onClick = { onAddWidget(WidgetKind.GRID) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(WidgetKind.GRID.addLabel))
        }
        WidgetKind.entries.filterNot { it == WidgetKind.GRID }.forEach { kind ->
            OutlinedButton(
                onClick = { onAddWidget(kind) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(stringResource(kind.addLabel))
            }
        }
    }
}

@Composable
private fun WidgetCard(item: HomeItem, onClick: () -> Unit) {
    val config = item.config
    val untitled = stringResource(item.kind.untitledLabel)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = config?.title?.takeIf { it.isNotBlank() } ?: untitled,
                style = MaterialTheme.typography.titleSmall,
            )
            val people = when {
                config == null -> stringResource(R.string.home_widget_needs_setup)
                config.source == ContactSource.FAVORITES -> stringResource(config.source.labelRes)
                else -> pluralStringResource(R.plurals.people_count, config.contacts.size, config.contacts.size)
            }
            Text(
                text = stringResource(R.string.home_widget_summary, stringResource(item.kind.label), people),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private val WidgetKind.label: Int
    get() = when (this) {
        WidgetKind.GRID -> R.string.home_kind_grid
        WidgetKind.FOLDER -> R.string.home_kind_folder
        WidgetKind.NAME_LIST -> R.string.home_kind_list
        WidgetKind.LIST_FOLDER -> R.string.home_kind_list_folder
    }

private val WidgetKind.addLabel: Int
    get() = when (this) {
        WidgetKind.GRID -> R.string.home_add_grid
        WidgetKind.FOLDER -> R.string.home_add_folder
        WidgetKind.NAME_LIST -> R.string.home_add_list
        WidgetKind.LIST_FOLDER -> R.string.home_add_list_folder
    }
