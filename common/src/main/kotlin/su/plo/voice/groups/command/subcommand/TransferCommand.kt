package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class TransferCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "transfer"

    override val permissions = listOf(
        "transfer.owner" to PermissionDefault.TRUE,
        "transfer.all" to PermissionDefault.OP,
        "transfer.*" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {

        if (arguments.size != 2) return listOf()

        val arg = arguments.getOrNull(1) ?: return listOf()

        val player = source.getVoicePlayer(handler.voiceServer)

        return handler.voiceServer.playerManager.players
            .map { it.instance.name }
            .filter { it.startsWith(arg) && (it != player?.instance?.name) }
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

        val isOwner = group.owner?.id == player.instance.uuid

        when {
            source.hasAddonPermission("transfer.all") -> Unit
            source.hasAddonPermission("transfer.*") -> Unit
            source.hasAddonPermission("transfer.owner") && isOwner -> Unit
            else -> {
                source.noPermissionError(if (isOwner) "transfer.owner" else "transfer.all")
                return
            }
        }

        val playerName = arguments.getOrNull(1) ?: run {
            source.sendTranslatable("pv.addon.groups.command.transfer.error.usage")
            return
        }

        // todo: offline transfer?
//        val profile = handler.voiceServer.minecraftServer
//            .getGameProfile(playerName)
//            .orElse(null) ?: run {
//                source.sendTranslatable("pv.addon.groups.error.player_not_found")
//                return
//            }
        val newOwner = handler.voiceServer.playerManager
            .getPlayerByName(playerName)
                    .orElse(null) ?: run {
                source.sendTranslatable("pv.addon.groups.error.player_not_found")
                return
            }

        if (newOwner.instance.name == player.instance.name) {
            source.sendTranslatable("pv.addon.groups.command.transfer.error.already_owner")
            return
        }

        group.owner = newOwner.instance.gameProfile
        group.owner?.let { group.notifyPlayersTranslatable("server.pv.addon.groups.notifications.new_owner", it.name) }

//        source.sendTranslatable("pv.addon.groups.command.leave.success", group.name)
    }

    override fun checkCanExecute(source: MinecraftCommandSource): Boolean {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return false

        val isOwner = group.owner?.id == player.instance.uuid

        return when {
            source.hasAddonPermission("transfer.owner") && isOwner -> true
            source.hasAddonPermission("transfer.all") -> true
            source.hasAddonPermission("transfer.*") -> true
            else -> false
        }
    }
}
