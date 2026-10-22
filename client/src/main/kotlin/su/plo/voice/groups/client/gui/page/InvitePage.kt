package su.plo.voice.groups.client.gui.page

import su.plo.slib.api.chat.style.McTextStyle
import su.plo.voice.groups.client.gui.text
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.client.gui.widget.SearchFilter
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupsState
import su.plo.voice.groups.protocol.MemberAction
import java.util.UUID

class InvitePage(
    private val groupId: UUID,
) : GroupsPage() {
    private val search = SearchFilter()

    override fun isValid(state: GroupsState): Boolean =
        state.current?.let { it.summary.id == groupId && GroupAction.INVITE in it.actions } == true

    override fun init() {
        val group = checkNotNull(state.current)
        val members = group.members.map { it.id }
        val invited = group.sentInvites.orEmpty()
        val invitedIds = invited.map { it.id }
        val bannedIds = group.banned.orEmpty().map { it.id }
        val candidates =
            (invited + service.voicePlayers)
                .distinctBy { it.id }
                .filter { it.id !in members }
                .sortedBy { it.name.lowercase() }

        addHeader(tr("invite.title"))
        addRow(search.field(tr("invite.search")))

        if (candidates.isEmpty()) addHint(tr("invite.nobody"))
        candidates.forEach { addCandidate(it, it.id in invitedIds, it.id in bannedIds) }
    }

    private fun addCandidate(
        player: GroupPlayer,
        invited: Boolean,
        banned: Boolean,
    ) {
        val width = fitWidth(tr("invite"), tr("cancel"))
        val action =
            when {
                banned -> {
                    disabledButton(tr("invite"), tr("invite.banned"), width)
                }

                invited -> {
                    button(tr("cancel"), width) { button ->
                        perform(button) { memberAction(groupId, player.id, MemberAction.CANCEL_INVITE) }
                    }
                }

                else -> {
                    button(tr("invite"), width) { button ->
                        perform(button) { invite(groupId, player.id) }
                    }
                }
            }
        val status =
            when {
                banned -> tr("status.banned").withStyle(McTextStyle.RED)
                invited -> tr("status.invited").withStyle(McTextStyle.YELLOW)
                else -> null
            }

        add(
            search.track(
                player.name,
                tab.CardEntry(
                    player,
                    text(player.name),
                    status,
                    listOf(action),
                ),
            ),
        )
    }
}
