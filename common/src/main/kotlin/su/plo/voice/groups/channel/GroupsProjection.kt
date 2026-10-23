package su.plo.voice.groups.channel

import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.group.GroupImpl
import su.plo.voice.groups.group.GroupsManager
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupDetails
import su.plo.voice.groups.protocol.GroupInvite
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupSummary
import su.plo.voice.groups.protocol.GroupsCapabilities
import su.plo.voice.groups.protocol.GroupsState
import su.plo.voice.groups.protocol.JoinState
import su.plo.voice.groups.util.PlayerCollector
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.hasFlagPermission
import su.plo.voice.groups.util.extend.hasTierPermission
import java.util.UUID

class GroupsProjection(
    private val groupManager: GroupsManager,
    private val playerCollector: PlayerCollector,
) {
    fun state(viewer: VoicePlayer): GroupsState {
        val source = viewer.instance
        val visible = visiblePlayers(viewer)
        val config = groupManager.config

        return GroupsState(
            groupManager.groupByPlayer[source.uuid]
                ?.let { details(it, viewer, Presence(it, visible)) },
            groupManager
                .getPlayerInvites(source.uuid)
                .mapNotNull { invite ->
                    val group = groupManager.groupsById[invite.groupId] ?: return@mapNotNull null
                    val presence = Presence(group, visible)

                    GroupInvite(invite.id, summary(group, viewer, presence), presence.player(invite.inviter))
                },
            GroupsCapabilities(
                canCreate = source.hasAddonPermission("create"),
                canBrowse = source.hasAddonPermission("browse"),
                canLeave = source.hasAddonPermission("leave"),
                minNameLength = config.minimumNameLength,
                maxNameLength = config.maximumNameLength,
            ),
        )
    }

    fun browse(viewer: VoicePlayer): List<GroupSummary> {
        val source = viewer.instance
        if (!source.hasAddonPermission("browse")) return emptyList()

        val browseAll = source.hasAddonPermission("browse.all") || source.hasAddonPermission("browse.*")
        val visible = visiblePlayers(viewer)

        return groupManager.groupsById.values
            .filter { browseAll || it.hasPermission(source) }
            .map { summary(it, viewer, Presence(it, visible)) }
            .sortedWith(compareByDescending<GroupSummary> { it.onlineCount }.thenBy { it.name.lowercase() })
    }

    fun actions(
        group: GroupImpl,
        viewer: VoicePlayer,
    ): Set<GroupAction> {
        val source = viewer.instance
        val isOwner = group.isOwner(viewer)

        fun tier(
            permission: String,
            othersTier: String = "all",
        ) = source.hasTierPermission(permission, isOwner, othersTier)

        val canSet = tier("set")

        return buildSet {
            if (tier("invite", othersTier = "member")) add(GroupAction.INVITE)
            if (tier("kick")) add(GroupAction.KICK)
            if (tier("ban")) {
                add(GroupAction.BAN)
                add(GroupAction.UNBAN)
            }
            if (tier("transfer")) add(GroupAction.TRANSFER)
            if (tier("delete")) add(GroupAction.DELETE)
            if (canSet && source.hasFlagPermission("name")) add(GroupAction.SET_NAME)
            if (canSet && source.hasFlagPermission("password")) add(GroupAction.SET_PASSWORD)
            if (canSet && source.hasFlagPermission("permissions")) add(GroupAction.SET_PERMISSIONS)
            if (canSet && source.hasFlagPermission("persistent")) add(GroupAction.SET_PERSISTENT)
        }
    }

    private fun details(
        group: GroupImpl,
        viewer: VoicePlayer,
        presence: Presence,
    ): GroupDetails {
        val actions = actions(group, viewer)

        return GroupDetails(
            summary(group, viewer, presence),
            group.players.map(presence::player),
            actions,
            password = group.password.takeIf { GroupAction.SET_PASSWORD in actions },
            permissions = group.permissionsFilter.toSet().takeIf { GroupAction.SET_PERMISSIONS in actions },
            banned =
                if (GroupAction.UNBAN in actions) {
                    group.bannedPlayers.map(presence::player)
                } else {
                    null
                },
            sentInvites =
                if (GroupAction.INVITE in actions) {
                    group.invites.map { presence.player(it.target) }
                } else {
                    null
                },
        )
    }

    private fun summary(
        group: GroupImpl,
        viewer: VoicePlayer,
        presence: Presence,
    ): GroupSummary =
        GroupSummary(
            group.id,
            group.name,
            group.owner?.let(presence::player),
            group.password != null,
            group.persistent,
            group.permissionsFilter.isNotEmpty(),
            presence.onlineMembers.size,
            group.playerCount,
            joinState(group, viewer),
        )

    private fun joinState(
        group: GroupImpl,
        viewer: VoicePlayer,
    ): JoinState {
        val source = viewer.instance

        return when {
            source.hasAddonPermission("join.*") -> JoinState.JOINABLE
            group.isBanned(source.uuid) -> JoinState.BANNED
            !source.hasAddonPermission("join") -> JoinState.NO_PERMISSION
            !source.hasAddonPermission("join.all") && !group.hasPermission(source) -> JoinState.NO_PERMISSION
            group.password != null && group.getInvite(source.uuid) == null -> JoinState.PASSWORD
            else -> JoinState.JOINABLE
        }
    }

    private fun visiblePlayers(viewer: VoicePlayer): Set<UUID> =
        playerCollector
            .getVisibleOnlinePlayers(viewer)
            .filter { it.hasVoiceChat() }
            .mapTo(HashSet()) { it.instance.uuid }

    private class Presence(
        private val group: GroupImpl,
        private val visible: Set<UUID>,
    ) {
        val onlineMembers: Set<UUID> =
            group.onlinePlayers.mapNotNullTo(HashSet()) { player ->
                player.instance.uuid.takeIf { it in visible }
            }

        fun player(profile: McGameProfile): GroupPlayer {
            val online =
                if (profile.id in group.playersById) {
                    profile.id in onlineMembers
                } else {
                    profile.id in visible
                }

            return GroupPlayer(profile, online)
        }
    }
}
