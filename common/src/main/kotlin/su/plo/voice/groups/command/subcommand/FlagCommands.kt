package su.plo.voice.groups.command.subcommand

import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import dev.apehum.mcdsl.command.DslCommandBuilder
import dev.apehum.mcdsl.command.literalCommandBuilder
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.voice.groups.api.result.SetNameResult
import su.plo.voice.groups.api.result.SetPasswordResult
import su.plo.voice.groups.api.result.SetPermissionsResult
import su.plo.voice.groups.api.result.SetPersistentResult
import su.plo.voice.groups.command.GroupsCommands
import su.plo.voice.groups.command.flagPermission
import su.plo.voice.groups.command.sender
import su.plo.voice.groups.command.tierPermissions
import su.plo.voice.groups.util.extend.sendTranslatable

private const val IDENTICAL_VALUE = "pv.addon.groups.command.set.error.identical_value"

fun GroupsCommands.setCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("set") {
        requires(tierPermissions("set"))

        literalCommand("name") {
            requires(flagPermission("name"))

            val name by argument("name", StringArgumentType.greedyString())

            executes {
                val (_, group) = requireGroup("set") ?: return@executes

                when (val result = group.setName(name)) {
                    is SetNameResult.Success -> {}

                    SetNameResult.IdenticalValue -> {
                        source.sendTranslatable(IDENTICAL_VALUE)
                    }

                    is SetNameResult.NameLengthInvalid -> {
                        source.sendTranslatable("pv.addon.groups.error.name_length", result.min, result.max)
                    }
                }
            }
        }

        literalCommand("password") {
            requires(flagPermission("password"))

            val password by argument("password", StringArgumentType.greedyString())

            executes {
                val (_, group) = requireGroup("set") ?: return@executes

                when (group.setPassword(password)) {
                    SetPasswordResult.Success -> {}

                    SetPasswordResult.IdenticalValue -> {
                        source.sendTranslatable(IDENTICAL_VALUE)
                    }
                }
            }
        }

        literalCommand("permissions") {
            requires(flagPermission("permissions"))

            val permissions by argument("permissions", StringArgumentType.greedyString())

            executes {
                val (_, group) = requireGroup("set") ?: return@executes

                val newPermissions = permissions.split(",").map { it.trim() }.toHashSet()
                when (group.setPermissions(newPermissions)) {
                    is SetPermissionsResult.Success -> {}

                    SetPermissionsResult.IdenticalValue -> {
                        source.sendTranslatable(IDENTICAL_VALUE)
                    }
                }
            }
        }

        literalCommand("persistent") {
            requires(flagPermission("persistent"))

            val persistent by argument("persistent", BoolArgumentType.bool())

            executes {
                val (_, group) = requireGroup("set") ?: return@executes

                when (group.setPersistent(persistent)) {
                    SetPersistentResult.Success -> {}

                    SetPersistentResult.IdenticalValue -> {
                        source.sendTranslatable(IDENTICAL_VALUE)
                    }
                }
            }
        }
    }

fun GroupsCommands.unsetCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("unset") {
        requires(tierPermissions("unset"))

        literalCommand("password") {
            requires(flagPermission("password"))

            executes {
                val (_, group) = requireGroup("unset") ?: return@executes

                when (group.setPassword(null)) {
                    SetPasswordResult.Success -> {}

                    SetPasswordResult.IdenticalValue -> {
                        source.sendTranslatable("pv.addon.groups.command.set.error.password_not_set")
                    }
                }
            }
        }

        literalCommand("permissions") {
            requires(flagPermission("permissions"))

            executes {
                val (_, group) = requireGroup("unset") ?: return@executes

                when (group.setPermissions(null)) {
                    is SetPermissionsResult.Success -> {}

                    SetPermissionsResult.IdenticalValue -> {
                        source.sendTranslatable("pv.addon.groups.command.set.error.permissions_not_set")
                    }
                }
            }
        }
    }
