package su.plo.voice.groups.client.gui.page

import su.plo.voice.groups.client.gui.text
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupsState
import java.util.UUID

class BannedPage(
    private val groupId: UUID,
) : GroupsPage() {
    override fun isValid(state: GroupsState): Boolean =
        state.current?.let { it.summary.id == groupId && GroupAction.UNBAN in it.actions } == true

    override fun init() {
        val page = addCategory(tr("banned.title"), leading = listOf(backButton()))

        val banned = state.current?.banned.orEmpty()
        if (banned.isEmpty()) {
            page.add(hint(tr("banned.empty")))
            return
        }

        banned.forEach { player ->
            val unban =
                button(tr("banned.unban")) { button ->
                    perform(button) { unban(groupId, player.id) }
                }
            page.add(
                card(
                    player,
                    text(player.name),
                    null,
                    listOf(unban),
                ),
            )
        }
    }
}
