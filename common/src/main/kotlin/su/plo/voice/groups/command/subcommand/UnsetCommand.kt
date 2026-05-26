package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.api.result.SetPasswordResult
import su.plo.voice.groups.api.result.SetPermissionsResult
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.util.extend.getVoicePlayer
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.hasFlagPermission
import su.plo.voice.groups.util.extend.noPermissionError
import su.plo.voice.groups.util.extend.notInGroupError
import su.plo.voice.groups.util.extend.playerOnlyCommandError
import su.plo.voice.groups.util.extend.sendTranslatable

class UnsetCommand(
    handler: CommandHandler,
) : SubCommand(handler) {
    override val name = "unset"

    override val permissions =
        listOf(
            "unset.owner" to PermissionDefault.TRUE,
            "unset.*" to PermissionDefault.OP,
            "unset.all" to PermissionDefault.OP,
        )

    private val unsetFlags =
        listOf(
            "password",
            "permissions",
        )

    override fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> {
        val flagName = arguments.getOrNull(1) ?: return listOf()

        if (arguments.size == 2) {
            return unsetFlags.filter { it.startsWith(flagName) && source.hasFlagPermission(it) }
        }

        return listOf()
    }

    override fun execute(
        source: McCommandSource,
        arguments: Array<String>,
    ) {
        val player = source.getVoicePlayer(handler.voiceServer) ?: return source.playerOnlyCommandError()
        val group = handler.groupManager.getPlayerGroup(player.instance.uuid) ?: return source.notInGroupError()
        val isOwner = group.isOwner(player)

        when {
            source.hasAddonPermission("unset.all") -> {
                Unit
            }

            source.hasAddonPermission("unset.*") -> {
                Unit
            }

            source.hasAddonPermission("unset.owner") && isOwner -> {
                Unit
            }

            else -> {
                source.noPermissionError(if (isOwner) "unset.owner" else "unset.all")
                return
            }
        }

        val flagName =
            arguments.getOrNull(1) ?: run {
                source.sendTranslatable("pv.addon.groups.command.unset.error.usage")
                return
            }

        if (!unsetFlags.contains(flagName)) {
            source.sendTranslatable(
                "pv.addon.groups.command.set.error.unknown_flag",
                flagName,
                unsetFlags.filter { source.hasFlagPermission(it) }.joinToString(", "),
            )
            return
        }

        if (!source.hasFlagPermission(flagName)) {
            source.noPermissionError("flag.$flagName")
            return
        }

        when (flagName) {
            "password" -> {
                when (group.setPassword(null)) {
                    SetPasswordResult.Success -> {}

                    SetPasswordResult.IdenticalValue -> {
                        source.sendTranslatable("pv.addon.groups.command.set.error.password_not_set")
                    }
                }
            }

            "permissions" -> {
                when (group.setPermissions(null)) {
                    is SetPermissionsResult.Success -> {}

                    SetPermissionsResult.IdenticalValue -> {
                        source.sendTranslatable("pv.addon.groups.command.set.error.permissions_not_set")
                    }
                }
            }

            else -> {
                source.sendTranslatable("pv.addon.groups.error.unknown")
            }
        }
    }

    override fun checkCanExecute(source: McCommandSource): Boolean {
        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.getPlayerGroup(player.instance.uuid) ?: return false

        val isOwner = group.isOwner(player)

        return when {
            source.hasAddonPermission("unset.owner") && isOwner -> true
            source.hasAddonPermission("unset.all") -> true
            source.hasAddonPermission("unset.*") -> true
            else -> false
        }
    }
}
