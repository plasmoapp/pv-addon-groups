package su.plo.voice.groups.utils.extend

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.entity.player.McPlayer
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import java.util.*

fun McCommandSource.getVoicePlayer(server: PlasmoBaseVoiceServer) = if (this is McPlayer) {
    server.playerManager.getPlayerById(this.uuid).orElse(null)
} else null

fun McCommandSource.sendTranslatable(key: String, vararg args: Any) {
    sendMessage(McTextComponent.translatable(key, *args))
}

fun McCommandSource.checkNotNullAndNoFlagPermission(
    value: Any?,
    flag: String
): Boolean = ((value != null) && !hasFlagPermission(flag))
    .also { if (it) noPermissionError("flag.$flag") }

fun McCommandSource.hasAddonPermission(command: String): Boolean =
    this.hasPermission("pv.addon.groups.*") || this.hasPermission("pv.addon.groups.$command")

fun McCommandSource.hasFlagPermission(flag: String): Boolean =
    this.hasPermission("pv.addon.groups.*") || this.hasPermission("pv.addon.groups.flag.$flag")

fun McCommandSource.parseUuidOrPrintError(string: String): UUID? = string
    .runCatching { UUID.fromString(string) }
    .getOrNull()
    ?: run {
        sendTranslatable("pv.addon.groups.error.uuid_parse", string)
        null
    }

fun McCommandSource.checkAddonPermissionAndPrintError(permission: String): Boolean =
    !hasAddonPermission(permission).also {
        if (!it) sendTranslatable("pv.addon.groups.error.no_permission", permission)
    }

fun McCommandSource.noPermissionError(permission: String) =
    sendTranslatable("pv.addon.groups.error.no_permission", "pv.addon.groups.$permission")

fun McCommandSource.playerOnlyCommandError() =
    sendTranslatable("pv.error.player_only_command")

fun McCommandSource.groupNotFoundError() =
    sendTranslatable("pv.addon.groups.error.group_not_found")

fun McCommandSource.notInGroupError() =
    sendTranslatable("pv.addon.groups.error.not_in_group")

fun McCommandSource.printDivider() =
    sendTranslatable("pv.addon.groups.divider")

fun McCommandSource.sendMessage(components: List<McTextComponent>) =
    components.forEach { sendMessage(it) }

fun McCommandSource.printEmpty() =
    sendMessage(McTextComponent.empty())
