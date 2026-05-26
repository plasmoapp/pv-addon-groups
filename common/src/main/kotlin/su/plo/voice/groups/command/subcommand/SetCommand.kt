package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.api.result.SetNameResult
import su.plo.voice.groups.api.result.SetPasswordResult
import su.plo.voice.groups.api.result.SetPermissionsResult
import su.plo.voice.groups.api.result.SetPersistentResult
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.util.extend.getVoicePlayer
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.hasFlagPermission
import su.plo.voice.groups.util.extend.noPermissionError
import su.plo.voice.groups.util.extend.notInGroupError
import su.plo.voice.groups.util.extend.playerOnlyCommandError
import su.plo.voice.groups.util.extend.sendTranslatable

class SetCommand(
    handler: CommandHandler,
) : SubCommand(handler) {
    override val name = "set"

    override val permissions =
        listOf(
            "set.owner" to PermissionDefault.TRUE,
            "set.*" to PermissionDefault.OP,
            "set.all" to PermissionDefault.OP,
        )

    override fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> {
        val flagName = arguments.getOrNull(1) ?: return listOf()

        if (arguments.size == 2) {
            return handler.flags.keys.filter { it.startsWith(flagName) && source.hasFlagPermission(it) }
        }

        if (!source.hasFlagPermission(flagName)) return listOf()

        return when {
            flagName == "name" -> listOf(handler.getTranslationByKey("pv.addon.groups.arg.name", source))
            flagName == "password" && arguments.size == 3 -> listOf(handler.getTranslationByKey("pv.addon.groups.arg.password", source))
            flagName == "permissions" -> listOf(handler.getTranslationByKey("pv.addon.groups.arg.permissions", source))
            flagName == "persistent" && arguments.size == 3 -> listOf("true", "false")
            else -> listOf()
        }
    }

    override fun execute(
        source: McCommandSource,
        arguments: Array<String>,
    ) {
        val player = source.getVoicePlayer(handler.voiceServer) ?: return source.playerOnlyCommandError()
        val group = handler.groupManager.getPlayerGroup(player.instance.uuid) ?: return source.notInGroupError()
        val isOwner = group.isOwner(player)

        when {
            source.hasAddonPermission("set.all") -> {
                Unit
            }

            source.hasAddonPermission("set.*") -> {
                Unit
            }

            source.hasAddonPermission("set.owner") && isOwner -> {
                Unit
            }

            else -> {
                source.noPermissionError(if (isOwner) "set.owner" else "set.all")
                return
            }
        }

        val flagName =
            arguments.getOrNull(1) ?: run {
                source.sendTranslatable("pv.addon.groups.command.set.error.usage")
                return
            }

        if (!handler.flags.keys.contains(flagName)) {
            source.sendTranslatable(
                "pv.addon.groups.command.set.error.unknown_flag",
                flagName,
                handler.flags.keys
                    .filter { source.hasFlagPermission(it) }
                    .joinToString(", "),
            )
            return
        }

        if (!source.hasFlagPermission(flagName)) {
            source.noPermissionError("flag.$flagName")
            return
        }

        arguments.getOrNull(2) ?: run {
            source.sendTranslatable("pv.addon.groups.command.set.error.usage")
            return
        }

        val flagValue = arguments.drop(2).joinToString(" ")

        when (flagName) {
            "name" -> {
                when (group.setName(flagValue)) {
                    is SetNameResult.Success -> {}

                    SetNameResult.IdenticalValue -> {
                        source.sendTranslatable("pv.addon.groups.command.set.error.identical_value")
                    }

                    is SetNameResult.NameLengthInvalid -> {
                        source.sendTranslatable("pv.addon.groups.error.name_length")
                    }
                }
            }

            "password" -> {
                when (group.setPassword(flagValue)) {
                    SetPasswordResult.Success -> {}

                    SetPasswordResult.IdenticalValue -> {
                        source.sendTranslatable("pv.addon.groups.command.set.error.identical_value")
                    }
                }
            }

            "permissions" -> {
                val newPermissions = flagValue.split(",").map { it.trim() }.toHashSet()
                when (group.setPermissions(newPermissions)) {
                    is SetPermissionsResult.Success -> {}

                    SetPermissionsResult.IdenticalValue -> {
                        source.sendTranslatable("pv.addon.groups.command.set.error.identical_value")
                    }
                }
            }

            "persistent" -> {
                val newValue = flagValue.toBooleanStrictOrNull() ?: false
                when (group.setPersistent(newValue)) {
                    SetPersistentResult.Success -> {}

                    SetPersistentResult.IdenticalValue -> {
                        source.sendTranslatable("pv.addon.groups.command.set.error.identical_value")
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
            source.hasAddonPermission("set.owner") && isOwner -> true
            source.hasAddonPermission("set.all") -> true
            source.hasAddonPermission("set.*") -> true
            else -> false
        }
    }
}
