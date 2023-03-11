package su.plo.voice.groups.utils.extend

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import java.util.*

fun MinecraftCommandSource.getVoicePlayer(server: PlasmoBaseVoiceServer) = if (this is MinecraftServerPlayer) {
    server.playerManager.getPlayerById(this.uuid).orElse(null)
} else null

fun MinecraftCommandSource.sendTranslatable(key: String, vararg args: Any?) {
    sendMessage(MinecraftTextComponent.translatable(key, *args))
}

fun MinecraftCommandSource.checkNotNullAndNoFlagPermission(
    value: Any?,
    flag: String
): Boolean = ((value != null) && !hasFlagPermission(flag))
    .also { if (it) noPermissionError("flag.$flag") }

fun MinecraftCommandSource.hasAddonPermission(command: String): Boolean =
    this.hasPermission("pv.addon.groups.*") || this.hasPermission("pv.addon.groups.$command")

fun MinecraftCommandSource.hasFlagPermission(flag: String): Boolean =
    this.hasPermission("pv.addon.groups.*") || this.hasPermission("pv.addon.groups.flag.$flag")

fun MinecraftCommandSource.parseUuidOrPrintError(string: String): UUID? = string
    .runCatching { UUID.fromString(string) }
    .getOrNull()
    ?: run {
        sendTranslatable("pv.addon.groups.error.uuid_parse", string)
        null
    }

fun MinecraftCommandSource.checkAddonPermissionAndPrintError(permission: String): Boolean =
    !hasAddonPermission(permission).also {
        if (!it) sendTranslatable("pv.addon.groups.error.no_permission", permission)
    }

fun MinecraftCommandSource.noPermissionError(permission: String) =
    sendTranslatable("pv.addon.groups.error.no_permission", "pv.addon.groups.$permission")

fun MinecraftCommandSource.playerOnlyCommandError() =
    sendTranslatable("pv.error.player_only_command")

fun MinecraftCommandSource.groupNotFoundError() =
    sendTranslatable("pv.addon.groups.error.group_not_found")

fun MinecraftCommandSource.notInGroupError() =
    sendTranslatable("pv.addon.groups.error.not_in_group")

fun MinecraftCommandSource.printDivider() =
    sendTranslatable("pv.addon.groups.divider")

fun MinecraftCommandSource.sendMessage(components: List<MinecraftTextComponent>) =
    components.forEach { sendMessage(it) }

fun MinecraftCommandSource.printEmpty() =
    sendMessage(MinecraftTextComponent.empty())
