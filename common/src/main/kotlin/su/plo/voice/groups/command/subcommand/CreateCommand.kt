package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.api.result.CreateResult
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.util.extend.asTextComponents
import su.plo.voice.groups.util.extend.checkAddonPermissionAndPrintError
import su.plo.voice.groups.util.extend.checkNotNullAndNoFlagPermission
import su.plo.voice.groups.util.extend.createGroup
import su.plo.voice.groups.util.extend.getVoicePlayer
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.hasFlagPermission
import su.plo.voice.groups.util.extend.printDivider
import su.plo.voice.groups.util.extend.sendMessage

class CreateCommand(
    handler: CommandHandler,
) : SubCommand(handler) {
    override val name = "create"

    override val permissions =
        listOf(
            "create" to PermissionDefault.TRUE,
        )

    private data class Arguments(
        val name: String? = null,
        val password: String? = null,
        val permissions: List<String>? = null,
        val persistent: Boolean? = null,
    ) {
        val usedFlags: List<String> get() =
            listOfNotNull(
                name?.let { "name" },
                password?.let { "password" },
                permissions?.let { "permissions" },
                persistent?.let { "persistent" },
            )
    }

    private fun parseArguments(arguments: Array<String>): Arguments =
        arguments
            .mapIndexed { index, value -> if (value.contains(":")) index else null }
            .filterNotNull()
            .let { flagIndexes ->
                flagIndexes.mapIndexed { index, flagIndex ->
                    val endIndex = flagIndexes.getOrNull(index + 1) ?: arguments.size
                    arguments
                        .slice(flagIndex..endIndex.minus(1))
                        .joinToString(" ")
                }
            }.associate {
                val split = it.split(":", limit = 2)
                split.getOrNull(0)!!.trim() to split.getOrNull(1)?.trim()
            }.let { args ->
                Arguments(
                    args["name"],
                    args["password"]?.split(" ")?.firstOrNull(),
                    args["permissions"]?.split(",")?.map { it.trim() },
                    args["persistent"]?.toBooleanStrictOrNull(),
                )
            }

    override fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> {
        val insideFlag =
            arguments
                .getOrNull(arguments.size.minus(2))
                ?.let {
                    (it.contains(":") || it.endsWith(","))
                }
                ?: false

        val lastArg = arguments.lastOrNull() ?: ""

        if (!insideFlag) {
            return handler.flags
                .map { it.key }
                .filterNot { parseArguments(arguments).usedFlags.contains(it) }
                .filter { source.hasFlagPermission(it) && it.startsWith(lastArg) }
                .map { "$it:" }
        }

        val flagName =
            arguments
                .findLast { it.contains(":") }
                ?.split(":", limit = 2)
                ?.getOrNull(0)
                ?: return listOf()

        if (!source.hasFlagPermission(flagName)) return listOf()

        return when (flagName) {
            "name" -> listOf(handler.getTranslationByKey("pv.addon.groups.arg.name", source))
            "password" -> listOf(handler.getTranslationByKey("pv.addon.groups.arg.password", source))
            "permissions" -> listOf(handler.getTranslationByKey("pv.addon.groups.arg.permissions", source))
            "persistent" -> listOf("true", "false")
            else -> listOf()
        }
    }

    override fun execute(
        source: McCommandSource,
        arguments: Array<String>,
    ) {
        if (source.checkAddonPermissionAndPrintError("create")) return

        val parsedArgs = parseArguments(arguments)

        if (source.checkNotNullAndNoFlagPermission(parsedArgs.name, "name")) return
        if (source.checkNotNullAndNoFlagPermission(parsedArgs.password, "password")) return
        if (source.checkNotNullAndNoFlagPermission(parsedArgs.permissions, "permissions")) return
        if (source.checkNotNullAndNoFlagPermission(parsedArgs.persistent, "persistent")) return

        val result =
            handler.groupManager.createGroup {
                source.getVoicePlayer(handler.voiceServer)?.let { creator(it) }
                parsedArgs.name?.let { name(it) }
                parsedArgs.password?.let { password(it) }
                parsedArgs.permissions?.let { permissions(it.toHashSet()) }
                parsedArgs.persistent?.let { persistent(it) }
            }

        when (result) {
            is CreateResult.Success -> {
                source.sendMessage(McTextComponent.translatable("pv.addon.groups.command.create.success", result.group.name))
                source.printDivider()
                source.sendMessage(result.group.asTextComponents(handler, source))
                source.printDivider()
            }

            is CreateResult.NameLengthInvalid -> {
                source.sendMessage(McTextComponent.translatable("pv.addon.groups.error.name_length", result.min, result.max))
            }
        }
    }

    override fun checkCanExecute(source: McCommandSource): Boolean = source.hasAddonPermission("create")
}
