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
enum class JoinState {
    JOINABLE,
    PASSWORD,
    NO_PERMISSION,
    BANNED,
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
    val joinState: JoinState,
)

@Serializable
data class GroupDetails(
    val summary: GroupSummary,
    val members: List<GroupPlayer>,
    val actions: Set<GroupAction>,
)

@Serializable
data class GroupsCapabilities(
    val canCreate: Boolean,
    val canBrowse: Boolean,
    val canLeave: Boolean,
)

@Serializable
@SerialName("state")
data class GroupsState(
    val current: GroupDetails?,
    val capabilities: GroupsCapabilities,
)

@Serializable
sealed interface ActionResult

sealed interface ActionError : ActionResult

@Serializable
@SerialName("create_result")
sealed interface CreateResult : ActionResult

@Serializable
@SerialName("join_result")
sealed interface JoinResult : ActionResult

@Serializable
@SerialName("leave_result")
sealed interface LeaveResult : ActionResult

@Serializable
@SerialName("success")
data object Success : CreateResult, JoinResult, LeaveResult

@Serializable
@SerialName("no_permission")
data object NoPermission : ActionError, CreateResult, JoinResult, LeaveResult

@Serializable
@SerialName("group_not_found")
data object GroupNotFound : ActionError, JoinResult

@Serializable
@SerialName("not_in_group")
data object NotInGroup : ActionError, LeaveResult

@Serializable
@SerialName("already_in_group")
data object AlreadyInGroup : ActionError, JoinResult

@Serializable
@SerialName("banned")
data object Banned : ActionError, JoinResult

@Serializable
@SerialName("password_required")
data object PasswordRequired : ActionError, JoinResult

@Serializable
@SerialName("wrong_password")
data object WrongPassword : ActionError, JoinResult

@Serializable
@SerialName("name_length_invalid")
data class NameLengthInvalid(
    val min: Int,
    val max: Int,
) : ActionError,
    CreateResult

@Serializable
@SerialName("state_request")
data object StateRequest : ChannelRequest<GroupsState>

@Serializable
@SerialName("browse")
data object BrowseRequest : ChannelRequest<BrowseResponse>

@Serializable
@SerialName("browse_response")
data class BrowseResponse(
    val groups: List<GroupSummary>,
)

@Serializable
@SerialName("create")
data class CreateRequest(
    val name: String? = null,
) : ChannelRequest<CreateResult>

@Serializable
@SerialName("join")
data class JoinRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    val password: String? = null,
) : ChannelRequest<JoinResult>

@Serializable
@SerialName("leave")
data object LeaveRequest : ChannelRequest<LeaveResult>

@Serializable
@SerialName("state_changed")
data class GroupsStateChanged(
    val state: GroupsState,
)
