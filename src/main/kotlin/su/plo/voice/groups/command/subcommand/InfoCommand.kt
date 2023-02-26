package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class InfoCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "info"

    override val permissions = listOf(
        "info" to PermissionDefault.TRUE,
        "info.owner" to PermissionDefault.TRUE,
        "info.member" to PermissionDefault.TRUE,
        "info.*" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> = listOf()

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return source.playerOnlyCommandError()

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return source.notInGroupError()

        val isOwner = group.owner == player

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
}