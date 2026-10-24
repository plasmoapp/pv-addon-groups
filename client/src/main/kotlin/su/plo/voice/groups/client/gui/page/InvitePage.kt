package su.plo.voice.groups.client.gui.page

import kotlinx.coroutines.delay
import su.plo.lib.mod.client.gui.components.AbstractScrollbar
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextStyle
import su.plo.voice.client.gui.settings.VoiceSettingsScreen
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

private typealias Entry = AbstractScrollbar<VoiceSettingsScreen>.Entry

class InvitePage(
    private val groupId: UUID,
) : GroupsPage() {
    private var query = ""
    private var remoteResults: InviteCandidates? = null
    private var searching = false
    private var resultsAnchor: Entry? = null

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
        addHeader(tr("invite.title"), loadingLabel { searching && remoteResults != null })

        val searchRow =
            row(
                textField(tr("invite.search"), query) {
                    if (it == query) return@textField
                    query = it
                    if (remoteSearch) search(after = SEARCH_DEBOUNCE) else updateResults()
                },
            )
        add(searchRow)
        resultsAnchor = searchRow

        resultEntries().forEach(::add)
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
        val anchor = resultsAnchor ?: return

        if (tab.isShown(this)) tab.replaceEntriesAfter(anchor, resultEntries())
    }

    private fun resultEntries(): List<Entry> {
        val group = checkNotNull(state.current)
        val candidates = candidates(group) ?: return listOf(tab.HintEntry { tr("loading") })
        val invitedIds = group.sentInvites.orEmpty().mapTo(HashSet()) { it.id }
        val bannedIds = group.banned.orEmpty().mapTo(HashSet()) { it.id }

        val footer = remoteFooter()

        return buildList {
            if (candidates.isEmpty() && footer == null) {
                val hint = if (query.isBlank()) tr("invite.nobody") else tr("invite.no_matches")
                add(tab.HintEntry { hint })
            }
            candidates.forEach {
                add(candidateEntry(it, it.id in invitedIds, it.id in bannedIds))
            }
            footer?.let { add(tab.HintEntry { it }) }
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

    private fun candidateEntry(
        player: GroupPlayer,
        invited: Boolean,
        banned: Boolean,
    ): Entry {
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

        return tab.CardEntry(
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
