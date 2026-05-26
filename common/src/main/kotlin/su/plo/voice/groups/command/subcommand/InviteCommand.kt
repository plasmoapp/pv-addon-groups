package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.api.result.InviteResult
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.util.extend.getVoicePlayer
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.noPermissionError
import su.plo.voice.groups.util.extend.notInGroupError
import su.plo.voice.groups.util.extend.playerOnlyCommandError
import su.plo.voice.groups.util.extend.sendTranslatable

class InviteCommand(
    handler: CommandHandler,
) : SubCommand(handler) {
    override val name = "invite"

    override val permissions =
        listOf(
            "invite.owner" to PermissionDefault.TRUE,
            "invite.member" to PermissionDefault.TRUE,
            "invite.*" to PermissionDefault.OP,
        )

    override fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> {
        if (arguments.size != 2) return listOf()

        val arg = arguments.getOrNull(1) ?: return listOf()

        val player = source.getVoicePlayer(handler.voiceServer) ?: return listOf()

        val group = handler.groupManager.getPlayerGroup(player.instance.uuid) ?: return listOf()

        return handler.playerCollector
            .getVisibleOnlinePlayers(player)
            .filter { !group.onlinePlayers.contains(it) && (it != player) }
            .map { it.instance.name }
            .filter { it.startsWith(arg, true) }
    }

    override fun execute(
        source: McCommandSource,
        arguments: Array<String>,
    ) {
        val player =
            source.getVoicePlayer(handler.voiceServer) ?: run {
                source.playerOnlyCommandError()
                return
            }

        val group =
            handler.groupManager.getPlayerGroup(player.instance.uuid) ?: run {
                source.notInGroupError()
                return
            }

        val isOwner = group.isOwner(player)

        when {
            source.hasAddonPermission("invite.*") -> Unit
            source.hasAddonPermission("invite.member") -> Unit
            source.hasAddonPermission("invite.owner") && isOwner -> Unit
            !isOwner -> return source.noPermissionError("invite.member")
            else -> return source.noPermissionError("invite.owner")
        }

        val playerName =
            arguments.getOrNull(1) ?: run {
                source.sendTranslatable("pv.addon.groups.command.invite.error.usage")
                return
            }

        val target =
            handler.playerCollector
                .getVisibleOnlinePlayers(player)
                .firstOrNull { it.instance.name == playerName }
                ?: run {
                    source.sendTranslatable("pv.addon.groups.error.player_not_found")
                    return
                }

        when (group.invite(player, target)) {
            InviteResult.Success -> source.sendTranslatable("pv.addon.groups.command.invite.success", target.instance.name)
            InviteResult.Self -> source.sendTranslatable("pv.addon.groups.command.invite.error.invite_self")
            InviteResult.AlreadyInGroup -> source.sendTranslatable("pv.addon.groups.command.invite.error.already_joined")
            InviteResult.Banned -> source.sendTranslatable("pv.addon.groups.command.invite.error.banned")
        }
    }

    override fun checkCanExecute(source: McCommandSource): Boolean {
        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.getPlayerGroup(player.instance.uuid) ?: return false

        val isOwner = group.isOwner(player)

        return when {
            source.hasAddonPermission("invite.owner") && isOwner -> true
            source.hasAddonPermission("invite.member") -> true
            source.hasAddonPermission("invite.*") -> true
            else -> false
        }
    }
}
