package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent

class StatusLabel(
    width: Int,
    private val value: () -> McTextComponent?,
) : GuiAbstractWidget(0, 0, width, 20) {
    override fun render(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val value = value() ?: return
        val textX = x + width - RenderUtil.getTextWidth(value)
        context.drawString(value, textX, y + (height - RenderUtil.getFontHeight()) / 2 + 1, Colors.GRAY)
    }

    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean = false
}
