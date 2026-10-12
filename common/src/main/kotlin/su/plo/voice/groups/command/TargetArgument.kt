package su.plo.voice.groups.command

import dev.apehum.mcdsl.command.DslCommandBuilder
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.voice.api.server.player.VoicePlayer

typealias CommandArgument<T> = DslCommandBuilder.Argument<McBrigadierSource, T>

typealias PlayerCandidates = (source: McCommandSource) -> Collection<VoicePlayer>

interface TargetArgument {
    fun players(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        candidates: PlayerCandidates,
    ): CommandArgument<List<VoicePlayer>>

    fun player(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        candidates: PlayerCandidates,
    ): CommandArgument<VoicePlayer?>
}
