package su.plo.voice.groups.api.event

/** The reason a pending invite was removed. */
enum class InviteRemoveReason {
    /** The invited player joined the group. */
    ACCEPTED,

    /** The invited player declined the invite. */
    DECLINED,

    /** The invite was cancelled. */
    CANCELLED,

    /** The invited player was banned from the group. */
    BANNED,

    /** The invited player left the server. */
    PLAYER_QUIT,

    /** The group was deleted. */
    GROUP_DELETED,
}
