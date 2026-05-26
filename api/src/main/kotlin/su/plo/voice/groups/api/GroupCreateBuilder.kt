package su.plo.voice.groups.api

import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.result.CreateResult
import java.util.function.Function

/** Builder for a new group, obtained via [GroupsApi.createGroup]. */
class GroupCreateBuilder(
    private val builder: Function<GroupCreateBuilder, CreateResult>,
) {
    /** The group owner, or `null` for an ownerless group. */
    var creator: VoicePlayer? = null
        private set

    /** The group name, or `null` to use the default name format. */
    var name: String? = null
        private set

    /** The group password, or `null` for no password. */
    var password: String? = null
        private set

    /** The group permission filter, or `null` for no filter. */
    var permissions: Set<String>? = null
        private set

    /** Whether the group persists across restarts. `false` by default. */
    var persistent: Boolean = false
        private set

    /**
     * Sets [creator].
     *
     * @param creator the group owner.
     */
    fun creator(creator: VoicePlayer) = apply { this.creator = creator }

    /**
     * Sets [name].
     *
     * @param name the group name.
     */
    fun name(name: String) = apply { this.name = name }

    /**
     * Sets [password].
     *
     * @param password the group password.
     */
    fun password(password: String) = apply { this.password = password }

    /**
     * Sets [permissions].
     *
     * @param permissions the group permission filter.
     */
    fun permissions(permissions: Set<String>) = apply { this.permissions = permissions }

    /**
     * Sets [persistent].
     *
     * @param persistent whether the group persists across restarts.
     */
    fun persistent(persistent: Boolean) = apply { this.persistent = persistent }

    /** Creates the group. */
    fun create(): CreateResult = builder.apply(this)
}
