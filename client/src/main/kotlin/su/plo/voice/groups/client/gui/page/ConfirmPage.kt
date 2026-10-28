package su.plo.voice.groups.client.gui.page

import su.plo.lib.mod.client.gui.components.CompositeColumnWidget
import su.plo.lib.mod.client.gui.components.MultilineTextWidget
import su.plo.lib.mod.client.render.Colors
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
        val page = addCategory(title)
        page.add(
            CompositeColumnWidget(ACTIONS_GAP).apply {
                add(MultilineTextWidget(message, Colors.WHITE, true))
                add(
                    row(
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
                    ),
                )
            },
        )
    }

    private companion object {
        const val ACTIONS_GAP = 10
    }
}
