package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.getVoicePlayer
import su.plo.voice.groups.utils.extend.hasAddonPermission
import su.plo.voice.groups.utils.extend.noPermissionError
import su.plo.voice.groups.utils.extend.notInGroupError
import su.plo.voice.groups.utils.extend.playerOnlyCommandError
import su.plo.voice.groups.utils.extend.printDivider
import su.plo.voice.groups.utils.extend.sendTranslatable

class InviteCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "invite"

    override val permissions = listOf(
        "invite.owner" to PermissionDefault.TRUE,
        "invite.member" to PermissionDefault.TRUE,
        "invite.*" to PermissionDefault.OP,
    )

    override fun suggest(source: McCommandSource, arguments: Array<String>): List<String> {

        if (arguments.size != 2) return listOf()

        val arg = arguments.getOrNull(1) ?: return listOf()

        val player = source.getVoicePlayer(handler.voiceServer) ?: return listOf()

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return listOf()

        return handler.addon.getVisibleOnlinePlayers(player)
            .filter { !group.onlinePlayers.contains(it) && (it != player) }
            .map { it.instance.name }
            .filter { it.startsWith(arg, true) }
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

        when {
            source.hasAddonPermission("invite.*") -> Unit
            source.hasAddonPermission("invite.member") -> Unit
            source.hasAddonPermission("invite.owner") && isOwner -> Unit
            !isOwner -> return source.noPermissionError("invite.member")
            else -> return source.noPermissionError("invite.owner")
        }

        val playerName = arguments.getOrNull(1) ?: run {
            source.sendTranslatable("pv.addon.groups.command.invite.error.usage")
            return
        }

        if (playerName == player.instance.name) {
            source.sendTranslatable("pv.addon.groups.command.invite.error.invite_self")
            return
        }

        val invitedPlayer = handler.addon.getVisibleOnlinePlayers(player)
            .firstOrNull { it.instance.name == playerName }
            ?.instance
            ?: run {
                source.sendTranslatable("pv.addon.groups.error.player_not_found")
                return
            }

        handler.groupManager.groupByPlayer[invitedPlayer.uuid]
            ?.also { if (it.id == group.id) {
                source.sendTranslatable("pv.addon.groups.command.invite.error.already_joined")
                return
            }}

        if (group.isBanned(invitedPlayer.uuid)) {
            source.sendTranslatable("pv.addon.groups.command.invite.error.banned")
            return
        }

        source.sendTranslatable("pv.addon.groups.command.invite.success", invitedPlayer.name)

        invitedPlayer.printDivider()
        invitedPlayer.sendTranslatable("pv.addon.groups.format.invite", player.instance.name, group.inlineChatComponent())
        invitedPlayer.sendMessage(group.joinButtonWithPassword())
        invitedPlayer.printDivider()
    }

    override fun checkCanExecute(source: McCommandSource): Boolean {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return false

        val isOwner = group.isOwner(player)

        return when {
            source.hasAddonPermission("invite.owner") && isOwner -> true
            source.hasAddonPermission("invite.member") -> true
            source.hasAddonPermission("invite.*") -> true
            else -> false
        }
    }
}
