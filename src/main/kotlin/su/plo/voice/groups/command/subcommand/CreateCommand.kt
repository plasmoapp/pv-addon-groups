package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.group.Group
import su.plo.voice.groups.utils.extend.*
import java.util.*

class CreateCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "create"

    override val permissions = listOf(
        "create" to PermissionDefault.TRUE,
    )

    private data class Arguments(
        val name: String? = null,
        val password: String? = null,
        val permissions: List<String>? = null,
        val persistent: Boolean? = null,
    ) {
        val usedFlags: List<String> get() = listOfNotNull(
            name?.let { "name" },
            password?.let { "password" },
            permissions?.let { "permissions" },
            persistent?.let { "persistent" },
        )
    }

    private fun parseArguments(arguments: Array<out String>): Arguments = arguments
        .mapIndexed { index, value -> if (value.contains(":")) index else null }
        .filterNotNull()
        .let { flagIndexes -> flagIndexes.mapIndexed { index, flagIndex ->
            val endIndex = flagIndexes.getOrNull(index + 1) ?: arguments.size
            arguments.slice(flagIndex..endIndex.minus(1))
                .joinToString(" ")
        }
        }.associate {
            val split = it.split(":", limit = 2)
            split.getOrNull(0)!!.trim() to split.getOrNull(1)?.trim()
        }
//        }.also { args ->
//            args.keys.find { !handler.flags.keys.contains(it) }?.let {
//                return source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.command.create.error.name_length"))
//            }
//        }
        .let { args -> Arguments(
            args["name"],
            args["password"]?.split(" ")?.firstOrNull(),
            args["permissions"]?.split(",")?.map { it.trim() },
            args["persistent"]?.toBooleanStrictOrNull()
        )
    }


    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {

        val insideFlag = arguments.getOrNull(arguments.size.minus(2))
            ?.let {
                (it.contains(":") || it.endsWith(","))
            }
            ?: false

        val lastArg = arguments.lastOrNull() ?: ""

        if (!insideFlag) return handler.flags
            .map { it.key }
            .filterNot { parseArguments(arguments).usedFlags.contains(it) }
            .filter { source.hasFlagPermission(it) && it.startsWith(lastArg) }
            .map { "$it:" }

        val flagName = arguments.findLast { it.contains(":") }
            ?.split(":", limit = 2)
            ?.getOrNull(0)
            ?: return listOf()

        if (!source.hasFlagPermission(flagName)) return listOf()

        return when (flagName) {
            "name" -> listOf(handler.getTranslationByKey("pv.addon.groups.command.create.arg.name", source))
            "password" -> listOf(handler.getTranslationByKey("pv.addon.groups.command.create.arg.password", source))
            "permissions" -> listOf(handler.getTranslationByKey("pv.addon.groups.command.create.arg.permissions", source))
            "persistent" -> listOf("true", "false")
            else -> listOf()
        }
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        if (source.checkAddonPermissionAndPrintError("create")) return

        val parsedArgs = parseArguments(arguments)

        val player = source.getVoicePlayer(handler.voiceServer)

        val name = parsedArgs.name
            .also {
                if (source.checkNotNullAndNoFlagPermission(it, "name")) return
            } ?: handler.groupManager.config.defaultGroupNameFormat
                .replace("%player%", (player?.instance?.name ?: "Server"))

        val min = handler.groupManager.config.minimumNameLength
        val max = handler.groupManager.config.maximumNameLength

        if (name.length !in min..max) {
            source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.error.name_length"))
            return
        }

        val password = parsedArgs.password

        if (source.checkNotNullAndNoFlagPermission(password, "password")) return

        val permissions = parsedArgs.permissions

        if (source.checkNotNullAndNoFlagPermission(permissions, "permissions")) return

        val persistent = parsedArgs.persistent
            .also {
                if (source.checkNotNullAndNoFlagPermission(it, "persistent")) return
            } ?: false

        val group = Group(
            handler.groupManager.sourceLine.createBroadcastSet(),
            UUID.randomUUID(),
            name,
            password,
            persistent
        )

        handler.groupManager.groups[group.id] = group

        if (permissions != null) {
            group.permissionsFilter = permissions.toHashSet()
        }

        if (player != null) {
            group.owner = player.instance.gameProfile
            handler.groupManager.join(player, group)
        }

        source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.command.create.success", group.name))
    }

    override fun checkCanExecute(source: MinecraftCommandSource): Boolean = source.hasAddonPermission("create")
}
