package su.plo.voice.groups.util.extend

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.brigadier.McBrigadierSource
import java.util.UUID

fun McBrigadierSource.sendTranslatable(
    key: String,
    vararg args: Any,
) {
    sendFeedback(McTextComponent.translatable(key, *args))
}

fun McBrigadierSource.sendFeedback(components: List<McTextComponent>) = components.forEach { sendFeedback(it) }

fun McBrigadierSource.printDivider() = sendTranslatable("pv.addon.groups.divider")

fun McBrigadierSource.checkNotNullAndNoFlagPermission(
    value: Any?,
    flag: String,
): Boolean =
    ((value != null) && !source.hasFlagPermission(flag))
        .also { if (it) noPermissionError("flag.$flag") }

fun McBrigadierSource.checkAddonPermissionAndPrintError(permission: String): Boolean =
    !source.hasAddonPermission(permission).also {
        if (!it) sendTranslatable("pv.addon.groups.error.no_permission", permission)
    }

fun McBrigadierSource.parseUuidOrPrintError(string: String): UUID? =
    string
        .runCatching { UUID.fromString(string) }
        .getOrNull()
        ?: run {
            sendTranslatable("pv.addon.groups.error.uuid_parse", string)
            null
        }

fun McBrigadierSource.noPermissionError(permission: String) =
    sendTranslatable("pv.addon.groups.error.no_permission", "pv.addon.groups.$permission")

fun McBrigadierSource.playerOnlyCommandError() = sendTranslatable("pv.error.player_only_command")

fun McBrigadierSource.playerNotFoundError() = sendTranslatable("pv.addon.groups.error.player_not_found")

fun McBrigadierSource.groupNotFoundError() = sendTranslatable("pv.addon.groups.error.group_not_found")

fun McBrigadierSource.notInGroupError() = sendTranslatable("pv.addon.groups.error.not_in_group")
