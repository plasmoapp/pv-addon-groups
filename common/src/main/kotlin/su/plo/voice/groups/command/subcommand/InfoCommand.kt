package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class InfoCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "info"

    override val permissions = listOf(
        "info.owner" to PermissionDefault.TRUE,
        "info.member" to PermissionDefault.TRUE,
        "info.*" to PermissionDefault.OP,
    )

    override fun suggest(source: McCommandSource, arguments: Array<String>): List<String> = listOf()

    override fun execute(source: McCommandSource, arguments: Array<String>) {

        val player = source.getVoicePlayer(handler.voiceServer) ?: run {
            source.playerOnlyCommandError()
            return
        }

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: run {
            source.notInGroupError()
            return
        }

        val isOwner = group.isOwner(player)

        when {
            source.hasAddonPermission("info.*") -> Unit
            source.hasAddonPermission("info.member") -> Unit
            source.hasAddonPermission("info.owner") && isOwner -> Unit
            !isOwner -> return source.noPermissionError("info.member")
            else -> return source.noPermissionError("info.owner")
        }

        source.printDivider()
        source.sendMessage(group.asTextComponents(handler, source))
        source.printDivider()

    }

    override fun checkCanExecute(source: McCommandSource): Boolean {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return false

        val isOwner = group.isOwner(player)

        return when {
            source.hasAddonPermission("info.member") -> true
            isOwner && source.hasAddonPermission("info.owner") -> true
            source.hasAddonPermission("info.*") -> true
            else -> false
        }
    }

}
