package su.plo.voice.groups.utils.extend

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import java.util.*

fun MinecraftCommandSource.getVoicePlayer(server: PlasmoVoiceServer) = if (this is MinecraftServerPlayer) {
    server.playerManager.getPlayerById(this.uuid).orElse(null)
} else null

fun MinecraftCommandSource.sendTranslatable(key: String, args: Any? = null) {
    sendMessage(MinecraftTextComponent.translatable(key, args))
}

fun MinecraftCommandSource.checkNotNullAndNoPermission(
    value: Any?,
    permission: String
): Boolean = ((value != null) && !hasPermission(permission))
    .also { if (it) noPermissionError(permission) }

fun MinecraftCommandSource.hasPermission(command: String): Boolean =
    this.hasPermission("pv.addon.groups.*") || this.hasPermission("pv.addon.groups.$command")


fun MinecraftCommandSource.parseUuidOrPrintError(string: String): UUID? = string
    ?.runCatching { UUID.fromString(string) }
    ?.getOrNull()
    ?: run {
        sendTranslatable("pv.addon.groups.command.delete.usage")
        null
    }

fun MinecraftCommandSource.checkPermissionAndPrintError(permission: String): Boolean =
    !hasPermission(permission).also {
        if (it) sendTranslatable("pv.addon.groups.error.no_permission", permission)
    }

fun MinecraftCommandSource.noPermissionError(permission: String) =
    sendTranslatable("pv.addon.groups.error.no_permission", permission)

fun MinecraftCommandSource.playerOnlyCommandError() =
    sendTranslatable("pv.error.player_only_command")

fun MinecraftCommandSource.groupNotFoundError() =
    sendTranslatable("pv.addon.groups.error.group_not_found")

fun MinecraftCommandSource.printDivider() =
    sendTranslatable("pv.addon.groups.divider")
