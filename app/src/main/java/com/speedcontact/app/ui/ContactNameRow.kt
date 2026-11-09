package com.speedcontact.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedcontact.app.model.WidgetConfig
import com.speedcontact.app.widget.WidgetPerson

/** One person in a names list, drawn the same way as on the home screen widget. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactNameRow(
    person: WidgetPerson,
    config: WidgetConfig,
    foreground: Color,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val options = config.list
    val description = stringResource(config.tapAction.descriptionRes, person.name)
    val clickModifier = if (onClick != null) {
        Modifier
            .combinedClickable(role = Role.Button, onLongClick = onLongClick, onClick = onClick)
            .semantics { contentDescription = description }
    } else {
        Modifier
    }
    // Filling its line, a Latin name would otherwise line up on the opposite side from Arabic ones.
    val nameAlign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextAlign.Right else TextAlign.Left
    val numberColor = foreground.copy(alpha = 0.75f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = options.spacing.rowPaddingDp.dp),
        ) {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    text = person.name,
                    color = foreground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = nameAlign,
                    fontSize = options.fontSizeSp.sp,
                    fontWeight = if (options.boldName) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .weight(1f)
                        .alignByBaseline(),
                )
                if (options.showsNumberBeside) {
                    Text(
                        text = person.number.asLeftToRight(),
                        color = numberColor,
                        maxLines = 1,
                        fontSize = options.numberFontSizeSp.sp,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .alignByBaseline(),
                    )
                }
            }
            if (options.showsNumberBelow) {
                Text(
                    text = person.number.asLeftToRight(),
                    color = numberColor,
                    maxLines = 1,
                    fontSize = options.numberFontSizeSp.sp,
                )
            }
        }
        if (options.showDividers && !isLast) {
            HorizontalDivider(color = foreground.copy(alpha = 0.25f))
        }
    }
}
