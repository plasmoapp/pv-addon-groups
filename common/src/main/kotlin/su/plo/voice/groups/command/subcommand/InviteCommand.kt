package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class InviteCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "invite"

    override val permissions = listOf(
        "invite.owner" to PermissionDefault.TRUE,
        "invite.member" to PermissionDefault.TRUE,
        "invite.*" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {

        if (arguments.size != 2) return listOf()

        val arg = arguments.getOrNull(1) ?: return listOf()

        val player = source.getVoicePlayer(handler.voiceServer) ?: return listOf()

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return listOf()

        return handler.voiceServer.playerManager.players
            .filter { !group.players.contains(it) && (it != player) }
            .map { it.instance.name }
            .filter { it.startsWith(arg, true) }
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        val player = source.getVoicePlayer(handler.voiceServer) ?: run {
            source.playerOnlyCommandError()
            return
        }

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: run {
            source.notInGroupError()
            return
        }

        val isOwner = group.owner == player

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

        val invitedPlayer = handler.voiceServer
            .playerManager
            .getPlayerByName(playerName)
            .orElse(null)?.instance ?: run {
                source.sendTranslatable("pv.addon.groups.error.player_not_found")
                return
            }

        handler.groupManager.groupByPlayer[invitedPlayer.uuid]
            ?.also { if (it.id == group.id) {
                source.sendTranslatable("pv.addon.groups.command.invite.error.already_joined")
                return
            }}

        source.sendTranslatable("pv.addon.groups.command.invite.success", invitedPlayer.name)

        invitedPlayer.printDivider()
        invitedPlayer.sendTranslatable("pv.addon.groups.format.invite", player.instance.name, group.inlineChatComponent())
        invitedPlayer.sendMessage(group.joinButtonWithPassword())
        invitedPlayer.printDivider()
    }

    override fun checkCanExecute(source: MinecraftCommandSource): Boolean {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return false

        val isOwner = group.owner?.id == player.instance.uuid

        return when {
            source.hasAddonPermission("invite.owner") && isOwner -> true
            source.hasAddonPermission("invite.member") -> true
            source.hasAddonPermission("invite.*") -> true
            else -> false
        }
    }
}
