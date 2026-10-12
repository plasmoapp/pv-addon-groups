package su.plo.voice.groups.proxy

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import dev.apehum.mcdsl.command.DslCommandBuilder
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.slib.api.entity.player.McPlayer
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.command.CommandArgument
import su.plo.voice.groups.command.PlayerCandidates
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
        val argument = builder.argument(name, StringArgumentType.greedyString()) { suggestNames(candidates) }

        return DslCommandBuilder.Argument { context ->
            val names =
                argument
                    .getValue(context)
                    .split(',', ' ')
                    .filter { it.isNotEmpty() }
                    .distinct()

            context.resolve(names) ?: emptyList()
        }
    }

    override fun player(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        candidates: PlayerCandidates,
    ): CommandArgument<VoicePlayer?> {
        val argument = builder.argument(name, StringArgumentType.word()) { suggestNames(candidates) }

        return DslCommandBuilder.Argument { context ->
            context.resolve(listOf(argument.getValue(context)))?.single()
        }
    }

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

    private fun RequiredArgumentBuilder<McBrigadierSource, String>.suggestNames(candidates: PlayerCandidates) {
        suggests { context, builder ->
            val word = builder.remaining.substringAfterLast(',').substringAfterLast(' ')
            val offset = builder.createOffset(builder.input.length - word.length)

            candidates(context.source.source)
                .map { it.instance.name }
                .filter { it.startsWith(word, ignoreCase = true) }
                .forEach { offset.suggest(it) }

            offset.buildFuture()
        }
    }
}
