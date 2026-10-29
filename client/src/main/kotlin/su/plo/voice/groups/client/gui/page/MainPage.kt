package su.plo.voice.groups.client.gui.page

import su.plo.lib.mod.client.gui.components.CompositeColumnWidget
import su.plo.lib.mod.client.gui.components.CompositeRowWidget
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.slib.api.chat.style.McTextStyle
import su.plo.voice.client.gui.settings.widget.VolumeSliderWidget
import su.plo.voice.groups.client.gui.addonIcon
import su.plo.voice.groups.client.gui.gray
import su.plo.voice.groups.client.gui.groupStatus
import su.plo.voice.groups.client.gui.onlineStatus
import su.plo.voice.groups.client.gui.pvIcon
import su.plo.voice.groups.client.gui.text
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.client.gui.widget.CardWidget
import su.plo.voice.groups.client.gui.widget.ContextMenuButton
import su.plo.voice.groups.client.gui.widget.ResetButton
import su.plo.voice.groups.client.gui.widget.SearchFilter
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupDetails
import su.plo.voice.groups.protocol.GroupInvite
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupSummary
import su.plo.voice.groups.protocol.JoinState

class MainPage : GroupsPage() {
    private val search = SearchFilter()
    private var groups: List<GroupSummary>? = null
    private var loading = false

    override fun onOpen() = reload()

    override fun onStateChanged() {
        reload()
        tab.refresh()
    }

    override fun init() {
        val current = state.current
        current?.let { addCurrentGroup(it) }

        if (state.invites.isNotEmpty()) {
            val invites = addCategory(tr("main.invites"))
            state.invites.forEach { invites.add(inviteCard(it)) }
        }

        when {
            state.capabilities.canBrowse -> addBrowse(current)
            state.capabilities.canCreate -> addSection().add(row(createButton(width = 0)))
            current == null && state.invites.isEmpty() -> addSection().add(hint(tr("main.not_in_group")))
        }
    }

    private fun reload() {
        if (!state.capabilities.canBrowse) return

        loading = true
        tab.launch({ browse() }, onFailure = { loading = false }) { groups ->
            loading = false
            this.groups =
                groups.sortedWith(
                    compareByDescending<GroupSummary> { it.onlineCount }.thenBy { it.name.lowercase() },
                )
            tab.refresh()
        }
    }

    private fun addCurrentGroup(group: GroupDetails) {
        val self = service.self
        val buttons =
            listOfNotNull(
                iconButton(addonIcon("invite"), tr("invite.title")) { tab.open(InvitePage(group.summary.id)) }
                    .takeIf { GroupAction.INVITE in group.actions },
                iconButton(addonIcon("edit"), tr("settings.title")) { tab.open(SettingsPage(group.summary.id)) }
                    .takeIf { group.actions.any { it in SettingsPage.ACTIONS } },
                iconButton(addonIcon("leave"), tr("leave.title")) { leave(group) }
                    .takeIf { state.capabilities.canLeave },
            )

        val members = addCategory(tr("main.your_group")).add(CompositeColumnWidget(MEMBER_GAP))
        members.add(
            card(
                group.summary.owner,
                text(group.summary.name),
                groupStatus(group.summary),
                buttons,
                headSize = CURRENT_GROUP_HEAD_SIZE,
            ),
        )

        group.members
            .sortedWith(compareBy<GroupPlayer> { it.id != self.id }.thenBy { it.name.lowercase() })
            .forEach { members.add(memberRow(group, it)) }
    }

    private fun addBrowse(current: GroupDetails?) {
        val browse = addCategory(tr("main.browse"), loadingLabel { loading && groups != null })
        val create = createButton().takeIf { state.capabilities.canCreate }
        browse.add(row(search.field(tr("main.search")), create))

        val groups = groups
        if (groups == null) {
            browse.add(hint(tr("loading")))
            return
        }

        val others = groups.filter { it.id != current?.summary?.id }
        if (others.isEmpty()) browse.add(hint(tr("main.no_groups")))
        others.forEach { browse.add(groupCard(it)) }
    }

    private fun createButton(width: Int = fitWidth(tr("main.create"))) =
        button(tr("main.create"), width) { button ->
            tab.leaveCurrentThen(button, tr("main.create.title")) { create() }
        }

    private fun leave(group: GroupDetails) {
        confirm(
            tr("leave.title"),
            listOf(group.leaveConsequence(service.self.id)),
            tr("leave.confirm"),
        ) { leave() }
    }

