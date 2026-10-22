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

/**
 * Nullable fields are sent only when the viewer has the matching [GroupAction]:
 * - [password] for [GroupAction.SET_PASSWORD]
 * - [permissions] for [GroupAction.SET_PERMISSIONS]
 * - [banned] for [GroupAction.UNBAN]
 * - [sentInvites] for [GroupAction.INVITE]
 */
@Serializable
data class GroupDetails(
    val summary: GroupSummary,
    val members: List<GroupPlayer>,
    val actions: Set<GroupAction>,
    val password: String? = null,
    val permissions: Set<String>? = null,
    val banned: List<GroupPlayer>? = null,
    val sentInvites: List<GroupPlayer>? = null,
)

@Serializable
data class GroupInvite(
    @Serializable(with = UUIDByteSerializer::class)
    val id: UUID,
    val group: GroupSummary,
    val from: GroupPlayer,
)

@Serializable
data class GroupsCapabilities(
    val canCreate: Boolean,
    val canBrowse: Boolean,
    val canLeave: Boolean,
    val minNameLength: Int,
    val maxNameLength: Int,
)

@Serializable
@SerialName("state")
data class GroupsState(
    val current: GroupDetails?,
    val invites: List<GroupInvite>,
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
@SerialName("member_action_result")
sealed interface MemberActionResult : ActionResult

@Serializable
@SerialName("invite_result")
sealed interface InviteResult : ActionResult

@Serializable
@SerialName("decline_invite_result")
sealed interface DeclineInviteResult : ActionResult

@Serializable
@SerialName("delete_result")
sealed interface DeleteResult : ActionResult

@Serializable
@SerialName("update_settings_result")
sealed interface UpdateSettingsResult : ActionResult

@Serializable
@SerialName("success")
data object Success :
    CreateResult,
    JoinResult,
    LeaveResult,
    MemberActionResult,
    InviteResult,
    DeclineInviteResult,
    DeleteResult,
    UpdateSettingsResult

@Serializable
@SerialName("no_permission")
data object NoPermission :
    ActionError,
    CreateResult,
    JoinResult,
    LeaveResult,
    MemberActionResult,
    InviteResult,
    DeleteResult,
    UpdateSettingsResult

@Serializable
@SerialName("group_not_found")
data object GroupNotFound :
    ActionError,
    JoinResult,
    MemberActionResult,
    InviteResult,
    DeclineInviteResult,
    DeleteResult,
    UpdateSettingsResult

@Serializable
@SerialName("player_not_found")
data object PlayerNotFound : ActionError, MemberActionResult, InviteResult

@Serializable
@SerialName("not_in_group")
data object NotInGroup : ActionError, LeaveResult

@Serializable
@SerialName("already_in_group")
data object AlreadyInGroup : ActionError, JoinResult, InviteResult

@Serializable
@SerialName("banned")
data object Banned : ActionError, JoinResult, InviteResult

@Serializable
@SerialName("not_banned")
data object NotBanned : ActionError, MemberActionResult

@Serializable
@SerialName("password_required")
data object PasswordRequired : ActionError, JoinResult

@Serializable
@SerialName("wrong_password")
data object WrongPassword : ActionError, JoinResult

@Serializable
@SerialName("self")
data object Self : ActionError, MemberActionResult, InviteResult

@Serializable
@SerialName("name_length_invalid")
data class NameLengthInvalid(
    val min: Int,
    val max: Int,
) : ActionError,
    CreateResult,
    UpdateSettingsResult

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
enum class MemberAction {
    CANCEL_INVITE,
    UNBAN,
}

@Serializable
@SerialName("member_action")
data class MemberActionRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    @Serializable(with = UUIDByteSerializer::class)
    val playerId: UUID,
    val action: MemberAction,
) : ChannelRequest<MemberActionResult>

@Serializable
@SerialName("invite")
data class InviteRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    @Serializable(with = UUIDByteSerializer::class)
    val playerId: UUID,
) : ChannelRequest<InviteResult>

@Serializable
@SerialName("decline_invite")
data class DeclineInviteRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val inviteId: UUID,
) : ChannelRequest<DeclineInviteResult>

@Serializable
@SerialName("delete")
data class DeleteRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
) : ChannelRequest<DeleteResult>

/**
 * Null fields are left unchanged.
 * An empty [password] removes the password, empty [permissions] removes the join filter.
 */
@Serializable
@SerialName("update_settings")
data class UpdateSettingsRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    val name: String? = null,
    val password: String? = null,
    val permissions: Set<String>? = null,
    val persistent: Boolean? = null,
) : ChannelRequest<UpdateSettingsResult>

@Serializable
@SerialName("state_changed")
data class GroupsStateChanged(
    val state: GroupsState,
)
