package su.plo.voice.groups.proxy

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import dev.apehum.mcdsl.command.DslCommandBuilder
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.slib.api.entity.player.McPlayer
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.command.CommandArgument
import su.plo.voice.groups.command.PlayerCandidates
import su.plo.voice.groups.command.ProfileCandidates
import su.plo.voice.groups.command.TargetArgument
import su.plo.voice.groups.util.extend.playerNotFoundError

class ProxyTargetArgument(
    private val voiceServer: () -> PlasmoBaseVoiceServer,
) : TargetArgument {
    override fun players(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        candidates: PlayerCandidates,
    ): CommandArgument<List<VoicePlayer>> {
        val argument =
            builder.argument(name, StringArgumentType.greedyString()) {
                suggestNames { source -> candidates(source).map { it.instance.name } }
            }

        return DslCommandBuilder.Argument { context ->
            context.resolve(parseNames(argument.getValue(context))) ?: emptyList()
        }
    }

    override fun profiles(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        lookup: ProfileCandidates,
        suggestions: ProfileCandidates,
    ): CommandArgument<List<McGameProfile>> {
        val argument =
            builder.argument(name, StringArgumentType.greedyString()) {
                suggestNames { source -> suggestions(source).map { it.name } }
            }

        return DslCommandBuilder.Argument { context ->
            context.resolveProfiles(parseNames(argument.getValue(context)), lookup) ?: emptyList()
        }
    }

    override fun profile(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        lookup: ProfileCandidates,
        suggestions: ProfileCandidates,
    ): CommandArgument<McGameProfile?> {
        val argument =
            builder.argument(name, StringArgumentType.word()) {
                suggestNames { source -> suggestions(source).map { it.name } }
            }

        return DslCommandBuilder.Argument { context ->
            context.resolveProfiles(listOf(argument.getValue(context)), lookup)?.single()
        }
    }

    private fun parseNames(input: String): List<String> =
        input
            .split(',', ' ')
            .filter { it.isNotEmpty() }
            .distinct()

    private fun CommandContext<McBrigadierSource>.resolve(names: List<String>): List<VoicePlayer>? {
        val sender = source.source as? McPlayer

        return names.map { name ->
            voiceServer()
                .playerManager
                .getPlayerByName(name)
                .orElse(null)
                ?.takeIf { sender?.canSee(it.instance) ?: true }
                ?: run {
                    source.playerNotFoundError()
                    return null
                }
        }
    }

    private fun CommandContext<McBrigadierSource>.resolveProfiles(
        names: List<String>,
        lookup: ProfileCandidates,
    ): List<McGameProfile>? {
        val candidates = lookup(source.source)

        return names.map { name ->
            candidates.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: run {
                source.playerNotFoundError()
                return null
            }
        }
    }

    private fun RequiredArgumentBuilder<McBrigadierSource, String>.suggestNames(names: (McCommandSource) -> Collection<String>) {
        suggests { context, builder ->
            val word = builder.remaining.substringAfterLast(',').substringAfterLast(' ')
            val offset = builder.createOffset(builder.input.length - word.length)

            names(context.source.source)
                .filter { it.startsWith(word, ignoreCase = true) }
                .forEach { offset.suggest(it) }

            offset.buildFuture()
        }
    }
}
