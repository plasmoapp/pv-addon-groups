package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.utils.extend.getVoicePlayer
import su.plo.voice.groups.utils.extend.noPermissionError
import su.plo.voice.groups.utils.extend.notInGroupError
import su.plo.voice.groups.utils.extend.playerOnlyCommandError
import su.plo.voice.groups.utils.extend.sendTranslatable

class BanCommand(handler: CommandHandler): ManagementCommand(handler, "ban") {

    override fun suggest(source: McCommandSource, arguments: Array<String>): List<String> {

        if (arguments.size != 2) return listOf()

        val arg = arguments.getOrNull(1) ?: return listOf()

        val player = source.getVoicePlayer(handler.voiceServer) ?: return emptyList()

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return emptyList()

        return handler.addon.getVisibleOnlinePlayers(player)
            .filter { !group.isBanned(it.instance.uuid) }
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
            source.noPermissionError(if (isOwner) "ban.owner" else "ban.all")
            return
        }

        val playerName = arguments.getOrNull(1) ?: run {
            source.sendTranslatable("pv.addon.groups.command.ban.error.usage")
            return
        }

        val target = handler.addon.getVisibleOnlinePlayers(player)
            .firstOrNull { it.instance.name == playerName }
            ?: run {
                source.sendTranslatable("pv.addon.groups.error.player_not_found")
                return
            }

        if (group.isBanned(target.instance.uuid)) {
            source.sendTranslatable("pv.addon.groups.command.ban.error.already_banned")
            return
        }

        handler.groupManager.ban(group, target)
    }
}
