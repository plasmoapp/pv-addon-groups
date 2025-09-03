package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class DeleteCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "delete"

    override val permissions = listOf(
        "delete.owner" to PermissionDefault.TRUE,
        "delete.all" to PermissionDefault.OP,
        "delete.*" to PermissionDefault.OP,
    )

    override fun suggest(source: McCommandSource, arguments: Array<String>): List<String> {

//        if (arguments.size != 2) return listOf("")
//
//        val uuidArgument = arguments[1]
//
//        if (handler.hasPermission(source, arrayOf("delete.all", "delete.*")))
//            return handler.groupManager.groups.values
//                .map { it.id.toString() }
//                .filter { it.startsWith(uuidArgument) }
//
//        val player = if (source is MinecraftServerPlayer) {
//            handler.voiceServer.playerManager.getPlayerById(source.uuid).orElse(null)
//        } else null
//
//        return handler.groupManager.groups.values
//            .filter { it.owner == player }
//            .map { it.id.toString() }
//            .filter { it.startsWith(uuidArgument) }

        return listOf()
    }

    override fun execute(source: McCommandSource, arguments: Array<String>) {

//        val groupUuid = arguments.getOrNull(1)
//            ?.runCatching { UUID.fromString(this) }
//            ?.getOrNull()
//            ?: run {
//                source.sendMessage(McTextComponent.translatable("pv.addon.groups.command.delete.usage"))
//                return
//            }

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
            source.hasAddonPermission("delete.all") || source.hasAddonPermission("delete.*") -> Unit
            source.hasAddonPermission("delete.owner") && isOwner -> Unit
            else -> return source.noPermissionError(
                if (isOwner) "delete.owner" else "delete.all"
            )
        }

        group.notifyPlayersTranslatable("pv.addon.groups.notifications.group_deleted")

        handler.groupManager.deleteGroup(group)

//        source.sendTranslatable("pv.addon.groups.command.delete.success")
    }

    override fun checkCanExecute(source: McCommandSource): Boolean {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return false

        val isOwner = group.isOwner(player)

        return when {
            source.hasAddonPermission("delete.owner") && isOwner -> true
            source.hasAddonPermission("delete.all") -> true
            source.hasAddonPermission("delete.*") -> true
            else -> false
        }
    }

}
