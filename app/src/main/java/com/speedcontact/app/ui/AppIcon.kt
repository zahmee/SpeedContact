package com.speedcontact.app.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.speedcontact.app.R

private const val ICON_PX = 240

/** The app's launcher icon. It is adaptive, which Compose's own image loading cannot draw. */
@Composable
fun AppIcon(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val icon = remember(context) {
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?.toBitmap(ICON_PX, ICON_PX)
            ?.asImageBitmap()
    }
    if (icon != null) Image(icon, contentDescription = null, modifier = modifier)
}
