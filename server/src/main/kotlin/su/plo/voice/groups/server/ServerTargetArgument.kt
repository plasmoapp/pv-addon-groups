package su.plo.voice.groups.server

import com.mojang.brigadier.context.CommandContext
import dev.apehum.mcdsl.command.DslCommandBuilder
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.slib.api.entity.player.McPlayer
import su.plo.slib.api.server.command.brigadier.McArgumentTypes
import su.plo.slib.api.server.entity.player.McServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.command.CommandArgument
import su.plo.voice.groups.command.PlayerCandidates
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

    override fun player(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        candidates: PlayerCandidates,
    ): CommandArgument<VoicePlayer?> {
        val argument = builder.argument(name, McArgumentTypes.player())

        return DslCommandBuilder.Argument { context ->
            context.visibleVoicePlayers(listOf(argument.getValue(context).resolve(context.source))).firstOrNull()
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
}
