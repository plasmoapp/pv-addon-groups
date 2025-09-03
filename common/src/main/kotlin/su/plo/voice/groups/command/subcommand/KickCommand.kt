package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.utils.extend.getVoicePlayer
import su.plo.voice.groups.utils.extend.noPermissionError
import su.plo.voice.groups.utils.extend.notInGroupError
import su.plo.voice.groups.utils.extend.playerOnlyCommandError
import su.plo.voice.groups.utils.extend.sendTranslatable

class KickCommand(handler: CommandHandler): ManagementCommand(handler, "kick") {

    override fun suggest(source: McCommandSource, arguments: Array<String>): List<String> {

        if (arguments.size != 2) return listOf()

        val arg = arguments.getOrNull(1) ?: return listOf()

        val player = source.getVoicePlayer(handler.voiceServer) ?: return emptyList()

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return emptyList()

        return group.onlinePlayers
            .filter { it != player }
            .map { it.instance.name }
            .filter { it.startsWith(arg) }
    }

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

        if (!checkCanExecute(source)) {
            source.noPermissionError(if (isOwner) "kick.owner" else "kick.all")
            return
        }

        val playerName = arguments.getOrNull(1) ?: run {
            source.sendTranslatable("pv.addon.groups.command.kick.error.usage")
            return
        }

        val target = handler.voiceServer.playerManager
            .getPlayerByName(playerName)
                    .orElse(null) ?: run {
                source.sendTranslatable("pv.addon.groups.error.player_not_found")
                return
            }

        if (!group.playersIds.contains(target.instance.uuid)) {
            source.sendTranslatable("pv.addon.groups.command.kick.error.not_in_group")
            return
        }

        handler.groupManager.kick(group, target)
    }
}
