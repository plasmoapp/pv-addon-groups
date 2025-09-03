package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class LeaveCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "leave"

    override val permissions = listOf(
        "leave" to PermissionDefault.TRUE,
    )

    override fun suggest(source: McCommandSource, arguments: Array<String>): List<String> = listOf()

    override fun execute(source: McCommandSource, arguments: Array<String>) {

        if (source.checkAddonPermissionAndPrintError("leave")) return

        val player = source.getVoicePlayer(handler.voiceServer) ?: run {
            source.playerOnlyCommandError()
            return
        }

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: run {
            source.notInGroupError()
            return
        }

        handler.groupManager.leave(player)

        source.sendTranslatable("pv.addon.groups.command.leave.success", group.name)

        group.notifyPlayersTranslatable("pv.addon.groups.notifications.player_left", player.instance.name)
    }

    override fun checkCanExecute(source: McCommandSource): Boolean =
        source.getVoicePlayer(handler.voiceServer)
            ?.let { handler.groupManager.groupByPlayer[it.instance.uuid] }
            ?.let { true } ?: false
}
