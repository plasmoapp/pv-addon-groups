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
import su.plo.voice.groups.utils.extend.sendTranslatable

class TransferCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "transfer"

    override val permissions = listOf(
        "transfer.owner" to PermissionDefault.TRUE,
        "transfer.all" to PermissionDefault.OP,
        "transfer.*" to PermissionDefault.OP,
    )

    override fun suggest(source: McCommandSource, arguments: Array<String>): List<String> {

        if (arguments.size != 2) return listOf()

        val arg = arguments.getOrNull(1) ?: return listOf()

        val player = source.getVoicePlayer(handler.voiceServer)

        return handler.addon.getVisibleOnlinePlayers(player)
            .map { it.instance.name }
            .filter { it.startsWith(arg) && (it != player?.instance?.name) }
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
        val newOwner = handler.addon.getVisibleOnlinePlayers(player)
            .firstOrNull { it.instance.name == playerName }
            ?: run {
                source.sendTranslatable("pv.addon.groups.error.player_not_found")
                return
            }

        if (newOwner.instance.name == player.instance.name) {
            source.sendTranslatable("pv.addon.groups.command.transfer.error.already_owner")
            return
        }

        group.owner = newOwner.instance.gameProfile
        group.owner?.let { group.notifyPlayersTranslatable("pv.addon.groups.notifications.new_owner", it.name) }

//        source.sendTranslatable("pv.addon.groups.command.leave.success", group.name)
    }

    override fun checkCanExecute(source: McCommandSource): Boolean {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return false

        val isOwner = group.isOwner(player)

        return when {
            source.hasAddonPermission("transfer.owner") && isOwner -> true
            source.hasAddonPermission("transfer.all") -> true
            source.hasAddonPermission("transfer.*") -> true
            else -> false
        }
    }
}
