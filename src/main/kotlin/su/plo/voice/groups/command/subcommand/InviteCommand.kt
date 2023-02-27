package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.chat.MinecraftTextComponent
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

        val player = source.getVoicePlayer(handler.voiceServer)

        return handler.voiceServer.minecraftServer.players
            .map { it.name }
            .filter { it.startsWith(arg) && (it != player?.instance?.name) }
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return source.playerOnlyCommandError()

        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return source.notInGroupError()

        val isOwner = group.owner == player

        when {
            source.hasAddonPermission("invite.*") -> Unit
            source.hasAddonPermission("invite.member") -> Unit
            source.hasAddonPermission("invite.owner") && isOwner -> Unit
            !isOwner -> return source.noPermissionError("invite.member")
            else -> return source.noPermissionError("invite.owner")
        }

        val playerName = arguments.getOrNull(1)
            ?: return source.sendTranslatable("pv.addon.groups.command.invite.error.usage")

        if (playerName == player.instance.name)
            return source.sendTranslatable("pv.addon.groups.command.invite.error.invite_self")

        val invitedPlayer = handler.voiceServer.minecraftServer
            .getPlayerByName(playerName)
            .orElse(null) ?: return source.sendTranslatable("pv.addon.groups.error.player_not_found")

        handler.groupManager.groupByPlayer[invitedPlayer.uuid]
            ?.let {
                if (it.id == group.id)
                    return source.sendTranslatable("pv.addon.groups.command.invite.error.already_joined")
            }

        source.sendTranslatable("pv.addon.groups.command.invite.success")

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