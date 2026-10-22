package su.plo.voice.groups.client.service

import kotlinx.coroutines.delay
import net.minecraft.client.Minecraft
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.groups.protocol.ActionResult
import su.plo.voice.groups.protocol.AlreadyInGroup
import su.plo.voice.groups.protocol.Banned
import su.plo.voice.groups.protocol.CreateResult
import su.plo.voice.groups.protocol.DeclineInviteResult
import su.plo.voice.groups.protocol.DeleteResult
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupDetails
import su.plo.voice.groups.protocol.GroupInvite
import su.plo.voice.groups.protocol.GroupNotFound
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupSummary
import su.plo.voice.groups.protocol.GroupsCapabilities
import su.plo.voice.groups.protocol.GroupsState
import su.plo.voice.groups.protocol.InviteResult
import su.plo.voice.groups.protocol.JoinResult
import su.plo.voice.groups.protocol.JoinState
import su.plo.voice.groups.protocol.LeaveResult
import su.plo.voice.groups.protocol.MemberAction
import su.plo.voice.groups.protocol.MemberActionResult
import su.plo.voice.groups.protocol.NameLengthInvalid
import su.plo.voice.groups.protocol.NoPermission
import su.plo.voice.groups.protocol.NotBanned
import su.plo.voice.groups.protocol.NotInGroup
import su.plo.voice.groups.protocol.PasswordRequired
import su.plo.voice.groups.protocol.PlayerNotFound
import su.plo.voice.groups.protocol.Self
import su.plo.voice.groups.protocol.Success
import su.plo.voice.groups.protocol.UpdateSettingsRequest
import su.plo.voice.groups.protocol.UpdateSettingsResult
import su.plo.voice.groups.protocol.WrongPassword
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds

class MockGroupsService : GroupsService {
    private val lock = Any()
    private val listeners = CopyOnWriteArrayList<GroupsStateListener>()

    private val selfPlayer = Minecraft.getInstance().user.let { Player(it.profileId, it.name) }

    private val selfPermissions =
        setOf(
            "browse",
            "create",
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
            "group.builder",
        )

    private val players = mutableMapOf<UUID, Player>()

    private val groups = mutableListOf<Group>()
    private val invites = mutableListOf<Invite>()

    @Volatile
    override var state: GroupsState? = null
        private set

    override val available: Boolean = true

    override val self: GroupPlayer
        get() = selfPlayer.dto()

    override val voicePlayers: List<GroupPlayer>
        get() = synchronized(lock) { players.values.filter { it.online }.map { it.dto() } }

