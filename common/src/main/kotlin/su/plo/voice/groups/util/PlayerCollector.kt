package su.plo.voice.groups.util

import su.plo.voice.api.server.player.VoicePlayer

fun interface PlayerCollector {
    fun getVisibleOnlinePlayers(player: VoicePlayer?): Collection<VoicePlayer>
}
