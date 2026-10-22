package su.plo.voice.groups.listener

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group
import su.plo.voice.groups.api.event.GroupInviteEvent
import su.plo.voice.groups.api.event.GroupNameChangedEvent
import su.plo.voice.groups.api.event.GroupOwnerChangedEvent
import su.plo.voice.groups.api.event.GroupPasswordChangedEvent
import su.plo.voice.groups.api.event.GroupPermissionsChangedEvent
import su.plo.voice.groups.api.event.GroupPersistentChangedEvent
import su.plo.voice.groups.api.event.LeaveReason
import su.plo.voice.groups.api.event.PlayerBannedFromGroupEvent
import su.plo.voice.groups.api.event.PlayerJoinedGroupEvent
import su.plo.voice.groups.api.event.PlayerLeftGroupEvent
import su.plo.voice.groups.api.event.PlayerUnbannedFromGroupEvent
import su.plo.voice.groups.util.extend.inlineChatComponent
import su.plo.voice.groups.util.extend.inviteJoinButton
import su.plo.voice.groups.util.extend.printDivider
import su.plo.voice.groups.util.extend.sendTranslatable
import java.util.UUID

object GroupsNotificationListener {
    fun register() {
        PlayerJoinedGroupEvent.registerListener(::onPlayerJoinedGroup)
        PlayerLeftGroupEvent.registerListener(::onPlayerLeftGroup)
        PlayerBannedFromGroupEvent.registerListener(::onPlayerBannedFromGroup)
        PlayerUnbannedFromGroupEvent.registerListener(::onPlayerUnbannedFromGroup)
        GroupOwnerChangedEvent.registerListener(::onGroupOwnerChanged)
        GroupNameChangedEvent.registerListener(::onGroupNameChanged)
        GroupPasswordChangedEvent.registerListener(::onGroupPasswordChanged)
        GroupPermissionsChangedEvent.registerListener(::onGroupPermissionsChanged)
        GroupPersistentChangedEvent.registerListener(::onGroupPersistentChanged)
        GroupInviteEvent.registerListener(::onGroupInvite)
    }

    private fun onPlayerJoinedGroup(
        group: Group,
        player: VoicePlayer,
    ) {
        group.broadcastToOthers(
            player.instance.uuid,
            "pv.addon.groups.notifications.player_join",
            player.instance.name,
        )
    }

    private fun onPlayerLeftGroup(
        group: Group,
        player: VoicePlayer,
        reason: LeaveReason,
    ) {
        val name = player.instance.name
        when (reason) {
            LeaveReason.LEFT -> {
                group.broadcast("pv.addon.groups.notifications.player_left", name)
            }

            LeaveReason.KICKED, LeaveReason.PERMISSIONS_REVOKED -> {
                group.broadcast("pv.addon.groups.notifications.player_kicked", name)
                player.instance.sendTranslatable("pv.addon.groups.notifications.kicked")
            }

            LeaveReason.BANNED -> {
                // covered by PlayerBannedFromGroupEvent
            }

            LeaveReason.GROUP_DELETED -> {
                player.instance.sendTranslatable("pv.addon.groups.notifications.group_deleted")
            }
        }
    }

    private fun onPlayerBannedFromGroup(
        group: Group,
        player: VoicePlayer,
        wasMember: Boolean,
    ) {
        group.broadcast("pv.addon.groups.notifications.player_banned", player.instance.name)
        if (wasMember) {
            player.instance.sendTranslatable("pv.addon.groups.notifications.banned", group.inlineChatComponent())
        }
    }

    private fun onPlayerUnbannedFromGroup(
        group: Group,
        profile: McGameProfile,
    ) {
        group.broadcast("pv.addon.groups.notifications.player_unbanned", profile.name)
    }

    private fun onGroupOwnerChanged(
        group: Group,
        oldOwner: McGameProfile?,
        newOwner: McGameProfile?,
    ) {
        newOwner?.let {
            group.broadcast("pv.addon.groups.notifications.new_owner", it.name)
        }
    }

    private fun onGroupNameChanged(
        group: Group,
        oldName: String,
        newName: String,
    ) {
        group.broadcast("pv.addon.groups.notifications.name_change", oldName, newName)
    }

    private fun onGroupPasswordChanged(
        group: Group,
        oldPassword: String?,
        newPassword: String?,
    ) {
        val key =
            if (newPassword != null) {
                "pv.addon.groups.notifications.password_changed"
            } else {
                "pv.addon.groups.notifications.password_unset"
            }
        group.broadcast(key)
    }

    private fun onGroupPermissionsChanged(
        group: Group,
        oldPermissions: Set<String>,
        newPermissions: Set<String>,
    ) {
        val key =
            if (newPermissions.isNotEmpty()) {
                "pv.addon.groups.notifications.permissions_changed"
            } else {
                "pv.addon.groups.notifications.permissions_unset"
            }
        group.broadcast(key)
    }

    private fun onGroupPersistentChanged(
        group: Group,
        persistent: Boolean,
    ) {
        val key =
            if (persistent) {
                "pv.addon.groups.notifications.persistent_true"
            } else {
                "pv.addon.groups.notifications.persistent_false"
            }
        group.broadcast(key)
    }

    private fun onGroupInvite(
        group: Group,
        inviter: VoicePlayer,
        target: VoicePlayer,
    ) {
        val targetPlayer = target.instance
        targetPlayer.printDivider()
        targetPlayer.sendTranslatable("pv.addon.groups.format.invite", inviter.instance.name, group.inlineChatComponent())
        targetPlayer.sendMessage(group.inviteJoinButton())
        targetPlayer.printDivider()
    }

    private fun Group.broadcast(
        key: String,
        vararg args: Any,
    ) {
        val component =
            McTextComponent
                .empty()
                .append(McTextComponent.translatable("pv.addon.groups.format.group_name", name))
                .append(McTextComponent.literal(" "))
                .append(McTextComponent.translatable(key, *args))
        onlinePlayers.forEach { it.instance.sendMessage(component) }
    }

    private fun Group.broadcastToOthers(
        excludeUuid: UUID,
        key: String,
        vararg args: Any,
    ) {
        val component =
            McTextComponent
                .empty()
                .append(McTextComponent.translatable("pv.addon.groups.format.group_name", name))
                .append(McTextComponent.literal(" "))
                .append(McTextComponent.translatable(key, *args))
        onlinePlayers
            .filter { it.instance.uuid != excludeUuid }
            .forEach { it.instance.sendMessage(component) }
    }
}
