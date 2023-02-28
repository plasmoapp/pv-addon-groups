package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class SetCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "set"

    override val permissions = listOf(
        "set.owner" to PermissionDefault.TRUE,
        "set.*" to PermissionDefault.OP,
        "set.all" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {

        val flagName = arguments.getOrNull(1) ?: return listOf()

        if (arguments.size == 2) {
            return handler.flags.keys.filter { it.startsWith(flagName) && source.hasFlagPermission(it) }
        }

        if (!source.hasFlagPermission(flagName)) return listOf()

        return when {
            flagName == "name" -> listOf(handler.getTranslationByKey("pv.addon.groups.command.create.arg.name", source))
            flagName == "password" && arguments.size == 3 -> listOf(handler.getTranslationByKey("pv.addon.groups.command.create.arg.password", source))
            flagName == "permissions" -> listOf(handler.getTranslationByKey("pv.addon.groups.command.create.arg.permissions", source))
            flagName == "persistent" && arguments.size == 3 -> listOf("true", "false")
            else -> listOf()
        }
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return source.playerOnlyCommandError()
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return source.notInGroupError()
        val isOwner = group.owner?.id == player.instance.uuid

        when {
            source.hasAddonPermission("set.all") -> Unit
            source.hasAddonPermission("set.*") -> Unit
            source.hasAddonPermission("set.owner") && isOwner -> Unit
            else -> {
                source.noPermissionError(if (isOwner) "set.owner" else "set.all")
                return
            }
        }

        val flagName = arguments.getOrNull(1) ?: run {
            source.sendTranslatable("pv.addon.groups.command.set.error.usage")
            return
        }

        if (!handler.flags.keys.contains(flagName)) {
            source.sendTranslatable("pv.addon.groups.command.set.error.unknown_flag",
                flagName,
                handler.flags.keys.filter { source.hasFlagPermission(it) }.joinToString(", ")
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

        when(flagName) {
            "name" -> {
                if (group.name == flagValue) {
                    source.sendTranslatable("pv.addon.groups.command.set.error.identical_value")
                    return
                }

                val min = handler.groupManager.config.minimumNameLength
                val max = handler.groupManager.config.maximumNameLength

                if (flagValue.length !in min..max) {
                    source.sendTranslatable("pv.addon.groups.error.name_length")
                    return
                }

                val oldName = group.name
                group.name = flagValue
                group.notifyPlayersTranslatable("pv.addon.groups.notifications.name_change", oldName, group.name)
            }
            "password" -> {
                if (group.password == flagValue) {
                    source.sendTranslatable("pv.addon.groups.command.set.error.identical_value")
                    return
                }
                group.password = flagValue
                group.notifyPlayersTranslatable("pv.addon.groups.notifications.password_changed")
            }
            "permissions" -> {

                val newValue = flagValue
                    .split(",")
                    .map { it.trim() }
                    .toHashSet()

                if (group.permissionsFilter == newValue) {
                    source.sendTranslatable("pv.addon.groups.command.set.error.identical_value")
                    return
                }

                group.permissionsFilter = newValue

                group.notifyPlayersTranslatable("pv.addon.groups.notifications.permissions_changed")

                group.players
                    .filter { !group.hasPermission(it.instance) }
                    .forEach {
                        handler.groupManager.leave(it)
                        it.instance.sendTranslatable("pv.addon.groups.notifications.kicked")
                        group.notifyPlayersTranslatable("pv.addon.groups.notifications.player_kicked", it.instance.name)
                    }
            }
            "persistent" -> {
                val newValue = flagValue.toBooleanStrictOrNull() ?: false

                if (group.persistent == newValue) {
                    source.sendTranslatable("pv.addon.groups.command.set.error.identical_value")
                    return
                }

                group.persistent = newValue

                if (group.persistent) group.notifyPlayersTranslatable("pv.addon.groups.notifications.persistent_true")
                else group.notifyPlayersTranslatable("pv.addon.groups.notifications.persistent_false")
            }
            else -> {
                source.sendTranslatable("pv.addon.groups.error.unknown")
                return
            }
        }
    }

    override fun checkCanExecute(source: MinecraftCommandSource): Boolean {

        val player = source.getVoicePlayer(handler.voiceServer) ?: return false
        val group = handler.groupManager.groupByPlayer[player.instance.uuid] ?: return false

        val isOwner = group.owner?.id == player.instance.uuid

        return when {
            source.hasAddonPermission("set.owner") && isOwner -> true
            source.hasAddonPermission("set.all") -> true
            source.hasAddonPermission("set.*") -> true
            else -> false
        }
    }
}
