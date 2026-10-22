package su.plo.voice.groups.client.gui.page

import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.slib.api.chat.style.McTextStyle
import su.plo.voice.client.gui.settings.widget.VolumeSliderWidget
import su.plo.voice.groups.client.gui.addonIcon
import su.plo.voice.groups.client.gui.groupStatus
import su.plo.voice.groups.client.gui.onlineStatus
import su.plo.voice.groups.client.gui.pvIcon
import su.plo.voice.groups.client.gui.text
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.client.gui.widget.ResetButton
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupDetails
import su.plo.voice.groups.protocol.GroupPlayer

class MainPage : GroupsPage() {
    override fun init() {
        state.current?.let { addCurrentGroup(it) }
    }

    private fun addCurrentGroup(group: GroupDetails) {
        val self = service.self
        val buttons =
            listOfNotNull(
                iconButton(addonIcon("invite"), tr("invite.title")) {
                    // TODO: open invite page
                }.takeIf { GroupAction.INVITE in group.actions },
                iconButton(addonIcon("edit"), tr("settings.title")) {
                    // TODO: open settings page
                }.takeIf { group.actions.any { it in SETTINGS_ACTIONS } },
                iconButton(addonIcon("leave"), tr("leave.title")) {
                    // TODO: confirm and leave
                }.takeIf { state.capabilities.canLeave },
            )

        addCategory(tr("main.your_group"))
        add(
            tab.CardEntry(
                group.summary.owner,
                text(group.summary.name),
                groupStatus(group.summary),
                buttons,
                headSize = 32,
            ),
        )

        val members =
            group.members
                .sortedWith(compareBy<GroupPlayer> { it.id != self.id }.thenBy { it.name.lowercase() })
        members.forEach { player ->
            val isSelf = player.id == self.id
            val isOwner = player.id == group.summary.owner?.id

            val name = if (isSelf) tr("you", player.name) else text(player.name)

            add(
                tab.MemberEntry(
                    player,
                    if (isOwner) name.withStyle(McTextStyle.GOLD) else name,
                    onlineStatus(player),
                    if (isSelf) emptyList() else memberControls(player),
                    tr("owner").takeIf { isOwner },
                ),
            )
        }
    }

    private fun memberControls(player: GroupPlayer): List<GuiAbstractWidget> {
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

        return listOf(volume, mute, reset)
    }

    private companion object {
        val SETTINGS_ACTIONS =
            setOf(
                GroupAction.SET_NAME,
                GroupAction.SET_PASSWORD,
                GroupAction.SET_PERMISSIONS,
                GroupAction.SET_PERSISTENT,
                GroupAction.UNBAN,
                GroupAction.DELETE,
            )
    }
}
