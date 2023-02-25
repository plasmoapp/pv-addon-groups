package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.group.Group
import java.util.*

class CreateCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "create"

    override val permissions = listOf(
        "create" to PermissionDefault.TRUE,
        "create.name" to PermissionDefault.TRUE,
        "create.password" to PermissionDefault.TRUE,
        "create.persistent" to PermissionDefault.OP,
        "create.permissions" to PermissionDefault.OP,
    )

    private val flags = listOf(
        "name",
        "password",
        "permissions",
        "persistent",
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
                .joinToString("")
                .filterNot { it.isWhitespace() } }
        }.associate {
            val split = it.split(":", limit = 2)
            split.getOrNull(0)!! to split.getOrNull(1)
        }.let { Arguments(
            it["name"],
            it["password"],
            it["permissions"]?.split(","),
            it["persistent"]?.toBooleanStrictOrNull()
        )
    }


    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {

        val insideFlag = arguments.getOrNull(arguments.size.minus(2))
            ?.let { it.contains(":") || it.endsWith(",") }
            ?: false

        if (!insideFlag) return flags
            .filterNot { parseArguments(arguments).usedFlags.contains(it) }
            .filter { handler.hasPermission(source, "create.$it") }
            .map { "$it:" }

        val flagName = arguments.findLast { it.contains(":") }
            ?.split(":", limit = 2)
            ?.getOrNull(0)

        if (!handler.hasPermission(source, "create.$flagName")) return listOf()

        return when (flagName) {
            "name" -> listOf(handler.getTranslationByKey("pv.addon.groups.command.create.arg.name", source))
            "password" -> listOf(handler.getTranslationByKey("pv.addon.groups.command.create.arg.password", source))
            "permissions" -> listOf(handler.getTranslationByKey("pv.addon.groups.command.create.arg.permissions", source))
            "persistent" -> listOf("true", "false")
            else -> listOf()
        }
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        if (!handler.hasPermission(source, "create"))
            return handler.noPermission(source, "create")

        val parsedArgs = parseArguments(arguments)

        val player = if (source is MinecraftServerPlayer) {
            handler.voiceServer.playerManager.getPlayerById(source.uuid).orElse(null)
        } else null

        val name = parsedArgs.name
            .also {
                if (handler.checkNotNullAndNoPermission(it, source, "create.name")) return
            }
            ?: player?.instance?.name?.let {
                handler.groupManager.config.defaultGroupNameFormat.replace("%player%", it)
            }
            ?: "Server"

        if (name.length !in 3..16) {
            source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.command.create.error.name_length"))
            return
        }

        val password = parsedArgs.password

        if (handler.checkNotNullAndNoPermission(password, source, "create.password")) return

        val permissions = parsedArgs.permissions

        if (handler.checkNotNullAndNoPermission(permissions, source, "create.permissions")) return

        val persistent = parsedArgs.persistent
            .also {
                if (handler.checkNotNullAndNoPermission(it, source, "create.persistent")) return
            } ?: false

        val group = Group(UUID.randomUUID(), name, password, persistent)

        handler.groupManager.groups[group.id] = group

        if (permissions != null) {
            group.permissionsFilter = permissions.toHashSet()
        }

        if (player != null) {
            group.owner = player
            handler.groupManager.join(player, group)
        }

        source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.command.create.success", group.name))
    }
}