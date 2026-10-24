package su.plo.voice.groups.channel

import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.event.connection.UdpClientDisconnectedEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.group.GroupImpl
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.InviteCandidates
import su.plo.voice.groups.protocol.MAX_INVITE_CANDIDATES
import su.plo.voice.groups.protocol.MIN_INVITE_QUERY_LENGTH
import su.plo.voice.groups.protocol.RateLimited
import su.plo.voice.groups.util.PlayerCollector
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InviteSearch(
    private val playerCollector: PlayerCollector,
    private val serverName: (VoicePlayer) -> String?,
) {
    private val rateWindows: MutableMap<UUID, RateWindow> = ConcurrentHashMap()

    fun checkRateLimit(playerId: UUID): RateLimited? {
        val now = System.nanoTime()
        val window =
            rateWindows.compute(playerId) { _, window ->
                window
                    ?.takeIf { now - it.start < RATE_WINDOW_NANOS }
                    ?.let { it.copy(count = it.count + 1) }
                    ?: RateWindow(now, 1)
            }

        if (window!!.count <= MAX_SEARCHES_PER_WINDOW) return null

        return RateLimited(retryAfterMs = Math.ceilDiv(window.start + RATE_WINDOW_NANOS - now, 1_000_000L))
    }

    fun search(
        viewer: VoicePlayer,
        group: GroupImpl,
        query: String,
    ): InviteCandidates {
        val search = query.trim()
        val networkWide = search.length >= MIN_INVITE_QUERY_LENGTH
        val viewerId = viewer.instance.uuid
        val viewerServer = serverName(viewer)

        val matches =
            playerCollector
                .getVisibleOnlinePlayers(viewer)
                .filter {
                    val id = it.instance.uuid
                    id != viewerId && id !in group.playersById && it.hasVoiceChat()
                }.mapNotNull { player ->
                    val sameServer = viewerServer != null && serverName(player) == viewerServer
                    if (!networkWide && !sameServer && group.getInvite(player.instance.uuid) == null) {
                        return@mapNotNull null
                    }

                    val kind = matchKind(player.instance.name, search) ?: return@mapNotNull null

                    Match(player, kind, sameServer)
                }.sortedWith(
                    compareBy<Match> { it.kind }
                        .thenBy { !it.sameServer }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.player.instance.name },
                )

        return InviteCandidates(
            matches
                .take(MAX_INVITE_CANDIDATES)
                .map { GroupPlayer(it.player.instance.gameProfile, online = true) },
            hasMore = matches.size > MAX_INVITE_CANDIDATES,
        )
    }

    @EventSubscribe
    fun onUdpDisconnected(event: UdpClientDisconnectedEvent) {
        rateWindows -= event.connection.player.instance.uuid
    }

    private fun matchKind(
        name: String,
        search: String,
    ): MatchKind? =
        when {
            search.isEmpty() || name.equals(search, ignoreCase = true) -> MatchKind.EXACT
            name.startsWith(search, ignoreCase = true) -> MatchKind.PREFIX
            name.contains(search, ignoreCase = true) -> MatchKind.CONTAINS
            else -> null
        }

    private class Match(
        val player: VoicePlayer,
        val kind: MatchKind,
        val sameServer: Boolean,
    )

    private enum class MatchKind {
        EXACT,
        PREFIX,
        CONTAINS,
    }

    private data class RateWindow(
        val start: Long,
        val count: Int,
    )

    private companion object {
        const val RATE_WINDOW_NANOS = 1_000_000_000L
        const val MAX_SEARCHES_PER_WINDOW = 5
    }
}
