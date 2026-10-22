package su.plo.voice.groups.client.service

import kotlinx.coroutines.delay
import net.minecraft.client.Minecraft
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupDetails
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupSummary
import su.plo.voice.groups.protocol.GroupsCapabilities
import su.plo.voice.groups.protocol.GroupsState
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

class MockGroupsService : GroupsService {
    private class Player(
        val id: UUID,
        val name: String,
        var online: Boolean = true,
    ) {
        constructor(name: String, online: Boolean = true) :
            this(UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray()), name, online)
    }

    private class Group(
        var name: String,
        var owner: UUID?,
        val members: MutableList<UUID>,
        var password: String? = null,
        var persistent: Boolean = false,
        val permissions: MutableSet<String> = mutableSetOf(),
        val id: UUID = UUID.randomUUID(),
    )

    private val lock = Any()
    private val listeners = CopyOnWriteArrayList<GroupsStateListener>()

    private val selfPlayer = Minecraft.getInstance().user.let { Player(it.profileId, it.name) }

    private val selfPermissions =
        setOf(
            "leave",
            "invite.owner",
            "invite.member",
            "set.owner",
            "delete.owner",
            "transfer.owner",
            "kick.owner",
            "ban.owner",
            "flag.name",
            "flag.password",
            "flag.persistent",
            "flag.permissions",
        )

    private val players = mutableMapOf<UUID, Player>()

    private fun player(
        name: String,
        online: Boolean = true,
    ): Player = Player(name, online).also { players[it.id] = it }

    private fun player(
        id: String,
        name: String,
        online: Boolean = true,
    ): Player = Player(UUID.fromString(id), name, online).also { players[it.id] = it }

    private val groups = mutableListOf<Group>()

    @Volatile
    override var state: GroupsState? = null
        private set

    override val available: Boolean = true

    override val self: GroupPlayer
        get() = selfPlayer.dto()

    init {
        players[selfPlayer.id] = selfPlayer

        val kpids = player("2714d55f-ffef-4655-a93e-d8ca13230e76", "KPidS")
        val venterok = player("cfb727e7-efcc-4596-8c2b-9c6e38c8eea4", "Venterok")
        val gnome = player("78bc6ced-3bc1-4f7f-9ec2-9657f79b05cb", "GNOME__", online = false)
        val longest = player("W".repeat(16))

        fun ids(vararg players: Player) = players.map { it.id }.toMutableList()

        groups +=
            Group(
                "${selfPlayer.name}'s group",
                selfPlayer.id,
                ids(selfPlayer, kpids, venterok, longest, gnome),
                password = "hunter2",
                persistent = true,
            )
    }

    override fun addListener(listener: GroupsStateListener) {
        listeners += listener
    }

    override fun removeListener(listener: GroupsStateListener) {
        listeners -= listener
    }

    override suspend fun fetchState(): GroupsState {
        delay(LATENCY)
        return synchronized(lock) { project().also { state = it } }
    }

    private fun has(permission: String): Boolean = permission in selfPermissions

    private fun tier(
        command: String,
        group: Group,
        othersTier: String? = null,
    ): Boolean =
        has("$command.*") ||
            has("$command.all") ||
            (group.owner == selfPlayer.id && has("$command.owner")) ||
            (othersTier != null && selfPlayer.id in group.members && has("$command.$othersTier"))

    private fun Group.actions(): Set<GroupAction> {
        val set = tier("set", this)

        return buildSet {
            if (tier("invite", this@actions, othersTier = "member")) add(GroupAction.INVITE)
            if (tier("kick", this@actions)) add(GroupAction.KICK)
            if (tier("ban", this@actions)) {
                add(GroupAction.BAN)
                add(GroupAction.UNBAN)
            }
            if (tier("transfer", this@actions)) add(GroupAction.TRANSFER)
            if (tier("delete", this@actions)) add(GroupAction.DELETE)
            if (set && has("flag.name")) add(GroupAction.SET_NAME)
            if (set && has("flag.password")) add(GroupAction.SET_PASSWORD)
            if (set && has("flag.permissions")) add(GroupAction.SET_PERMISSIONS)
            if (set && has("flag.persistent")) add(GroupAction.SET_PERSISTENT)
        }
    }

    private fun current(): Group? = groupOf(selfPlayer.id)

    private fun groupOf(playerId: UUID): Group? = groups.firstOrNull { playerId in it.members }

    private fun Player.dto(): GroupPlayer = GroupPlayer(McGameProfile(id, name, emptyList()), online)

    private fun UUID.dto(): GroupPlayer? = players[this]?.dto()

    private fun Group.summary(): GroupSummary =
        GroupSummary(
            id,
            name,
            owner?.dto(),
            password != null,
            persistent,
            permissions.isNotEmpty(),
            members.count { players[it]?.online == true },
            members.size,
        )

    private fun Group.details(): GroupDetails =
        GroupDetails(
            summary(),
            members.mapNotNull { it.dto() },
            actions(),
        )

    private fun project(): GroupsState =
        GroupsState(
            current()?.details(),
            GroupsCapabilities(
                canLeave = has("leave"),
            ),
        )

    private companion object {
        val LATENCY = 200L.milliseconds
    }
}
