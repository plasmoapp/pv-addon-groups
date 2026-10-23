package su.plo.voice.groups.server.command

import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import dev.apehum.mcdsl.command.DslCommandBuilder
import dev.apehum.mcdsl.command.extension.suggests
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.slib.api.entity.player.McPlayer
import su.plo.slib.api.server.command.brigadier.McArgumentTypes
import su.plo.slib.api.server.entity.player.McServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.command.CommandArgument
import su.plo.voice.groups.command.PlayerCandidates
import su.plo.voice.groups.command.ProfileCandidates
import su.plo.voice.groups.command.TargetArgument
import su.plo.voice.groups.util.extend.playerNotFoundError

class ServerTargetArgument(
    private val voiceServer: () -> PlasmoVoiceServer,
) : TargetArgument {
    override fun players(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        candidates: PlayerCandidates,
    ): CommandArgument<List<VoicePlayer>> {
        val argument = builder.argument(name, McArgumentTypes.players())

        return DslCommandBuilder.Argument { context ->
            context.visibleVoicePlayers(argument.getValue(context).resolve(context.source))
        }
    }

    override fun profiles(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        lookup: ProfileCandidates,
        suggestions: ProfileCandidates,
    ): CommandArgument<List<McGameProfile>> {
        val type =
            ProfileTargetsType(McArgumentTypes.players(), lookup) { selector, source ->
                selector.resolve(source).map { it.gameProfile }
            }
        val argument = builder.argument(name, type) { suggestNames(suggestions) }

        return DslCommandBuilder.Argument { context ->
            context.resolveProfiles(argument.getValue(context))
        }
    }

    override fun profile(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        lookup: ProfileCandidates,
        suggestions: ProfileCandidates,
    ): CommandArgument<McGameProfile?> {
        val type =
            ProfileTargetsType(McArgumentTypes.player(), lookup) { selector, source ->
                listOf(selector.resolve(source).gameProfile)
            }
        val argument = builder.argument(name, type) { suggestNames(suggestions) }

        return DslCommandBuilder.Argument { context ->
            context.resolveProfiles(argument.getValue(context)).firstOrNull()
        }
    }

    private fun CommandContext<McBrigadierSource>.visibleVoicePlayers(players: List<McServerPlayer>): List<VoicePlayer> {
        val sender = source.source as? McPlayer

        return players
            .filter { sender?.canSee(it) ?: true }
            .map { voiceServer().playerManager.getPlayerByInstance(it.getInstance()) }
            .ifEmpty {
                source.playerNotFoundError()
                emptyList()
            }
    }

    private fun CommandContext<McBrigadierSource>.resolveProfiles(targets: ProfileTargetsResolver): List<McGameProfile> =
        targets.resolve(source).ifEmpty {
            source.playerNotFoundError()
            emptyList()
        }

    private fun RequiredArgumentBuilder<McBrigadierSource, ProfileTargetsResolver>.suggestNames(suggestions: ProfileCandidates) {
        suggests { context ->
            suggestions(context.source.source)
                .filter { it.name.startsWith(remaining, ignoreCase = true) }
                .forEach { suggest(it.name) }
        }
    }
}
