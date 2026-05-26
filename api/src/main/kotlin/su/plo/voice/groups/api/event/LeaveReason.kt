package su.plo.voice.groups.api.event

/** The reason a player left a group. */
enum class LeaveReason {
    LEFT,
    KICKED,
    BANNED,
    PERMISSIONS_REVOKED,
    GROUP_DELETED,
}
