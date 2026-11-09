package com.speedcontact.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.speedcontact.app.R
import com.speedcontact.app.model.ContactSource
import com.speedcontact.app.model.FolderIcon
import com.speedcontact.app.model.ListSpacing
import com.speedcontact.app.model.NumberPlacement
import com.speedcontact.app.model.PhotoShape
import com.speedcontact.app.model.PhotoSize
import com.speedcontact.app.model.PlaceholderStyle
import com.speedcontact.app.model.SortOrder
import com.speedcontact.app.model.TapAction
import com.speedcontact.app.model.WidgetBackground

val ContactSource.labelRes: Int
    @StringRes get() = when (this) {
        ContactSource.MANUAL -> R.string.source_manual
        ContactSource.FAVORITES -> R.string.source_favorites
    }

val TapAction.labelRes: Int
    @StringRes get() = when (this) {
        TapAction.CALL -> R.string.action_call
        TapAction.WHATSAPP_CHAT -> R.string.action_whatsapp_chat
        TapAction.WHATSAPP_CALL -> R.string.action_whatsapp_call
        TapAction.SMS -> R.string.action_sms
        TapAction.CONTACT_CARD -> R.string.action_card
    }

/** Read aloud by screen readers on each photo; takes the person's name. */
val TapAction.descriptionRes: Int
    @StringRes get() = when (this) {
        TapAction.CALL -> R.string.cd_call
        TapAction.WHATSAPP_CHAT -> R.string.cd_whatsapp_chat
        TapAction.WHATSAPP_CALL -> R.string.cd_whatsapp_call
        TapAction.SMS -> R.string.cd_sms
        TapAction.CONTACT_CARD -> R.string.cd_card
    }

val SortOrder.labelRes: Int
    @StringRes get() = when (this) {
        SortOrder.CUSTOM -> R.string.sort_custom
        SortOrder.ALPHABETICAL -> R.string.sort_alphabetical
        SortOrder.RECENT -> R.string.sort_recent
    }

val PhotoSize.labelRes: Int
    @StringRes get() = when (this) {
        PhotoSize.SMALL -> R.string.size_small
        PhotoSize.MEDIUM -> R.string.size_medium
        PhotoSize.LARGE -> R.string.size_large
    }

val PhotoShape.labelRes: Int
    @StringRes get() = when (this) {
        PhotoShape.ROUNDED -> R.string.shape_rounded
        PhotoShape.CIRCLE -> R.string.shape_circle
        PhotoShape.SQUARE -> R.string.shape_square
    }

val PlaceholderStyle.labelRes: Int
    @StringRes get() = when (this) {
        PlaceholderStyle.LIGHT -> R.string.placeholder_light
        PlaceholderStyle.DARK -> R.string.placeholder_dark
    }

val ListSpacing.labelRes: Int
    @StringRes get() = when (this) {
        ListSpacing.COMPACT -> R.string.spacing_compact
        ListSpacing.NORMAL -> R.string.spacing_normal
        ListSpacing.WIDE -> R.string.spacing_wide
    }

val NumberPlacement.labelRes: Int
    @StringRes get() = when (this) {
        NumberPlacement.BELOW -> R.string.number_below
        NumberPlacement.BESIDE -> R.string.number_beside
    }

val FolderIcon.labelRes: Int
    @StringRes get() = when (this) {
        FolderIcon.APP -> R.string.icon_app
        FolderIcon.PEOPLE -> R.string.icon_people
        FolderIcon.HOME -> R.string.icon_home
        FolderIcon.WORK -> R.string.icon_work
        FolderIcon.STAR -> R.string.icon_star
        FolderIcon.HEART -> R.string.icon_heart
        FolderIcon.PHONE -> R.string.icon_phone
        FolderIcon.CHAT -> R.string.icon_chat
        FolderIcon.SCHOOL -> R.string.icon_school
        FolderIcon.HOSPITAL -> R.string.icon_hospital
        FolderIcon.SHOP -> R.string.icon_shop
        FolderIcon.BUILDING -> R.string.icon_building
        FolderIcon.FOLDER -> R.string.icon_folder
        FolderIcon.CUSTOM -> R.string.icon_custom
    }

/** The app's own icon also stands in for a gallery picture that can no longer be read. */
val FolderIcon.drawableRes: Int
    @DrawableRes get() = when (this) {
        FolderIcon.APP, FolderIcon.CUSTOM -> R.mipmap.ic_launcher
        FolderIcon.PEOPLE -> R.drawable.folder_icon_people
        FolderIcon.HOME -> R.drawable.folder_icon_home
        FolderIcon.WORK -> R.drawable.folder_icon_work
        FolderIcon.STAR -> R.drawable.folder_icon_star
        FolderIcon.HEART -> R.drawable.folder_icon_heart
        FolderIcon.PHONE -> R.drawable.folder_icon_phone
        FolderIcon.CHAT -> R.drawable.folder_icon_chat
        FolderIcon.SCHOOL -> R.drawable.folder_icon_school
        FolderIcon.HOSPITAL -> R.drawable.folder_icon_hospital
        FolderIcon.SHOP -> R.drawable.folder_icon_shop
        FolderIcon.BUILDING -> R.drawable.folder_icon_building
        FolderIcon.FOLDER -> R.drawable.folder_icon_folder
    }

val WidgetBackground.labelRes: Int
    @StringRes get() = when (this) {
        WidgetBackground.BLUE -> R.string.background_blue
        WidgetBackground.DYNAMIC -> R.string.background_dynamic
        WidgetBackground.TRANSPARENT -> R.string.background_transparent
        WidgetBackground.BLACK_TRANSLUCENT -> R.string.background_black_translucent
        WidgetBackground.WHITE_TRANSLUCENT -> R.string.background_white_translucent
        WidgetBackground.BLACK -> R.string.background_black
        WidgetBackground.WHITE -> R.string.background_white
        WidgetBackground.PINK -> R.string.background_pink
    }
