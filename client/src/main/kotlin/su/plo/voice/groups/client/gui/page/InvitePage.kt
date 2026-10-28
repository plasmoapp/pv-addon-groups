package su.plo.voice.groups.client.gui.page

import kotlinx.coroutines.delay
import su.plo.lib.mod.client.gui.components.CompositeColumnWidget
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextStyle
import su.plo.voice.groups.client.gui.text
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.protocol.ActionError
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupDetails
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupsState
import su.plo.voice.groups.protocol.InviteCandidates
import su.plo.voice.groups.protocol.MIN_INVITE_QUERY_LENGTH
import su.plo.voice.groups.protocol.RateLimited
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class InvitePage(
    private val groupId: UUID,
) : GroupsPage() {
    private var query = ""
    private var remoteResults: InviteCandidates? = null
    private var searching = false
    private var results: CompositeColumnWidget? = null

    @Volatile
    private var searchId = 0

    private val remoteSearch: Boolean
        get() = state.capabilities.remoteInviteSearch

    override fun isValid(state: GroupsState): Boolean =
        state.current?.let { it.summary.id == groupId && GroupAction.INVITE in it.actions } == true

    override fun onOpen() {
        if (remoteSearch) search()
    }

    override fun onStateChanged() = updateResults()

    override fun init() {
        val page =
            addCategory(
                tr("invite.title"),
                loadingLabel { searching && remoteResults != null },
                leading = listOf(backButton()),
            )
        page.add(
            row(
                textField(tr("invite.search"), query) {
                    if (it == query) return@textField
                    query = it
                    if (remoteSearch) search(after = SEARCH_DEBOUNCE) else updateResults()
                },
            ),
        )

        results = page.add(CompositeColumnWidget(ELEMENT_GAP)).apply { widgets = resultWidgets() }
    }

    private fun search(after: Duration = Duration.ZERO) {
        val id = ++searchId
        val query = query
        searching = true

        tab.launch(
            {
                delay(after)
                if (id == searchId) inviteCandidates(query) else null
            },
            onFailure = {
                if (id == searchId) searching = false
            },
        ) { result ->
            if (result == null || id != searchId) return@launch

            when (result) {
                is InviteCandidates -> {
                    remoteResults = result
                    searching = false
                    updateResults()
                }

                is RateLimited -> {
                    search(after = result.retryAfterMs.milliseconds)
                }

                is ActionError -> {
                    searching = false
                    tab.toast(result)
                }
            }
        }
    }

    private fun updateResults() {
        val results = results ?: return

        if (tab.isShown(this)) results.widgets = resultWidgets()
    }

    private fun resultWidgets(): List<GuiAbstractWidget> {
        val group = checkNotNull(state.current)
        val candidates = candidates(group) ?: return listOf(hint(tr("loading")))
        val invitedIds = group.sentInvites.orEmpty().mapTo(HashSet()) { it.id }
        val bannedIds = group.banned.orEmpty().mapTo(HashSet()) { it.id }

        val footer = remoteFooter()

        return buildList {
            if (candidates.isEmpty() && footer == null) {
                add(hint(if (query.isBlank()) tr("invite.nobody") else tr("invite.no_matches")))
            }
            candidates.forEach {
                add(candidateCard(it, it.id in invitedIds, it.id in bannedIds))
            }
            footer?.let { add(hint(it)) }
        }
    }

    private fun remoteFooter(): McTextComponent? {
        val results = remoteResults?.takeIf { remoteSearch } ?: return null

        return when {
            results.hasMore -> {
                tr("invite.has_more", results.players.size)
            }

            query.trim().length < MIN_INVITE_QUERY_LENGTH -> {
                tr("invite.search_for_more", MIN_INVITE_QUERY_LENGTH)
            }

            else -> {
                null
            }
        }
    }

    private fun candidates(group: GroupDetails): List<GroupPlayer>? {
        val memberIds = group.members.mapTo(HashSet()) { it.id }
        val players =
            if (remoteSearch) {
                remoteResults?.players ?: return null
            } else {
                val search = query.trim()

                (group.sentInvites.orEmpty() + service.voicePlayers)
                    .distinctBy { it.id }
                    .filter { it.name.contains(search, ignoreCase = true) }
                    .sortedBy { it.name.lowercase() }
            }

        return players.filter { it.id !in memberIds }
    }

    private fun candidateCard(
        player: GroupPlayer,
        invited: Boolean,
        banned: Boolean,
    ): GuiAbstractWidget {
        val width = fitWidth(tr("invite"), tr("cancel"))
        val action =
            when {
                banned -> {
                    disabledButton(tr("invite"), tr("invite.banned"), width)
                }

                invited -> {
                    button(tr("cancel"), width) { button ->
                        perform(button) { cancelInvite(groupId, player.id) }
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

        return card(
            player,
            text(player.name),
            status,
            listOf(action),
        )
    }

    private companion object {
        val SEARCH_DEBOUNCE = 250.milliseconds
    }
}
