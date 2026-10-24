package su.plo.voice.groups.client.service

import net.minecraft.client.Minecraft
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.groups.client.channel.ChannelClient
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.BanRequest
import su.plo.voice.groups.protocol.BanResult
import su.plo.voice.groups.protocol.BrowseRequest
import su.plo.voice.groups.protocol.CancelInviteRequest
import su.plo.voice.groups.protocol.CancelInviteResult
import su.plo.voice.groups.protocol.CreateRequest
import su.plo.voice.groups.protocol.CreateResult
import su.plo.voice.groups.protocol.DeclineInviteRequest
import su.plo.voice.groups.protocol.DeclineInviteResult
import su.plo.voice.groups.protocol.DeleteRequest
import su.plo.voice.groups.protocol.DeleteResult
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupSummary
import su.plo.voice.groups.protocol.GroupsState
import su.plo.voice.groups.protocol.GroupsStateChanged
import su.plo.voice.groups.protocol.HandshakeRequest
import su.plo.voice.groups.protocol.HandshakeResponse
import su.plo.voice.groups.protocol.InviteCandidatesRequest
import su.plo.voice.groups.protocol.InviteCandidatesResult
import su.plo.voice.groups.protocol.InviteRequest
import su.plo.voice.groups.protocol.InviteResult
import su.plo.voice.groups.protocol.JoinRequest
import su.plo.voice.groups.protocol.JoinResult
import su.plo.voice.groups.protocol.KickRequest
import su.plo.voice.groups.protocol.KickResult
import su.plo.voice.groups.protocol.LeaveRequest
import su.plo.voice.groups.protocol.LeaveResult
import su.plo.voice.groups.protocol.MIN_PROTOCOL_VERSION
import su.plo.voice.groups.protocol.PROTOCOL_VERSION
import su.plo.voice.groups.protocol.StateRequest
import su.plo.voice.groups.protocol.TransferRequest
import su.plo.voice.groups.protocol.TransferResult
import su.plo.voice.groups.protocol.UnbanRequest
import su.plo.voice.groups.protocol.UnbanResult
import su.plo.voice.groups.protocol.UpdateSettingsRequest
import su.plo.voice.groups.protocol.UpdateSettingsResult
import su.plo.voice.groups.protocol.isProtocolCompatible
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.jvm.optionals.getOrNull

private val logger = McLoggerFactory.createLogger(ADDON_LOGGER, "NetworkGroupsService")

class NetworkGroupsService(
    private val channel: ChannelClient,
    private val voiceClient: PlasmoVoiceClient,
) : GroupsService {
    private val listeners = CopyOnWriteArrayList<GroupsStateListener>()
    private val pushes = AtomicLong()

    @Volatile
    private var session: HandshakeResponse? = null

    @Volatile
    override var state: GroupsState? = null
        private set

    override val available: Boolean
        get() = session != null

    override val self: GroupPlayer
        get() =
            Minecraft.getInstance().user.let {
                GroupPlayer(McGameProfile(it.profileId, it.name, emptyList()), online = true)
            }

    override val voicePlayers: List<GroupPlayer>
        get() =
            voiceClient.serverConnection
                .map { connection ->
                    connection.players.map {
                        GroupPlayer(it.toGameProfile(), online = true)
                    }
                }.getOrNull()
                ?: emptyList()

    init {
        channel.registerMessageHandler<GroupsStateChanged> { message ->
            pushes.incrementAndGet()
            state = message.state
            listeners.forEach { it(message.state) }
        }
    }

    override fun addListener(listener: GroupsStateListener) {
        listeners += listener
    }

    override fun removeListener(listener: GroupsStateListener) {
        listeners -= listener
    }

    override suspend fun fetchState(): GroupsState {
        val pushesBefore = pushes.get()
        val fetched = channel.request(StateRequest)
        if (pushes.get() == pushesBefore) state = fetched

        return state ?: fetched
    }

    override suspend fun browse(): List<GroupSummary> = channel.request(BrowseRequest).groups

    override suspend fun inviteCandidates(query: String): InviteCandidatesResult = channel.request(InviteCandidatesRequest(query))

    override suspend fun create(): CreateResult = channel.request(CreateRequest)

    override suspend fun join(
        groupId: UUID,
        password: String?,
    ): JoinResult = channel.request(JoinRequest(groupId, password))

    override suspend fun leave(): LeaveResult = channel.request(LeaveRequest)

    override suspend fun kick(
        groupId: UUID,
        playerId: UUID,
    ): KickResult = channel.request(KickRequest(groupId, playerId))

    override suspend fun ban(
        groupId: UUID,
        playerId: UUID,
    ): BanResult = channel.request(BanRequest(groupId, playerId))

    override suspend fun unban(
        groupId: UUID,
        playerId: UUID,
    ): UnbanResult = channel.request(UnbanRequest(groupId, playerId))

    override suspend fun transfer(
        groupId: UUID,
        playerId: UUID,
    ): TransferResult = channel.request(TransferRequest(groupId, playerId))

    override suspend fun invite(
        groupId: UUID,
        playerId: UUID,
    ): InviteResult = channel.request(InviteRequest(groupId, playerId))

    override suspend fun cancelInvite(
        groupId: UUID,
        playerId: UUID,
    ): CancelInviteResult = channel.request(CancelInviteRequest(groupId, playerId))

    override suspend fun declineInvite(inviteId: UUID): DeclineInviteResult = channel.request(DeclineInviteRequest(inviteId))

    override suspend fun delete(groupId: UUID): DeleteResult = channel.request(DeleteRequest(groupId))

    override suspend fun updateSettings(request: UpdateSettingsRequest): UpdateSettingsResult = channel.request(request)

    suspend fun handshake(): HandshakeResponse? {
        session = null

        val response = channel.request(HandshakeRequest(PROTOCOL_VERSION, MIN_PROTOCOL_VERSION))

        session =
            response.takeIf { isProtocolCompatible(it.protocolVersion, it.minProtocolVersion) }
                ?: run {
                    logger.warn(
                        "Groups protocol mismatch: server supports {}..{}, this mod supports {}..{}",
                        response.minProtocolVersion,
                        response.protocolVersion,
                        MIN_PROTOCOL_VERSION,
                        PROTOCOL_VERSION,
                    )
                    null
                }

        return session
    }

    fun reset() {
        session = null
        state = null
        channel.reset()
    }
}
