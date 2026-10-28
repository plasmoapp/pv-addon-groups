package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.components.DisplayWidget
import su.plo.lib.mod.client.gui.components.TextWidget
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent

class TooltipTextWidget(
    private val text: McTextComponent,
    private val showTooltip: (mouseX: Int, mouseY: Int) -> Unit,
) : DisplayWidget(0, TextWidget.TEXT_HEIGHT) {
    override fun renderButton(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        context.drawOrderedString(text, width, x, y, Colors.WHITE)

        if (hovered && mouseX < x + RenderUtil.getTextWidth(text)) showTooltip(mouseX, mouseY)
    }
}
