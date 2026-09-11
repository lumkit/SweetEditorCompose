package io.github.lumkit.sweeteditor.contextmenu

import io.github.lumkit.sweeteditor.ContextMenuItem
import io.github.lumkit.sweeteditor.ContextMenuItemProvider
import io.github.lumkit.sweeteditor.ContextMenuRequest
import io.github.lumkit.sweeteditor.ContextMenuSection
import io.github.lumkit.sweeteditor.HitTarget
import io.github.lumkit.sweeteditor.HitTargetType
import io.github.lumkit.sweeteditor.core.protocol.GestureType
import io.github.lumkit.sweeteditor.core.protocol.HitTarget as CoreHitTarget
import io.github.lumkit.sweeteditor.core.protocol.HitTargetType as CoreHitTargetType

internal enum class ContextMenuSignal {
    SHOW,
    DISMISS,
    IGNORE,
}

internal fun contextMenuSignal(enabled: Boolean, gestureType: GestureType): ContextMenuSignal {
    if (!enabled) return ContextMenuSignal.IGNORE
    return when (gestureType) {
        GestureType.CONTEXT_MENU -> ContextMenuSignal.SHOW
        GestureType.TAP,
        GestureType.DOUBLE_TAP,
        GestureType.SCROLL,
        GestureType.FAST_SCROLL,
        GestureType.SCALE,
        GestureType.DRAG_SELECT,
        -> ContextMenuSignal.DISMISS
        else -> ContextMenuSignal.IGNORE
    }
}

internal fun buildContextMenuSections(
    provider: ContextMenuItemProvider?,
    request: ContextMenuRequest,
): List<ContextMenuSection> {
    if (provider != null) {
        return sanitizeContextMenuSections(provider.provideMenuItems(request))
    }
    return defaultContextMenuSections(request)
}

internal fun sanitizeContextMenuSections(sections: List<ContextMenuSection>?): List<ContextMenuSection> {
    if (sections.isNullOrEmpty()) return emptyList()
    return sections.mapNotNull { section ->
        if (section.items.isEmpty()) null else ContextMenuSection(section.items)
    }
}

internal fun defaultContextMenuSections(request: ContextMenuRequest): List<ContextMenuSection> {
    val sections = ArrayList<ContextMenuSection>(3)
    if (request.hitTarget.type == HitTargetType.LINK && request.linkTarget.isNotEmpty()) {
        sections += ContextMenuSection(
            listOf(
                ContextMenuItem(ContextMenuItem.ACTION_OPEN_LINK, "Open Link"),
                ContextMenuItem(ContextMenuItem.ACTION_COPY_LINK, "Copy Link"),
            ),
        )
    }
    if (request.hasSelection) {
        sections += ContextMenuSection(
            listOf(
                ContextMenuItem(ContextMenuItem.ACTION_CUT, "Cut"),
                ContextMenuItem(ContextMenuItem.ACTION_COPY, "Copy"),
            ),
        )
    }
    sections += ContextMenuSection(
        listOf(
            ContextMenuItem(ContextMenuItem.ACTION_PASTE, "Paste"),
            ContextMenuItem(ContextMenuItem.ACTION_SELECT_ALL, "Select All"),
        ),
    )
    return sections
}

internal fun CoreHitTarget.toPublicHitTarget(): HitTarget = HitTarget(
    type = type.toPublic(),
    line = line,
    column = column,
    iconId = iconId,
    colorValue = colorValue,
)

private fun CoreHitTargetType.toPublic(): HitTargetType = when (this) {
    CoreHitTargetType.NONE -> HitTargetType.NONE
    CoreHitTargetType.INLAY_HINT_TEXT -> HitTargetType.INLAY_HINT_TEXT
    CoreHitTargetType.INLAY_HINT_ICON -> HitTargetType.INLAY_HINT_ICON
    CoreHitTargetType.INLAY_HINT_COLOR -> HitTargetType.INLAY_HINT_COLOR
    CoreHitTargetType.CODELENS -> HitTargetType.CODELENS
    CoreHitTargetType.LINK -> HitTargetType.LINK
    CoreHitTargetType.GUTTER_ICON -> HitTargetType.GUTTER_ICON
    CoreHitTargetType.FOLD_GUTTER -> HitTargetType.FOLD_GUTTER
    CoreHitTargetType.FOLD_PLACEHOLDER -> HitTargetType.FOLD_PLACEHOLDER
}
