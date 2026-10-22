package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.components.TextFieldWidget
import su.plo.lib.mod.client.render.gui.GuiCursor
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.client.gui.settings.VoiceSettingsScreen

class LockedTextField(
    private val screen: VoiceSettingsScreen,
    width: Int,
    value: String,
    placeholder: McTextComponent,
    private val reason: McTextComponent,
) : TextFieldWidget(0, 0, width, 20, placeholder) {
    init {
        this.value = value
        isEditable = false
    }

    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean = false

    override fun changeFocus(lookForwards: Boolean): Boolean = false

    override fun renderButton(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.renderButton(context, mouseX, mouseY, delta)
        if (isHovered(mouseX.toDouble(), mouseY.toDouble())) {
            context.requestCursor(GuiCursor.NOT_ALLOWED)
            screen.setTooltip(reason, mouseX, mouseY)
        }
    }
}
