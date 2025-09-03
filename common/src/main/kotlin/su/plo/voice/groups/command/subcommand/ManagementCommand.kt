package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.getVoicePlayer
import su.plo.voice.groups.utils.extend.hasAddonPermission

abstract class ManagementCommand(
    handler: CommandHandler,
    final override val name: String,
    private val permissionPrefix: String = name
) : SubCommand(handler) {

    final override val permissions = listOf(
        "$permissionPrefix.owner" to PermissionDefault.TRUE,
        "$permissionPrefix.all" to PermissionDefault.OP,
        "$permissionPrefix.*" to PermissionDefault.OP,
    )

    final override fun checkCanExecute(source: McCommandSource): Boolean {
        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return false

        val isOwner = group.isOwner(player)

        return when {
            source.hasAddonPermission("$permissionPrefix.owner") && isOwner -> true
            source.hasAddonPermission("$permissionPrefix.all") -> true
            source.hasAddonPermission("$permissionPrefix.*") -> true
            else -> false
        }
    }
}