    private fun memberRow(
        group: GroupDetails,
        player: GroupPlayer,
    ): CompositeRowWidget {
        val isSelf = player.id == service.self.id
        val isOwner = player.id == group.summary.owner?.id
        val name = if (isSelf) tr("you", player.name) else text(player.name)

        return playerRow(
            player,
            if (isOwner) name.withStyle(McTextStyle.GOLD) else name,
            onlineStatus(player),
            if (isSelf) emptyList() else memberControls(group, player),
            titleTooltip = tr("owner").takeIf { isOwner },
        )
    }

    private fun memberControls(
        group: GroupDetails,
        player: GroupPlayer,
    ): List<GuiAbstractWidget> {
        val volumeEntry = tab.volumes.getPlayerVolume(player.id)
        val muteEntry = tab.volumes.getPlayerMute(player.id)

        val volume = VolumeSliderWidget(tab.hotkeys, volumeEntry, "%", 0, 0, 80, 20)
        val mute =
            iconButton({ pvIcon(if (muteEntry.value()) "speaker_menu_disabled" else "speaker_menu") }, null) {
                muteEntry.set(!muteEntry.value())
            }
        val reset =
            ResetButton({ volumeEntry.isDefault && !muteEntry.value() }) {
                volumeEntry.reset()
                muteEntry.set(false)
                volume.updateValue()
            }
        return listOfNotNull(volume, mute, reset, memberMenu(group, player))
    }

    private fun memberMenu(
        group: GroupDetails,
        player: GroupPlayer,
    ): GuiAbstractWidget? {
        val groupId = group.summary.id
        val groupName = group.summary.name
        val isOwner = player.id == group.summary.owner?.id
        val items =
            buildList {
                if (GroupAction.TRANSFER in group.actions && !isOwner) {
                    add(
                        tr("manage.transfer") to {
                            confirm(
                                tr("manage.transfer"),
                                listOf(
                                    tr("manage.transfer.message", player.name, groupName),
                                ),
                                tr("manage.transfer.confirm"),
                            ) { transfer(groupId, player.id) }
                        },
                    )
                }

                if (GroupAction.KICK in group.actions) {
                    add(
                        tr("manage.kick") to {
                            confirm(
                                tr("manage.kick.title"),
                                listOf(
                                    tr("manage.kick.message", player.name, groupName),
                                    tr("manage.kick.details"),
                                ),
                                tr("manage.kick"),
                            ) { kick(groupId, player.id) }
                        },
                    )
                }

                if (GroupAction.BAN in group.actions && !isOwner) {
                    add(
                        tr("manage.ban") to {
                            confirm(
                                tr("manage.ban.title"),
                                listOf(
                                    tr("manage.ban.message", player.name, groupName),
                                ),
                                tr("manage.ban"),
                            ) { ban(groupId, player.id) }
                        },
                    )
                }
            }

        if (items.isNotEmpty()) return ContextMenuButton(tab, player.id, items)
        if (group.actions.none { it in MENU_ACTIONS }) return null

        return iconButton(addonIcon("more"), tr("error.owner")).also { it.isActive = false }
    }

    private fun inviteCard(invite: GroupInvite): CardWidget {
        val accept =
            iconButton(addonIcon("accept"), tr("main.invite.accept")) { button ->
                tab.leaveCurrentThen(button, tr("main.join.title", invite.group.name)) { join(invite.group.id) }
            }
        val decline =
            iconButton(addonIcon("decline"), tr("main.invite.decline")) { button ->
                perform(button) { declineInvite(invite.id) }
            }

        return card(
            invite.from,
            text(invite.group.name),
            tr("main.invite.from", invite.from.name, invite.group.onlineCount).gray(),
            listOf(accept, decline),
        )
    }

    private fun groupCard(group: GroupSummary): CardWidget {
        val join =
            when (group.joinState) {
                JoinState.JOINABLE -> {
                    button(tr("join")) { button ->
                        tab.leaveCurrentThen(button, tr("main.join.title", group.name)) { join(group.id) }
                    }
                }

                JoinState.PASSWORD -> {
                    button(tr("join")) { tab.open(PasswordPage(group)) }
                }

                JoinState.NO_PERMISSION -> {
                    disabledButton(tr("join"), tr("main.join.no_permission"))
                }

                JoinState.BANNED -> {
                    disabledButton(tr("join"), tr("main.join.banned"))
                }
            }

        return search.track(
            listOfNotNull(group.name, group.owner?.name).joinToString(" "),
            card(group.owner, text(group.name), groupStatus(group), listOf(join)),
        )
    }

    private companion object {
        const val MEMBER_GAP = 6
        const val CURRENT_GROUP_HEAD_SIZE = 32

        val MENU_ACTIONS = setOf(GroupAction.TRANSFER, GroupAction.KICK, GroupAction.BAN)
    }
}
