package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.components.DisplayWidget
import su.plo.lib.mod.client.gui.components.TextWidget
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent
import java.awt.Color

class TooltipTextWidget(
    private val text: McTextComponent,
    private val color: Color,
    private val tooltip: McTextComponent?,
    private val showTooltip: (tooltip: McTextComponent, mouseX: Int, mouseY: Int) -> Unit,
) : DisplayWidget(0, TextWidget.TEXT_HEIGHT) {
    override fun renderButton(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        context.drawOrderedString(text, width, x, y, color)

        if (!hovered) return

        val textWidth = RenderUtil.getTextWidth(text)
        val shownTooltip = tooltip ?: text.takeIf { textWidth > width } ?: return

        if (mouseX < x + minOf(textWidth, width)) {
            showTooltip(shownTooltip, mouseX, mouseY)
        }
    }
}
