package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class LeaveCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "leave"

    override val permissions = listOf(
        "leave" to PermissionDefault.TRUE,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> = listOf()

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        if (source.checkAddonPermissionAndPrintError("leave")) return

        val player = source.getVoicePlayer(handler.voiceServer) ?: return source.playerOnlyCommandError()

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return source.notInGroupError()

        handler.groupManager.leave(player)

        source.sendTranslatable("pv.addon.groups.command.leave.success", group.name)

        group.notifyPlayersTranslatable("pv.addon.groups.notifications.player_left", player.instance.name)
    }
}