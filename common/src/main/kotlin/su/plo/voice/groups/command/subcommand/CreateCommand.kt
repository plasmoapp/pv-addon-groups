package su.plo.voice.groups.command.subcommand

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import dev.apehum.mcdsl.command.DslCommandBuilder
import dev.apehum.mcdsl.command.literalCommandBuilder
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.voice.groups.api.result.CreateResult
import su.plo.voice.groups.command.GroupsCommands
import su.plo.voice.groups.command.GroupsPermissions
import su.plo.voice.groups.command.anyPermission
import su.plo.voice.groups.command.sender
import su.plo.voice.groups.util.extend.asTextComponents
import su.plo.voice.groups.util.extend.checkNotNullAndNoFlagPermission
import su.plo.voice.groups.util.extend.createGroup
import su.plo.voice.groups.util.extend.getVoicePlayer
import su.plo.voice.groups.util.extend.hasFlagPermission
import su.plo.voice.groups.util.extend.printDivider
import su.plo.voice.groups.util.extend.sendFeedback

fun GroupsCommands.createCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("create") {
        requires(anyPermission("create"))

        val rawFlags by argumentOr("flags", StringArgumentType.greedyString(), "") {
            suggests { context, builder -> suggestFlags(context.source.source, builder).buildFuture() }
        }

        executes {
            val flags = parseFlags(rawFlags.split(" "))

            if (source.checkNotNullAndNoFlagPermission(flags.name, "name")) return@executes
            if (source.checkNotNullAndNoFlagPermission(flags.password, "password")) return@executes
            if (source.checkNotNullAndNoFlagPermission(flags.permissions, "permissions")) return@executes
            if (source.checkNotNullAndNoFlagPermission(flags.persistent, "persistent")) return@executes

            val result =
                groupManager.createGroup {
                    sender.getVoicePlayer(voiceServer)?.let { creator(it) }
                    flags.name?.let { name(it) }
                    flags.password?.let { password(it) }
                    flags.permissions?.let { permissions(it.toHashSet()) }
                    flags.persistent?.let { persistent(it) }
                }

            when (result) {
                is CreateResult.Success -> {
                    source.sendFeedback(McTextComponent.translatable("pv.addon.groups.command.create.success", result.group.name))
                    source.printDivider()
                    source.sendFeedback(result.group.asTextComponents(this@createCommand, sender))
                    source.printDivider()
                }

                is CreateResult.NameLengthInvalid -> {
                    source.sendFeedback(McTextComponent.translatable("pv.addon.groups.error.name_length", result.min, result.max))
                }
            }
        }
    }

private data class CreateFlags(
    val name: String? = null,
    val password: String? = null,
    val permissions: List<String>? = null,
    val persistent: Boolean? = null,
) {
    val usedFlags: List<String>
        get() =
            listOfNotNull(
                name?.let { "name" },
                password?.let { "password" },
                permissions?.let { "permissions" },
                persistent?.let { "persistent" },
            )
}

private fun parseFlags(tokens: List<String>): CreateFlags =
    tokens
        .mapIndexedNotNull { index, value -> if (value.contains(":")) index else null }
        .let { flagIndexes ->
            flagIndexes.mapIndexed { index, flagIndex ->
                val endIndex = flagIndexes.getOrNull(index + 1) ?: tokens.size
                tokens
                    .slice(flagIndex until endIndex)
                    .joinToString(" ")
            }
        }.associate {
            val split = it.split(":", limit = 2)
            split[0].trim() to split.getOrNull(1)?.trim()
        }.let { flags ->
            CreateFlags(
                flags["name"],
                flags["password"]?.split(" ")?.firstOrNull(),
                flags["permissions"]?.split(",")?.map { it.trim() },
                flags["persistent"]?.toBooleanStrictOrNull(),
            )
        }

private fun suggestFlags(
    source: McCommandSource,
    builder: SuggestionsBuilder,
): SuggestionsBuilder {
    val tokens = builder.remaining.split(" ")
    val lastToken = tokens.last()
    val offset = builder.createOffset(builder.input.length - lastToken.length)

    val insideFlag =
        tokens
            .getOrNull(tokens.size - 2)
            ?.let { it.contains(":") || it.endsWith(",") }
            ?: false

    if (!insideFlag) {
        val usedFlags = parseFlags(tokens).usedFlags
        GroupsPermissions.flags
            .filter { it !in usedFlags && source.hasFlagPermission(it) && it.startsWith(lastToken) }
            .forEach { offset.suggest("$it:") }
        return offset
    }

    val flagName =
        tokens
            .findLast { it.contains(":") }
            ?.substringBefore(":")
            ?: return offset

    if (flagName == "persistent" && source.hasFlagPermission(flagName)) {
        listOf("true", "false")
            .filter { it.startsWith(lastToken) }
            .forEach { offset.suggest(it) }
    }

    return offset
}
