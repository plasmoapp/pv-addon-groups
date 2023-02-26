package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.group.Group
import su.plo.voice.groups.utils.extend.checkPermissionAndPrintError
import su.plo.voice.groups.utils.extend.getVoicePlayer
import su.plo.voice.groups.utils.extend.playerOnlyCommandError
import su.plo.voice.groups.utils.extend.sendTranslatable
import java.util.*

class JoinCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "join"

    override val permissions = listOf(
        "join" to PermissionDefault.TRUE,
        "join.all" to PermissionDefault.OP,
        "join.*" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {
        if (arguments.size != 2) return listOf()



        return listOf()
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        if (source.checkPermissionAndPrintError("join")) return

        val player = source.getVoicePlayer(handler.voiceServer) ?: return source.playerOnlyCommandError()



//        val group = handler.groupManager.groups[player.instance.uuid] ?: return handler.groupNotFoundError(source)

//        handler.groupManager.leave(player)
//
//        source.sendTranslatable("pv.addon.groups.command.leave.success", group.name)
//
//        group.notifyPlayersTranslatable("pv.addon.groups.notifications.player_left", player.instance.name)
    }
}