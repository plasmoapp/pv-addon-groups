package su.plo.voice.groups.command

import dev.apehum.mcdsl.command.DslCommandBuilder
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.api.server.player.VoicePlayer

typealias CommandArgument<T> = DslCommandBuilder.Argument<McBrigadierSource, T>

typealias PlayerCandidates = (source: McCommandSource) -> Collection<VoicePlayer>

typealias ProfileCandidates = (source: McCommandSource) -> Collection<McGameProfile>

interface TargetArgument {
    fun players(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        candidates: PlayerCandidates,
    ): CommandArgument<List<VoicePlayer>>

    fun profiles(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        lookup: ProfileCandidates,
        suggestions: ProfileCandidates,
    ): CommandArgument<List<McGameProfile>>

    fun profile(
        builder: DslCommandBuilder<McBrigadierSource>,
        name: String,
        lookup: ProfileCandidates,
        suggestions: ProfileCandidates,
    ): CommandArgument<McGameProfile?>
}
