package com.speedcontact.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedcontact.app.domain.GridMetrics
import com.speedcontact.app.model.PhotoShape
import com.speedcontact.app.model.WidgetBackground
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.widget.WidgetTile

/** A person's photo and name, drawn the same way as on the home screen widget. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactTile(
    tile: WidgetTile,
    config: WidgetConfig,
    cell: Dp,
    fontSizeBoost: Int = 0,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val description = stringResource(config.tapAction.descriptionRes, tile.person.name)
    val clickModifier = if (onClick != null) {
        Modifier
            .clip(config.shape.tapShape(cell))
            .combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .semantics { contentDescription = description }
    } else {
        Modifier
    }
    val isCircle = config.shape == PhotoShape.CIRCLE
    val fontSize = (GridMetrics.nameFontSizeSp(config.size) + fontSizeBoost).sp

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .size(cell)
            .padding(3.dp)
            .then(clickModifier),
    ) {
        Image(
            bitmap = tile.image.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        if (tile.showsFullName) {
            Text(
                text = tile.person.name,
                color = Color(config.placeholder.nameArgb),
                maxLines = GridMetrics.fullNameMaxLines(config.size),
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = if (isCircle) cell * 0.14f else 4.dp),
            )
        }
        if (config.name.showName) {
            Text(
                text = tile.person.name,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                fontSize = fontSize,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isCircle) cell * 0.14f else 4.dp,
                        end = if (isCircle) cell * 0.14f else 4.dp,
                        bottom = if (isCircle) cell * 0.08f else 4.dp,
                    ),
            )
        }
    }
}

private fun PhotoShape.tapShape(cell: Dp): Shape = when (this) {
    PhotoShape.CIRCLE -> CircleShape
    PhotoShape.ROUNDED -> RoundedCornerShape(cell * 0.12f)
    PhotoShape.SQUARE -> RectangleShape
}

@Composable
fun widgetBackgroundColor(background: WidgetBackground): Color =
    if (background == WidgetBackground.DYNAMIC) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        Color(background.argb)
    }

@Composable
fun widgetForegroundColor(background: WidgetBackground): Color = when {
    background == WidgetBackground.DYNAMIC -> MaterialTheme.colorScheme.onSurface
    background.isDark -> Color.White
    else -> Color(0xFF15181D)
}

/** Whether content on this background is light, so system bar icons can match it. */
@Composable
fun isDarkBackground(background: WidgetBackground): Boolean =
    if (background == WidgetBackground.DYNAMIC) {
        androidx.compose.foundation.isSystemInDarkTheme()
    } else {
        background.isDark
    }
