package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.util.extend.getVoicePlayer
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.noPermissionError
import su.plo.voice.groups.util.extend.notInGroupError
import su.plo.voice.groups.util.extend.playerOnlyCommandError

class DeleteCommand(
    handler: CommandHandler,
) : SubCommand(handler) {
    override val name = "delete"

    override val permissions =
        listOf(
            "delete.owner" to PermissionDefault.TRUE,
            "delete.all" to PermissionDefault.OP,
            "delete.*" to PermissionDefault.OP,
        )

    override fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> = listOf()

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
            source.hasAddonPermission("delete.all") || source.hasAddonPermission("delete.*") -> Unit

            source.hasAddonPermission("delete.owner") && isOwner -> Unit

            else -> return source.noPermissionError(
                if (isOwner) "delete.owner" else "delete.all",
            )
        }

        handler.groupManager.deleteGroup(group)
    }

    override fun checkCanExecute(source: McCommandSource): Boolean {
        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.getPlayerGroup(player.instance.uuid) ?: return false

        val isOwner = group.isOwner(player)

        return when {
            source.hasAddonPermission("delete.owner") && isOwner -> true
            source.hasAddonPermission("delete.all") -> true
            source.hasAddonPermission("delete.*") -> true
            else -> false
        }
    }
}
