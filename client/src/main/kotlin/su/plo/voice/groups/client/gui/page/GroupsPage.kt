package su.plo.voice.groups.client.gui.page

import net.minecraft.resources.ResourceLocation
import su.plo.config.entry.BooleanConfigEntry
import su.plo.lib.mod.client.gui.components.Button
import su.plo.lib.mod.client.gui.components.CompositeColumnWidget
import su.plo.lib.mod.client.gui.components.CompositeRowWidget
import su.plo.lib.mod.client.gui.components.IconButton
import su.plo.lib.mod.client.gui.components.TextFieldWidget
import su.plo.lib.mod.client.gui.components.TextWidget
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.client.gui.settings.widget.TitleWidget
import su.plo.voice.client.gui.settings.widget.ToggleButton
import su.plo.voice.groups.client.gui.GroupsTabWidget
import su.plo.voice.groups.client.gui.addonIcon
import su.plo.voice.groups.client.gui.gray
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.client.gui.widget.CardWidget
import su.plo.voice.groups.client.gui.widget.HeadWidget
import su.plo.voice.groups.client.gui.widget.LinkWidget
import su.plo.voice.groups.client.gui.widget.TooltipTextWidget
import su.plo.voice.groups.client.service.GroupsService
import su.plo.voice.groups.protocol.ActionResult
import su.plo.voice.groups.protocol.GroupPlayer
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

    protected fun addCategory(
        title: McTextComponent,
        vararg trailing: GuiAbstractWidget,
        leading: List<GuiAbstractWidget> = emptyList(),
    ): CompositeColumnWidget = addSection().apply { add(TitleWidget(title, leading, trailing.toList())) }

    protected fun addSection(): CompositeColumnWidget = tab.addSection()

    protected fun backButton(onBack: (Button) -> Unit = { tab.back() }): IconButton =
        iconButton(addonIcon("arrow_left"), tr("back"), onBack)

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
        onPress: (Button) -> Unit = {},
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
        onPress: (Button) -> Unit = {},
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

    protected fun toggle(
        label: McTextComponent,
        tooltip: McTextComponent?,
        value: Boolean,
        onChange: (Boolean) -> Unit,
    ): CompositeRowWidget =
        labeled(
            label,
            ToggleButton(
                BooleanConfigEntry(value),
                0,
                0,
                tab.fieldWidth,
                20,
            ) { onChange(it) },
            tooltip,
        )

    protected fun loadingLabel(isLoading: () -> Boolean): TextWidget {
        val label = tr("loading")

        return TextWidget(
            { label.takeIf { isLoading() } },
            Colors.GRAY,
            false,
            RenderUtil.getTextWidth(label),
        )
    }

    protected fun hint(value: McTextComponent): TextWidget = TextWidget(value, Colors.GRAY, true)

    protected fun hint(value: () -> McTextComponent): TextWidget =
        TextWidget(
            { value() },
            Colors.GRAY,
            true,
        )

    protected fun fitWidth(vararg labels: McTextComponent): Int =
        labels.maxOf { RenderUtil.getTextWidth(it) } + BUTTON_PADDING * 2

    protected fun row(vararg widgets: GuiAbstractWidget?): CompositeRowWidget =
        CompositeRowWidget(ELEMENT_GAP, *widgets)

    protected fun labeled(
        label: McTextComponent,
        widget: GuiAbstractWidget,
        tooltip: McTextComponent? = null,
    ): CompositeRowWidget = row(text(label, tooltip), widget)

    protected fun card(
        player: GroupPlayer?,
        title: McTextComponent,
        subtitle: McTextComponent?,
        trailing: List<GuiAbstractWidget> = emptyList(),
        headSize: Int = HEAD_SIZE,
    ): CardWidget = CardWidget(playerRow(player, title, subtitle, trailing, headSize = headSize))

    protected fun playerRow(
        player: GroupPlayer?,
        title: McTextComponent,
        subtitle: McTextComponent?,
        trailing: List<GuiAbstractWidget> = emptyList(),
        titleTooltip: McTextComponent? = null,
        headSize: Int = HEAD_SIZE,
    ): CompositeRowWidget =
        row(
            HeadWidget(player, headSize).also { tab.loadHead(player) },
            labels(title, subtitle, titleTooltip),
            *trailing.toTypedArray(),
        )

    protected fun link(
        title: McTextComponent,
        subtitle: McTextComponent,
        onClick: () -> Unit,
    ): LinkWidget = LinkWidget(labels(title, subtitle, null), onClick)

    private fun labels(
        title: McTextComponent,
        subtitle: McTextComponent?,
        titleTooltip: McTextComponent?,
    ): CompositeColumnWidget =
        CompositeColumnWidget(LINE_GAP, LABELS_PADDING_TOP).apply {
            add(text(title, titleTooltip))
            subtitle?.let { add(TextWidget(it, Colors.GRAY)) }
        }

    private fun text(
        value: McTextComponent,
        tooltip: McTextComponent?,
    ): GuiAbstractWidget =
        if (tooltip == null) {
            TextWidget(value)
        } else {
            TooltipTextWidget(value) { mouseX, mouseY -> tab.setTooltip(tooltip, mouseX, mouseY) }
        }

    private fun tooltip(value: McTextComponent?): Button.OnTooltip =
        if (value == null) {
            Button.NO_TOOLTIP
        } else {
            Button.OnTooltip { _, mouseX, mouseY -> tab.setTooltip(value, mouseX, mouseY) }
        }

    protected companion object {
        const val ELEMENT_GAP = 4

        private const val LINE_GAP = 3

        private const val LABELS_PADDING_TOP = 1
        private const val BUTTON_PADDING = 8
        private const val HEAD_SIZE = 24
    }
}
