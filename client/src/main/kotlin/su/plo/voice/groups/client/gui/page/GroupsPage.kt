package su.plo.voice.groups.client.gui.page

import net.minecraft.resources.ResourceLocation
import su.plo.config.entry.BooleanConfigEntry
import su.plo.lib.mod.client.gui.components.AbstractScrollbar
import su.plo.lib.mod.client.gui.components.Button
import su.plo.lib.mod.client.gui.components.IconButton
import su.plo.lib.mod.client.gui.components.TextFieldWidget
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.client.gui.settings.VoiceSettingsScreen
import su.plo.voice.client.gui.settings.widget.CompositeRowWidget
import su.plo.voice.client.gui.settings.widget.ToggleButton
import su.plo.voice.groups.client.gui.GroupsTabWidget
import su.plo.voice.groups.client.gui.addonIcon
import su.plo.voice.groups.client.gui.gray
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.client.gui.widget.StatusLabel
import su.plo.voice.groups.client.service.GroupsService
import su.plo.voice.groups.protocol.ActionResult
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

    protected fun confirm(
        title: McTextComponent,
        message: List<McTextComponent>,
        confirmLabel: McTextComponent,
        pops: Int = 1,
        action: (suspend GroupsService.() -> ActionResult)?,
    ) {
        tab.open(ConfirmPage(title, message, confirmLabel, pops, action))
    }

    protected fun <R : ActionResult> perform(
        button: GuiAbstractWidget?,
        call: suspend GroupsService.() -> R,
    ) = tab.perform(button, call)

    protected fun add(entry: AbstractScrollbar<VoiceSettingsScreen>.Entry) = tab.addEntry(entry)

    protected fun addCategory(title: McTextComponent) = add(tab.CategoryEntry(title))

    protected fun addCategory(
        title: McTextComponent,
        vararg trailing: GuiAbstractWidget,
    ) = add(tab.SectionEntry(title, trailing.toList()))

    protected fun addHint(value: McTextComponent) = add(tab.HintEntry { value })

    protected fun addHint(value: () -> McTextComponent) = add(tab.HintEntry(value))

    protected fun addLoading() = addHint(tr("loading"))

    protected fun addToggle(
        label: McTextComponent,
        tooltip: McTextComponent?,
        value: Boolean,
        lockedReason: McTextComponent? = null,
        onChange: (Boolean) -> Unit,
    ) = add(
        tab.LabeledEntry(
            label,
            ToggleButton(
                BooleanConfigEntry(value).apply { isDisabled = lockedReason != null },
                0,
                0,
                tab.fieldWidth,
                20,
            ) { onChange(it) },
            tooltip,
            lockedReason,
        ),
    )

    protected fun addRow(vararg widgets: GuiAbstractWidget) = add(row(*widgets))

    protected fun addHeader(
        title: McTextComponent,
        vararg trailing: GuiAbstractWidget,
        onBack: (Button) -> Unit = { tab.back() },
    ) = add(
        tab.HeaderEntry(
            title,
            iconButton(addonIcon("arrow_left"), tr("back"), onBack),
            trailing.toList(),
        ),
    )

    protected fun button(
        label: McTextComponent,
        width: Int = fitWidth(label),
        tooltip: McTextComponent? = null,
        onPress: (Button) -> Unit,
    ): Button =
        Button(
            0,
            0,
            width,
            20,
            label,
            onPress,
            tooltip(tooltip),
        )

    protected fun disabledButton(
        label: McTextComponent,
        reason: McTextComponent,
        width: Int = fitWidth(label),
    ): Button = button(label, width, reason) {}.also { it.isActive = false }

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

    protected fun textField(
        placeholder: McTextComponent,
        value: String,
        width: Int = 0,
        onChange: (String) -> Unit,
    ): TextFieldWidget =
        TextFieldWidget(0, 0, width, 20, placeholder.gray()).apply {
            this.value = value
            setResponder(onChange)
        }

    protected fun loadingLabel(isLoading: () -> Boolean): StatusLabel {
        val label = tr("loading")
        return StatusLabel(RenderUtil.getTextWidth(label)) { label.takeIf { isLoading() } }
    }

    protected fun fitWidth(vararg labels: McTextComponent): Int = labels.maxOf { RenderUtil.getTextWidth(it) } + BUTTON_PADDING * 2

    private fun row(vararg widgets: GuiAbstractWidget) =
        tab.FullWidthEntry(
            CompositeRowWidget(
                0,
                0,
                tab.rowWidth,
                20,
                4,
                *widgets,
            ),
        )

    private fun tooltip(value: McTextComponent?): Button.OnTooltip =
        if (value == null) {
            Button.NO_TOOLTIP
        } else {
            Button.OnTooltip { _, mouseX, mouseY -> tab.setTooltip(value, mouseX, mouseY) }
        }

    private companion object {
        const val BUTTON_PADDING = 8
    }
}
