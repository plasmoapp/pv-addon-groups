package su.plo.voice.groups.util.extend

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.entity.player.McPlayer
import su.plo.voice.api.server.PlasmoBaseVoiceServer

fun McCommandSource.getVoicePlayer(server: PlasmoBaseVoiceServer) =
    if (this is McPlayer) {
        server.playerManager.getPlayerById(this.uuid).orElse(null)
    } else {
        null
    }

fun McCommandSource.sendTranslatable(
    key: String,
    vararg args: Any,
) {
    sendMessage(McTextComponent.translatable(key, *args))
}

fun McCommandSource.hasAddonPermission(command: String): Boolean =
    this.hasPermission("pv.addon.groups.*") || this.hasPermission("pv.addon.groups.$command")

fun McCommandSource.hasFlagPermission(flag: String): Boolean =
    this.hasPermission("pv.addon.groups.*") || this.hasPermission("pv.addon.groups.flag.$flag")

fun McCommandSource.printDivider() = sendTranslatable("pv.addon.groups.divider")
