package su.plo.voice.groups.client.gui

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextStyle
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupSummary

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
