package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*
import java.util.*

class DeleteCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "delete"

    override val permissions = listOf(
        "delete" to PermissionDefault.TRUE,
        "delete.owner" to PermissionDefault.TRUE,
        "delete.all" to PermissionDefault.OP,
        "delete.*" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {

        if (arguments.size != 2) return listOf("")

        val uuidArgument = arguments[1]

        if (handler.hasPermission(source, arrayOf("delete.all", "delete.*")))
            return handler.groupManager.groups.values
                .map { it.id.toString() }
                .filter { it.startsWith(uuidArgument) }

        val player = if (source is MinecraftServerPlayer) {
            handler.voiceServer.playerManager.getPlayerById(source.uuid).orElse(null)
        } else null

        return handler.groupManager.groups.values
            .filter { it.owner == player }
            .map { it.id.toString() }
            .filter { it.startsWith(uuidArgument) }
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        val groupUuid = arguments.getOrNull(1)
            ?.runCatching { UUID.fromString(this) }
            ?.getOrNull()
            ?: run {
                source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.command.delete.usage"))
                return
            }

        val group = handler.groupManager.groups[groupUuid] ?: return source.groupNotFoundError()

        val player = source.getVoicePlayer(handler.voiceServer)

        val isOwner = group.owner?.id == player?.instance?.uuid

        when {
            source.hasAddonPermission("delete.all") || source.hasAddonPermission("delete.*") -> Unit
            source.hasAddonPermission("delete.owner") && isOwner -> Unit
            else -> return source.noPermissionError(
                if (isOwner) "delete.owner" else "delete.all"
            )
        }

        group.notifyPlayersTranslatable("pv.addon.groups.notifications.group_deleted")

        handler.groupManager.deleteGroup(group)

        source.sendTranslatable("pv.addon.groups.command.delete.success")
    }
}