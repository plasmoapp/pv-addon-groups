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
    val remoteInviteSearch: Boolean,
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
@SerialName("kick_result")
sealed interface KickResult : ActionResult

@Serializable
@SerialName("ban_result")
sealed interface BanResult : ActionResult

@Serializable
@SerialName("unban_result")
sealed interface UnbanResult : ActionResult

@Serializable
@SerialName("transfer_result")
sealed interface TransferResult : ActionResult

@Serializable
@SerialName("invite_result")
sealed interface InviteResult : ActionResult

@Serializable
@SerialName("cancel_invite_result")
sealed interface CancelInviteResult : ActionResult

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
    KickResult,
    BanResult,
    UnbanResult,
    TransferResult,
    InviteResult,
    CancelInviteResult,
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
    KickResult,
    BanResult,
    UnbanResult,
    TransferResult,
    InviteResult,
    CancelInviteResult,
    DeleteResult,
    UpdateSettingsResult,
    InviteCandidatesResult

@Serializable
@SerialName("group_not_found")
data object GroupNotFound :
    ActionError,
    JoinResult,
    KickResult,
    BanResult,
    UnbanResult,
    TransferResult,
    InviteResult,
    CancelInviteResult,
    DeclineInviteResult,
    DeleteResult,
    UpdateSettingsResult

@Serializable
@SerialName("player_not_found")
data object PlayerNotFound : ActionError, BanResult, InviteResult, CancelInviteResult

@Serializable
@SerialName("not_in_group")
data object NotInGroup : ActionError, LeaveResult, KickResult, TransferResult, InviteCandidatesResult

@Serializable
@SerialName("already_in_group")
data object AlreadyInGroup : ActionError, JoinResult, InviteResult

@Serializable
@SerialName("banned")
data object Banned : ActionError, JoinResult, InviteResult

@Serializable
@SerialName("not_banned")
data object NotBanned : ActionError, UnbanResult

@Serializable
@SerialName("password_required")
data object PasswordRequired : ActionError, JoinResult

@Serializable
@SerialName("wrong_password")
data object WrongPassword : ActionError, JoinResult

@Serializable
@SerialName("self")
data object Self : ActionError, KickResult, BanResult, InviteResult

@Serializable
@SerialName("owner")
data object Owner : ActionError, BanResult, TransferResult

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

const val MAX_INVITE_CANDIDATES = 50

const val MIN_INVITE_QUERY_LENGTH = 2

/**
 * Handled only by the proxy addon; sent only when [GroupsCapabilities.remoteInviteSearch] is set.
 *
 * A [query] shorter than [MIN_INVITE_QUERY_LENGTH] matches only players on the requester's server and pending invites.
 */
@Serializable
@SerialName("invite_candidates")
data class InviteCandidatesRequest(
    val query: String,
) : ChannelRequest<InviteCandidatesResult>

@Serializable
@SerialName("invite_candidates_result")
sealed interface InviteCandidatesResult

@Serializable
@SerialName("candidates")
data class InviteCandidates(
    val players: List<GroupPlayer>,
    val hasMore: Boolean,
) : InviteCandidatesResult

@Serializable
@SerialName("rate_limited")
data class RateLimited(
    val retryAfterMs: Long,
) : InviteCandidatesResult

@Serializable
@SerialName("create")
data object CreateRequest : ChannelRequest<CreateResult>

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
@SerialName("kick")
data class KickRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    @Serializable(with = UUIDByteSerializer::class)
    val playerId: UUID,
) : ChannelRequest<KickResult>

@Serializable
@SerialName("ban")
data class BanRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    @Serializable(with = UUIDByteSerializer::class)
    val playerId: UUID,
) : ChannelRequest<BanResult>

@Serializable
@SerialName("unban")
data class UnbanRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    @Serializable(with = UUIDByteSerializer::class)
    val playerId: UUID,
) : ChannelRequest<UnbanResult>

@Serializable
@SerialName("transfer")
data class TransferRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    @Serializable(with = UUIDByteSerializer::class)
    val playerId: UUID,
) : ChannelRequest<TransferResult>

@Serializable
@SerialName("invite")
data class InviteRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    @Serializable(with = UUIDByteSerializer::class)
    val playerId: UUID,
) : ChannelRequest<InviteResult>

@Serializable
@SerialName("cancel_invite")
data class CancelInviteRequest(
    @Serializable(with = UUIDByteSerializer::class)
    val groupId: UUID,
    @Serializable(with = UUIDByteSerializer::class)
    val playerId: UUID,
) : ChannelRequest<CancelInviteResult>

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
