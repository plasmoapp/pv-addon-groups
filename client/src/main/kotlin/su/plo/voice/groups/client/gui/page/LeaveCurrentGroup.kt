package su.plo.voice.groups.client.gui.page

import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.groups.client.gui.GroupsTabWidget
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.client.service.GroupsService
import su.plo.voice.groups.protocol.ActionResult
import su.plo.voice.groups.protocol.GroupDetails
import java.util.UUID

fun <R : ActionResult> GroupsTabWidget.leaveCurrentThen(
    button: GuiAbstractWidget?,
    title: McTextComponent,
    pops: Int = 0,
    onFailure: (R) -> Unit = { toast(it) },
    action: suspend GroupsService.() -> R,
) {
    val current = state?.current
    if (current == null) {
        perform(button, action, onFailure) { if (pops > 0) back(pops) else refresh() }
        return
    }

    open(
        ConfirmPage(
            title,
            listOf(current.leaveConsequence(service.self.id)),
            tr("leave.current.confirm"),
            pops + 1,
            action,
            onFailure,
        ),
    )
}

fun GroupDetails.leaveConsequence(selfId: UUID): McTextComponent {
    val name = summary.name

    return when {
        summary.owner?.id != selfId -> tr("leave.current.member", name)
        summary.persistent -> tr("leave.current.owner_stays", name)
        members.any { it.id != selfId && it.online } -> tr("leave.current.random_owner", name)
        else -> tr("leave.current.deleted", name)
    }
}
