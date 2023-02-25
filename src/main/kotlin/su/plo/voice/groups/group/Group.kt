package su.plo.voice.groups.group

import su.plo.voice.api.server.player.VoicePlayer
import java.util.UUID

class Group(
    val id: UUID,
    var name: String,
    var password: String? = null,
    var persistent: Boolean = false,
) {
    var owner: VoicePlayer? = null
    val players: HashSet<VoicePlayer> = HashSet()
    val bannedPlayers: HashSet<VoicePlayer> = HashSet()
    var permissionsFilter: HashSet<String> = HashSet()

    val playerCount: Int
        get() { return players.size }
}