    init {
        players[selfPlayer.id] = selfPlayer

        val kpids = player("2714d55f-ffef-4655-a93e-d8ca13230e76", "KPidS")
        val venterok = player("cfb727e7-efcc-4596-8c2b-9c6e38c8eea4", "Venterok")
        val gnome = player("78bc6ced-3bc1-4f7f-9ec2-9657f79b05cb", "GNOME__", online = false)
        val longest = player("W".repeat(16))
        val haffk = player("4f3d5a24-6dc1-4743-a7c9-0a2c14d9795a", "haffk", online = false)
        val apehum = player("8f552657-df1d-42cd-89c6-c176e195f703", "Apehum")
        val binero = player("e7d9db44-3268-4409-9777-0227f7390fbd", "Binero")
        val notch = player("069a79f4-44e9-4726-a5be-fca90e38aaf5", "Notch")
        val jeb = player("853c80ef-3c37-49fd-aa49-938b674adae6", "jeb_")
        val dinnerbone = player("61699b2e-d327-4a01-9f1e-0ea8c3f06bc6", "Dinnerbone", online = false)
        val grumm = player("Grumm")
        val kai = player("Kai")
        val moderator = player("Moderator")
        val sunny = player("Sunny", online = false)
        val efe = player("Efe")
        val zuri = player("Zuri")
        val alex = player("Alex")
        val steve = player("Steve")

        listOf("Ari", "Makena", "Noor").forEach { player(it) }
        GENERATED_ADJECTIVES.forEachIndexed { adjectiveIndex, adjective ->
            GENERATED_NOUNS.forEachIndexed { nounIndex, noun ->
                val index = adjectiveIndex * GENERATED_NOUNS.size + nounIndex
                player("$adjective$noun", online = index % 7 != 0)
            }
        }

        fun ids(vararg players: Player) = players.map { it.id }.toMutableList()

        groups +=
            listOf(
                Group(
                    "${selfPlayer.name}'s group",
                    selfPlayer.id,
                    ids(selfPlayer, kpids, venterok, longest, gnome),
                    password = "hunter2",
                    persistent = true,
                    banned = ids(haffk, zuri),
                    sentInvites = ids(binero),
                ),
                Group("Builders", notch.id, ids(notch, alex, steve)),
                Group("Night Shift", jeb.id, ids(jeb, sunny), password = "zzz"),
                Group("Upside Down", dinnerbone.id, ids(dinnerbone), persistent = true),
                Group("Redstone Nerds", kai.id, ids(kai, zuri)),
                Group("Staff", moderator.id, ids(moderator), permissions = mutableSetOf("group.staff")),
                Group("Builder Guild", grumm.id, ids(grumm), permissions = mutableSetOf("group.builder")),
                Group("Server Lounge", null, mutableListOf(), persistent = true),
                Group("Griefers Anonymous", haffk.id, ids(haffk), banned = ids(selfPlayer)),
            )

        val apehumGroup = Group("Apehum's group", apehum.id, ids(apehum, efe))
        invites += Invite(apehumGroup, apehum.id)
        invites += Invite(groups.first { it.name == "Night Shift" }, jeb.id)
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

    override suspend fun browse(): List<GroupSummary> {
        delay(LATENCY)
        return synchronized(lock) {
            if (!has("browse")) return@synchronized emptyList()

            val browseAll = has("browse.all") || has("browse.*")
            groups
                .filter { browseAll || canPassFilter(it) }
                .map { it.summary() }
        }
    }

    override suspend fun create(): CreateResult =
        mutate {
            if (!has("create")) return@mutate NoPermission

            val name = "${selfPlayer.name}'s group"
            if (name.length !in MIN_NAME_LENGTH..MAX_NAME_LENGTH) {
                return@mutate NameLengthInvalid(MIN_NAME_LENGTH, MAX_NAME_LENGTH)
            }

            leaveCurrent()
            groups += Group(name, selfPlayer.id, mutableListOf(selfPlayer.id))
            Success
        }

    override suspend fun join(
        groupId: UUID,
        password: String?,
    ): JoinResult =
        mutate {
            val invite = invites.firstOrNull { it.group.id == groupId }
            val group =
                groups.firstOrNull { it.id == groupId }
                    ?: invite?.group
                    ?: return@mutate GroupNotFound

            if (selfPlayer.id in group.members) return@mutate AlreadyInGroup
            if (!has("join.*")) {
                if (selfPlayer.id in group.banned) return@mutate Banned
                if (!canPassFilter(group) && !has("join.all")) return@mutate NoPermission
                if (group.password != null && invite == null) {
                    if (password == null) return@mutate PasswordRequired
                    if (password != group.password) return@mutate WrongPassword
                }
            }

            leaveCurrent()
            if (group !in groups) groups += group
            group.members += selfPlayer.id
            group.sentInvites -= selfPlayer.id
            invites.removeAll { it.group.id == groupId }
            Success
        }

    override suspend fun leave(): LeaveResult =
        mutate {
            if (!has("leave")) return@mutate NoPermission
            if (!leaveCurrent()) return@mutate NotInGroup
            Success
        }

    override suspend fun memberAction(
        groupId: UUID,
        playerId: UUID,
        action: MemberAction,
    ): MemberActionResult =
        mutate {
            val group = current()?.takeIf { it.id == groupId } ?: return@mutate GroupNotFound
            val required =
                when (action) {
                    MemberAction.CANCEL_INVITE -> GroupAction.INVITE
                    MemberAction.KICK -> GroupAction.KICK
                    MemberAction.BAN -> GroupAction.BAN
                    MemberAction.UNBAN -> GroupAction.UNBAN
                    MemberAction.TRANSFER -> GroupAction.TRANSFER
                }
            if (required !in group.actions()) return@mutate NoPermission
            if (playerId == selfPlayer.id && action != MemberAction.UNBAN) return@mutate Self

            when (action) {
                MemberAction.CANCEL_INVITE -> {
                    if (!group.sentInvites.remove(playerId)) return@mutate PlayerNotFound
                }

                MemberAction.KICK -> {
                    if (!group.members.remove(playerId)) return@mutate NotInGroup
                }

                MemberAction.BAN -> {
                    group.members.remove(playerId)
                    group.sentInvites.remove(playerId)
                    if (playerId !in group.banned) group.banned += playerId
                }

                MemberAction.UNBAN -> {
                    if (!group.banned.remove(playerId)) return@mutate NotBanned
                }

                MemberAction.TRANSFER -> {
                    if (playerId !in group.members) return@mutate NotInGroup
                    group.owner = playerId
                }
            }
            Success
        }

    override suspend fun invite(
        groupId: UUID,
        playerId: UUID,
    ): InviteResult =
        mutate {
            val group = current()?.takeIf { it.id == groupId } ?: return@mutate GroupNotFound
            if (GroupAction.INVITE !in group.actions()) return@mutate NoPermission

            val target =
                players[playerId]?.takeIf { it.online }
                    ?: return@mutate PlayerNotFound

            when {
                target.id == selfPlayer.id -> {
                    Self
                }

                target.id in group.banned -> {
                    Banned
                }

                target.id in group.members -> {
                    AlreadyInGroup
                }

                else -> {
                    if (target.id !in group.sentInvites) group.sentInvites += target.id
                    Success
                }
            }
        }

    override suspend fun declineInvite(inviteId: UUID): DeclineInviteResult =
        mutate {
            if (!invites.removeAll { it.id == inviteId }) return@mutate GroupNotFound
            Success
        }

    override suspend fun delete(groupId: UUID): DeleteResult =
        mutate {
            val group = current()?.takeIf { it.id == groupId } ?: return@mutate GroupNotFound
            if (GroupAction.DELETE !in group.actions()) return@mutate NoPermission

            groups -= group
            invites.removeAll { it.group.id == groupId }
            Success
        }

    override suspend fun updateSettings(request: UpdateSettingsRequest): UpdateSettingsResult =
        mutate {
            val group = current()?.takeIf { it.id == request.groupId } ?: return@mutate GroupNotFound
            val actions = group.actions()

            fun allowed(
                value: Any?,
                action: GroupAction,
            ) = value == null || action in actions

            if (!allowed(request.name, GroupAction.SET_NAME) ||
                !allowed(request.password, GroupAction.SET_PASSWORD) ||
                !allowed(request.permissions, GroupAction.SET_PERMISSIONS) ||
                !allowed(request.persistent, GroupAction.SET_PERSISTENT)
            ) {
                return@mutate NoPermission
            }

            request.name?.let {
                if (it.length !in MIN_NAME_LENGTH..MAX_NAME_LENGTH) {
                    return@mutate NameLengthInvalid(MIN_NAME_LENGTH, MAX_NAME_LENGTH)
                }
            }

            request.name?.let { group.name = it }
            request.password?.let { group.password = it.ifEmpty { null } }
            request.permissions?.let {
                group.permissions.clear()
                group.permissions += it
            }
            request.persistent?.let { group.persistent = it }
            Success
        }

    private fun player(
        name: String,
        online: Boolean = true,
    ): Player = Player(name, online).also { players[it.id] = it }

    private fun player(
        id: String,
        name: String,
        online: Boolean = true,
    ): Player = Player(UUID.fromString(id), name, online).also { players[it.id] = it }

    private suspend fun <R : ActionResult> mutate(block: () -> R): R {
        delay(LATENCY)

        val (result, newState) =
            synchronized(lock) {
                val result = block()
                result to project().also { state = it }
            }
        if (result == Success) listeners.forEach { it(newState) }

        return result
    }

    private fun leaveCurrent(): Boolean {
        val group = current() ?: return false
        group.members -= selfPlayer.id

        if (!group.persistent) {
            if (group.owner == selfPlayer.id) {
                group.owner = group.members.filter { players[it]?.online == true }.randomOrNull()
            }
            if (group.members.none { players[it]?.online == true }) {
                groups -= group
            }
        }

        return true
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

    private fun canPassFilter(group: Group): Boolean = group.permissions.isEmpty() || group.permissions.any(::has)

    private fun current(): Group? = groupOf(selfPlayer.id)

    private fun groupOf(playerId: UUID): Group? = groups.firstOrNull { playerId in it.members }

    private fun Player.dto(): GroupPlayer = GroupPlayer(McGameProfile(id, name, emptyList()), online)

    private fun UUID.dto(): GroupPlayer? = players[this]?.dto()

    private fun Group.joinState(): JoinState =
        when {
            has("join.*") -> JoinState.JOINABLE
            selfPlayer.id in banned -> JoinState.BANNED
            !canPassFilter(this) && !has("join.all") -> JoinState.NO_PERMISSION
            password != null && invites.none { it.group.id == id } -> JoinState.PASSWORD
            else -> JoinState.JOINABLE
        }

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
            joinState(),
        )

    private fun Group.details(): GroupDetails {
        val actions = actions()

        return GroupDetails(
            summary(),
            members.mapNotNull { it.dto() },
            actions,
            password = password.takeIf { GroupAction.SET_PASSWORD in actions },
            permissions = permissions.toSet().takeIf { GroupAction.SET_PERMISSIONS in actions },
            banned = banned.mapNotNull { it.dto() }.takeIf { GroupAction.UNBAN in actions },
            sentInvites = sentInvites.mapNotNull { it.dto() }.takeIf { GroupAction.INVITE in actions },
        )
    }

    private fun project(): GroupsState =
        GroupsState(
            current()?.details(),
            invites.mapNotNull { invite ->
                invite.from.dto()?.let { GroupInvite(invite.id, invite.group.summary(), it) }
            },
            GroupsCapabilities(
                canCreate = has("create"),
                canBrowse = has("browse"),
                canLeave = has("leave"),
                minNameLength = MIN_NAME_LENGTH,
                maxNameLength = MAX_NAME_LENGTH,
            ),
        )

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
        val banned: MutableList<UUID> = mutableListOf(),
        val permissions: MutableSet<String> = mutableSetOf(),
        val sentInvites: MutableList<UUID> = mutableListOf(),
        val id: UUID = UUID.randomUUID(),
    )

    private class Invite(
        val group: Group,
        val from: UUID,
        val id: UUID = UUID.randomUUID(),
    )

    private companion object {
        val LATENCY = 200L.milliseconds
        const val MIN_NAME_LENGTH = 3
        const val MAX_NAME_LENGTH = 32

        val GENERATED_ADJECTIVES =
            listOf("Quiet", "Brave", "Lucky", "Swift", "Sleepy", "Clever", "Rusty", "Mossy", "Frosty")
        val GENERATED_NOUNS =
            listOf("Fox", "Cat", "Golem", "Panda", "Axolotl", "Creeper", "Beacon", "Llama", "Wolf")
    }
}
