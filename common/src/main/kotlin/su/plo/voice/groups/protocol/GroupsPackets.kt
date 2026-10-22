package su.plo.voice.groups.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.groups.util.serializer.McGameProfileNetworkSerializer
import su.plo.voice.groups.util.serializer.UUIDByteSerializer
import java.util.UUID

@Serializable
data class GroupPlayer(
    @Serializable(with = McGameProfileNetworkSerializer::class)
    val profile: McGameProfile,
    val online: Boolean,
) {
    val id: UUID get() = profile.id
    val name: String get() = profile.name
}

@Serializable
enum class GroupAction {
    INVITE,
    KICK,
    BAN,
    UNBAN,
    TRANSFER,
    DELETE,
    SET_NAME,
    SET_PASSWORD,
    SET_PERMISSIONS,
    SET_PERSISTENT,
}

@Serializable
data class GroupSummary(
    @Serializable(with = UUIDByteSerializer::class)
    val id: UUID,
    val name: String,
    val owner: GroupPlayer?,
    val hasPassword: Boolean,
    val persistent: Boolean,
    val restricted: Boolean,
    val onlineCount: Int,
    val playerCount: Int,
)

@Serializable
data class GroupDetails(
    val summary: GroupSummary,
    val members: List<GroupPlayer>,
    val actions: Set<GroupAction>,
)

@Serializable
data class GroupsCapabilities(
    val canLeave: Boolean,
)

@Serializable
@SerialName("state")
data class GroupsState(
    val current: GroupDetails?,
    val capabilities: GroupsCapabilities,
)

@Serializable
@SerialName("state_request")
data object StateRequest : ChannelRequest<GroupsState>

@Serializable
@SerialName("state_changed")
data class GroupsStateChanged(
    val state: GroupsState,
)
