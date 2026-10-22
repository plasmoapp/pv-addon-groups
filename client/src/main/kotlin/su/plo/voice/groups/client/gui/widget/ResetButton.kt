package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.components.Button
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.groups.client.gui.drawIcon
import su.plo.voice.groups.client.gui.pvIcon

class ResetButton(
    private val isDefault: () -> Boolean,
    onReset: () -> Unit,
) : Button(0, 0, 20, 20, McTextComponent.empty(), { onReset() }, NO_TOOLTIP) {
    override fun render(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        isActive = !isDefault()
        super.render(context, mouseX, mouseY, delta)
    }

    override fun renderButton(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.renderButton(context, mouseX, mouseY, delta)
        context.drawIcon(pvIcon("reset"), x + 2, y + 2, active)
    }
}
