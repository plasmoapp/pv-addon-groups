package su.plo.voice.groups.client.gui

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextStyle
import su.plo.voice.groups.client.service.GroupsStatus
import su.plo.voice.groups.protocol.ActionError
import su.plo.voice.groups.protocol.AlreadyInGroup
import su.plo.voice.groups.protocol.Banned
import su.plo.voice.groups.protocol.GroupNotFound
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupSummary
import su.plo.voice.groups.protocol.NameLengthInvalid
import su.plo.voice.groups.protocol.NoPermission
import su.plo.voice.groups.protocol.NotBanned
import su.plo.voice.groups.protocol.NotInGroup
import su.plo.voice.groups.protocol.Owner
import su.plo.voice.groups.protocol.PasswordRequired
import su.plo.voice.groups.protocol.PlayerNotFound
import su.plo.voice.groups.protocol.Self
import su.plo.voice.groups.protocol.WrongPassword

fun tr(
    key: String,
    vararg args: Any,
): McTextComponent = McTextComponent.translatable("gui.pv_addon_groups.$key", *args)

fun text(value: String): McTextComponent = McTextComponent.literal(value)

fun McTextComponent.gray(): McTextComponent = withStyle(McTextStyle.GRAY)

fun onlineStatus(player: GroupPlayer): McTextComponent =
    if (player.online) {
        tr("status.online").withStyle(McTextStyle.GREEN)
    } else {
        tr("status.offline").gray()
    }

fun groupStatus(group: GroupSummary): McTextComponent {
    val online = tr("group.online", group.onlineCount, group.playerCount)

    val flags =
        listOfNotNull(
            tr("group.flag.password").takeIf { group.hasPassword },
            tr("group.flag.persistent").takeIf { group.persistent },
            tr("group.flag.restricted").takeIf { group.restricted },
        )

    val status =
        if (flags.isEmpty()) {
            online
        } else {
            tr("group.status_flags", online, joinComponents(flags))
        }

    return status.gray()
}

fun addonStatusTooltip(status: GroupsStatus): McTextComponent =
    when (status) {
        GroupsStatus.SERVER_OUTDATED -> tr("outdated.server")
        GroupsStatus.CLIENT_OUTDATED -> tr("outdated.client")
        else -> tr("groups")
    }

fun ActionError.message(): McTextComponent =
    when (this) {
        NoPermission -> tr("error.no_permission")
        GroupNotFound -> tr("error.group_not_found")
        PlayerNotFound -> tr("error.player_not_found")
        NotInGroup -> tr("error.not_in_group")
        NotBanned -> tr("error.not_banned")
        AlreadyInGroup -> tr("error.already_in_group")
        Banned -> tr("error.banned")
        PasswordRequired -> tr("error.password_required")
        WrongPassword -> tr("error.wrong_password")
        Self -> tr("error.self")
        Owner -> tr("error.owner")
        is NameLengthInvalid -> tr("error.name_length_invalid", min, max)
    }

fun joinComponents(components: List<McTextComponent>): McTextComponent =
    McTextComponent.empty().append(
        components.flatMapIndexed { index, component ->
            if (index == 0) {
                listOf(component)
            } else {
                listOf(tr("list_separator"), component)
            }
        },
    )
