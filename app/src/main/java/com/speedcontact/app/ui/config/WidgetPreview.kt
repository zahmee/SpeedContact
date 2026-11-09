package com.speedcontact.app.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.speedcontact.app.R
import com.speedcontact.app.domain.GridMetrics
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.ui.ContactNameRow
import com.speedcontact.app.ui.ContactTile
import com.speedcontact.app.ui.widgetBackgroundColor
import com.speedcontact.app.ui.widgetForegroundColor
import com.speedcontact.app.widget.WidgetContent
import com.speedcontact.app.widget.WidgetKind
import com.speedcontact.app.widget.WidgetPerson
import com.speedcontact.app.widget.WidgetTile

private val WidgetPadding = 6.dp

// More lines than fit in the preview, even at the smallest text and spacing.
private const val PREVIEW_MAX_LINES = 10

/** A live sample of the widget over a wallpaper-like backdrop, so transparency is visible. */
@Composable
fun WidgetPreview(
    kind: WidgetKind,
    config: WidgetConfig,
    content: WidgetContent?,
    modifier: Modifier = Modifier,
) {
    val backdrop = Brush.linearGradient(listOf(Color(0xFF5B7C8D), Color(0xFFC39A6B)))
    val foreground = widgetForegroundColor(config.background)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp)
            .background(backdrop)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .clearAndSetSemantics { },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(widgetBackgroundColor(config.background))
                .padding(WidgetPadding),
        ) {
            if (config.showTitle && config.title.isNotBlank()) {
                Text(
                    text = config.title,
                    color = foreground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            when (content) {
                is WidgetContent.Tiles -> PreviewGrid(content.items, config)
                is WidgetContent.Names -> PreviewNames(content.items, config, foreground)
                WidgetContent.NoFavorites -> PreviewMessage(R.string.preview_no_favorites, foreground)
                WidgetContent.NeedsPermission -> PreviewMessage(
                    if (kind.isList) R.string.preview_needs_permission_names else R.string.preview_needs_permission,
                    foreground,
                )
                WidgetContent.NoPeople -> PreviewMessage(
                    if (kind.isList) R.string.preview_no_people_names else R.string.preview_no_people,
                    foreground,
                )
                null -> Unit
            }
        }
    }
}

@Composable
private fun PreviewGrid(tiles: List<WidgetTile>, config: WidgetConfig) {
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val columns = GridMetrics.columns(maxWidth.value, config.size)
        val cell = maxWidth / columns
        val visibleRows = (maxHeight / cell).toInt() + 1
        // Unbounded so a partly visible last row is clipped, as in the widget, and not squeezed.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true),
        ) {
            tiles.chunked(columns).take(visibleRows).forEach { row ->
                Row(horizontalArrangement = Arrangement.Start) {
                    row.forEach { tile -> ContactTile(tile, config, cell) }
                }
            }
        }
    }
}

@Composable
private fun PreviewNames(people: List<WidgetPerson>, config: WidgetConfig, color: Color) {
    Box(Modifier.fillMaxSize().clipToBounds()) {
        // Unbounded so a partly visible last line is clipped, as in the widget, and not squeezed.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true),
        ) {
            people.take(PREVIEW_MAX_LINES).forEachIndexed { index, person ->
                ContactNameRow(person, config, color, isLast = index == people.lastIndex)
            }
        }
    }
}

@Composable
private fun PreviewMessage(messageRes: Int, color: Color) {
    Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(messageRes),
            color = color,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
