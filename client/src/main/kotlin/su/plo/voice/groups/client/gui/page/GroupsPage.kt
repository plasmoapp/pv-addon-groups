package su.plo.voice.groups.client.gui.page

import net.minecraft.resources.ResourceLocation
import su.plo.lib.mod.client.gui.components.AbstractScrollbar
import su.plo.lib.mod.client.gui.components.Button
import su.plo.lib.mod.client.gui.components.IconButton
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.client.gui.settings.VoiceSettingsScreen
import su.plo.voice.groups.client.gui.GroupsTabWidget
import su.plo.voice.groups.client.service.GroupsService
import su.plo.voice.groups.protocol.GroupsState

abstract class GroupsPage {
    protected lateinit var tab: GroupsTabWidget
        private set

    protected val state: GroupsState
        get() = checkNotNull(tab.state)

    protected val service: GroupsService
        get() = tab.service

    fun attach(tab: GroupsTabWidget) {
        this.tab = tab
    }

    abstract fun init()

    open fun onOpen() = Unit

    open fun onStateChanged() = tab.refresh()

    open fun isValid(state: GroupsState): Boolean = true

    protected fun add(entry: AbstractScrollbar<VoiceSettingsScreen>.Entry) = tab.addEntry(entry)

    protected fun addCategory(title: McTextComponent) = add(tab.CategoryEntry(title))

    protected fun iconButton(
        icon: () -> ResourceLocation,
        tooltip: McTextComponent?,
        onPress: (Button) -> Unit,
    ): IconButton =
        IconButton(
            0,
            0,
            20,
            20,
            onPress,
            tooltip(tooltip),
            icon,
            true,
        )

    protected fun iconButton(
        icon: ResourceLocation,
        tooltip: McTextComponent?,
        onPress: (Button) -> Unit,
    ): IconButton = iconButton({ icon }, tooltip, onPress)

    private fun tooltip(value: McTextComponent?): Button.OnTooltip =
        if (value == null) {
            Button.NO_TOOLTIP
        } else {
            Button.OnTooltip { _, mouseX, mouseY -> tab.setTooltip(value, mouseX, mouseY) }
        }
}
