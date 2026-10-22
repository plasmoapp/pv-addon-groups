package su.plo.voice.groups.client.gui.page

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.client.service.GroupsService
import su.plo.voice.groups.protocol.ActionResult

class ConfirmPage<R : ActionResult>(
    private val title: McTextComponent,
    private val message: List<McTextComponent>,
    private val confirmLabel: McTextComponent,
    private val pops: Int,
    private val action: (suspend GroupsService.() -> R)?,
    private val onFailure: ((R) -> Unit)? = null,
) : GroupsPage() {
    override fun init() {
        addCategory(title)
        add(tab.MessageEntry(message))
        add(tab.SpacerEntry(8))
        addRow(
            button(tr("cancel"), width = 0) { tab.back() },
            button(confirmLabel, width = 0) { button ->
                val action = action ?: return@button tab.back(pops)

                tab.perform(
                    button,
                    action,
                    onFailure = { result ->
                        tab.back()
                        onFailure?.invoke(result) ?: tab.toast(result)
                    },
                ) { tab.back(pops) }
            },
        )
    }
}
