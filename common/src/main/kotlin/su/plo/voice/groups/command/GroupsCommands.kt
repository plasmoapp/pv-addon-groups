package su.plo.voice.groups.command

import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import dev.apehum.mcdsl.command.literalCommand
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.slib.api.entity.player.McPlayer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.api.Group
import su.plo.voice.groups.command.subcommand.banCommand
import su.plo.voice.groups.command.subcommand.browse
import su.plo.voice.groups.command.subcommand.browseCommand
import su.plo.voice.groups.command.subcommand.createCommand
import su.plo.voice.groups.command.subcommand.deleteCommand
import su.plo.voice.groups.command.subcommand.infoCommand
import su.plo.voice.groups.command.subcommand.inviteCommand
import su.plo.voice.groups.command.subcommand.joinCommand
import su.plo.voice.groups.command.subcommand.kickCommand
import su.plo.voice.groups.command.subcommand.leaveCommand
import su.plo.voice.groups.command.subcommand.setCommand
import su.plo.voice.groups.command.subcommand.transferCommand
import su.plo.voice.groups.command.subcommand.unbanCommand
import su.plo.voice.groups.command.subcommand.unsetCommand
import su.plo.voice.groups.util.PlayerCollector
import su.plo.voice.groups.util.extend.getVoicePlayer
import su.plo.voice.groups.util.extend.hasTierPermission
import su.plo.voice.groups.util.extend.noPermissionError
import su.plo.voice.groups.util.extend.notInGroupError
import su.plo.voice.groups.util.extend.playerOnlyCommandError

class GroupsCommands(
    private val addon: GroupsAddon,
    val playerCollector: PlayerCollector,
    val targets: TargetArgument,
) {
    val voiceServer
        get() = addon.voiceServer

    val groupManager
        get() = addon.groupManager

    fun build(): LiteralCommandNode<McBrigadierSource> =
        literalCommand("groups") {
            requires(anyPermission(*GroupsPermissions.commandPermissions.toTypedArray()))

            executes { browse(source, 1) }

            literalCommand(browseCommand())
            literalCommand(createCommand())
            literalCommand(joinCommand())
            literalCommand(leaveCommand())
            literalCommand(inviteCommand())
            literalCommand(infoCommand())
            literalCommand(setCommand())
            literalCommand(unsetCommand())
            literalCommand(deleteCommand())
            literalCommand(transferCommand())
            literalCommand(kickCommand())
            literalCommand(banCommand())
            literalCommand(unbanCommand())
        }

    fun CommandContext<McBrigadierSource>.requirePlayer(): VoicePlayer? =
        sender.getVoicePlayer(voiceServer) ?: run {
            source.playerOnlyCommandError()
            null
        }

    fun CommandContext<McBrigadierSource>.requireExecutor(): VoicePlayer? =
        ((source.executor as? McPlayer) ?: (sender as? McPlayer))
            ?.let { voiceServer.playerManager.getPlayerById(it.uuid).orElse(null) }
            ?: run {
                source.playerOnlyCommandError()
                null
            }

    fun CommandContext<McBrigadierSource>.requireGroup(
        permission: String,
        othersTier: String = "all",
    ): GroupMember? {
        val player = requirePlayer() ?: return null
        val group =
            groupManager.getPlayerGroup(player.instance.uuid) ?: run {
                source.notInGroupError()
                return null
            }

        val isOwner = group.isOwner(player)
        if (!sender.hasTierPermission(permission, isOwner, othersTier)) {
            source.noPermissionError(if (isOwner) "$permission.owner" else "$permission.$othersTier")
            return null
        }

        return GroupMember(player, group)
    }

    fun senderGroup(source: McCommandSource): GroupMember? {
        val player = source.getVoicePlayer(voiceServer) ?: return null
        val group = groupManager.getPlayerGroup(player.instance.uuid) ?: return null

        return GroupMember(player, group)
    }

    data class GroupMember(
        val player: VoicePlayer,
        val group: Group,
    )
}

val CommandContext<McBrigadierSource>.sender: McCommandSource
    get() = source.source
