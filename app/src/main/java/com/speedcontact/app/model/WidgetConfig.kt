package com.speedcontact.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class ContactSource { MANUAL, FAVORITES }

@Serializable
enum class TapAction { CALL, WHATSAPP_CHAT, WHATSAPP_CALL, SMS, CONTACT_CARD }

@Serializable
enum class SortOrder { CUSTOM, ALPHABETICAL, RECENT }

@Serializable
enum class PhotoSize(val targetCellDp: Int) {
    SMALL(64),
    MEDIUM(80),
    LARGE(106),
}

@Serializable
enum class PhotoShape { ROUNDED, CIRCLE, SQUARE }

/** [nameArgb] is the color of a name written directly on this placeholder. */
@Serializable
enum class PlaceholderStyle(val nameArgb: Long) {
    LIGHT(0xFF15181D),
    DARK(0xFFFFFFFF),
}

@Serializable
enum class WidgetBackground(val argb: Long, val isDark: Boolean) {
    BLUE(0xFF3F7DD1, true),
    DYNAMIC(0x00000000, false),
    TRANSPARENT(0x00000000, true),
    BLACK_TRANSLUCENT(0x99000000, true),
    WHITE_TRANSLUCENT(0xB3FFFFFF, false),
    BLACK(0xFF101114, true),
    WHITE(0xFFFFFFFF, false),
    PINK(0xFFD9548B, true),
}

@Serializable
data class NameOptions(
    val showName: Boolean = true,
    val preferNickname: Boolean = false,
    val fullName: Boolean = true,
    val lastNameFirst: Boolean = false,
    /** Only applies while [showName] is off: with no photo and no name, a tile says nothing. */
    val fullNameWithoutPhoto: Boolean = false,
) {
    val showsFullNameWithoutPhoto: Boolean get() = !showName && fullNameWithoutPhoto
}

@Serializable
enum class NumberPlacement { BELOW, BESIDE }

/** What a folder looks like on the home screen. [CUSTOM] is a picture from the phone's gallery. */
@Serializable
enum class FolderIcon { APP, PEOPLE, HOME, WORK, STAR, HEART, PHONE, CHAT, SCHOOL, HOSPITAL, SHOP, BUILDING, FOLDER, CUSTOM }

@Serializable
enum class ListSpacing(val rowPaddingDp: Int) {
    COMPACT(4),
    NORMAL(10),
    WIDE(18),
}

/** How a names list looks: lines of text, one person each, with no photos. */
@Serializable
data class ListOptions(
    val fontSizeSp: Int = DEFAULT_FONT_SP,
    val boldName: Boolean = false,
    val showNumber: Boolean = false,
    val numberPlacement: NumberPlacement = NumberPlacement.BELOW,
    val showDividers: Boolean = true,
    val spacing: ListSpacing = ListSpacing.NORMAL,
) {
    val numberFontSizeSp: Int get() = fontSizeSp * 3 / 4

    val showsNumberBelow: Boolean get() = showNumber && numberPlacement == NumberPlacement.BELOW

    val showsNumberBeside: Boolean get() = showNumber && numberPlacement == NumberPlacement.BESIDE

    fun withFontSize(sizeSp: Int): ListOptions = copy(fontSizeSp = sizeSp.coerceIn(MIN_FONT_SP, MAX_FONT_SP))

    /** A positive [steps] makes the text larger, a negative one smaller. */
    fun withFontStep(steps: Int): ListOptions = withFontSize(fontSizeSp + steps * FONT_STEP_SP)

    companion object {
        const val MIN_FONT_SP = 12
        const val MAX_FONT_SP = 40
        const val FONT_STEP_SP = 2
        const val DEFAULT_FONT_SP = 18
    }
}

/** A person chosen by hand, with the specific number the user picked for them. */
@Serializable
data class SelectedContact(
    val lookupKey: String,
    val contactId: Long,
    val number: String,
)

@Serializable
data class WidgetConfig(
    val title: String = "",
    val showTitle: Boolean = false,
    val source: ContactSource = ContactSource.MANUAL,
    val contacts: List<SelectedContact> = emptyList(),
    val favoriteOrder: List<String> = emptyList(),
    val tapAction: TapAction = TapAction.CALL,
    /** Offered by a long press in a folder or a double tap on the home screen. */
    val secondaryActions: Set<TapAction> = TapAction.entries.toSet(),
    val sort: SortOrder = SortOrder.CUSTOM,
    val size: PhotoSize = PhotoSize.MEDIUM,
    val shape: PhotoShape = PhotoShape.ROUNDED,
    val placeholder: PlaceholderStyle = PlaceholderStyle.LIGHT,
    val background: WidgetBackground = WidgetBackground.BLUE,
    val name: NameOptions = NameOptions(),
    val list: ListOptions = ListOptions(),
    /** Page arrows and "show all" under a widget whose people do not fit. */
    val showPageBar: Boolean = true,
    val folderIcon: FolderIcon = FolderIcon.APP,
    /** The picture saved for [FolderIcon.CUSTOM], kept when another icon is chosen for now. */
    val customIconFile: String? = null,
) {
    /** The options menu for a person: never the task a quick tap already does. */
    val menuActions: List<TapAction>
        get() = TapAction.entries.filter { it in secondaryActions && it != tapAction }
}

@Serializable
data class AppState(
    val widgets: Map<Int, WidgetConfig> = emptyMap(),
    /** Contact lookup key to the time of the last call started from a widget. */
    val lastCalled: Map<String, Long> = emptyMap(),
)
