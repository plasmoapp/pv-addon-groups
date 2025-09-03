package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class UnsetCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "unset"

    override val permissions = listOf(
        "unset.owner" to PermissionDefault.TRUE,
        "unset.*" to PermissionDefault.OP,
        "unset.all" to PermissionDefault.OP,
    )

    private val unsetFlags = listOf(
        "password",
        "permissions",
    )

    override fun suggest(source: McCommandSource, arguments: Array<String>): List<String> {

        val flagName = arguments.getOrNull(1) ?: return listOf()

        if (arguments.size == 2) {
            return unsetFlags.filter { it.startsWith(flagName) && source.hasFlagPermission(it) }
        }

        return listOf()
    }

    override fun execute(source: McCommandSource, arguments: Array<String>) {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return source.playerOnlyCommandError()
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return source.notInGroupError()
        val isOwner = group.isOwner(player)

        when {
            source.hasAddonPermission("unset.all") -> Unit
            source.hasAddonPermission("unset.*") -> Unit
            source.hasAddonPermission("unset.owner") && isOwner -> Unit
            else -> {
                source.noPermissionError(if (isOwner) "unset.owner" else "unset.all")
                return
            }
        }

        val flagName = arguments.getOrNull(1) ?: run {
            source.sendTranslatable("pv.addon.groups.command.unset.error.usage")
            return
        }

        if (!unsetFlags.contains(flagName)) {
            source.sendTranslatable("pv.addon.groups.command.set.error.unknown_flag",
                flagName,
                unsetFlags.filter { source.hasFlagPermission(it) }.joinToString(", ")
            )
            return
        }

        if (!source.hasFlagPermission(flagName)) {
            source.noPermissionError("flag.$flagName")
            return
        }

        when(flagName) {
            "password" -> {
                if (group.password == null) {
                    source.sendTranslatable("pv.addon.groups.command.set.error.password_not_set")
                    return
                }
                group.password = null
                group.notifyPlayersTranslatable("pv.addon.groups.notifications.password_unset")
            }
            "permissions" -> {
                if (group.permissionsFilter.isEmpty()) {
                    source.sendTranslatable("pv.addon.groups.command.set.error.permissions_not_set")
                    return
                }
                group.permissionsFilter.clear()
                group.notifyPlayersTranslatable("pv.addon.groups.notifications.permissions_unset")
            }
            else -> {
                source.sendTranslatable("pv.addon.groups.error.unknown")
                return
            }
        }
    }

    override fun checkCanExecute(source: McCommandSource): Boolean {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return false

        val isOwner = group.isOwner(player)

        return when {
            source.hasAddonPermission("unset.owner") && isOwner -> true
            source.hasAddonPermission("unset.all") -> true
            source.hasAddonPermission("unset.*") -> true
            else -> false
        }
    }
}
