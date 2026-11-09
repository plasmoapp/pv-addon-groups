package su.plo.voice.groups.client.gui.widget

import com.mojang.blaze3d.platform.InputConstants
import su.plo.lib.mod.client.gui.components.CompositeRowWidget
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.lib.mod.client.render.gui.GuiCursor
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color

class LinkWidget private constructor(
    labels: GuiAbstractWidget,
    private val openButton: OpenButton,
    private val onClick: () -> Unit,
) : CardWidget(CompositeRowWidget(GAP, labels, openButton), PADDING) {
    constructor(
        labels: GuiAbstractWidget,
        onClick: () -> Unit,
    ) : this(labels, OpenButton(onClick), onClick)

    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean {
        if (super.mouseClicked(mouseX, mouseY, button)) return true
        if (button != InputConstants.MOUSE_BUTTON_LEFT || !isMouseOver(mouseX, mouseY)) return false

        playDownSound()
        onClick()
        return true
    }

    override fun isMouseOver(
        mouseX: Double,
        mouseY: Double,
    ): Boolean = visible && isHovered(mouseX, mouseY)

    override fun renderBackground(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
    ) {
        super.renderBackground(context, mouseX, mouseY)
        openButton.rowHovered = hovered
        if (!hovered) return

        context.fill(x, y, x + width, y + height, HOVER_COLOR)
        context.requestCursor(GuiCursor.POINTING_HAND)
    }

    private companion object {
        const val GAP = 4
        const val PADDING = 4

        val HOVER_COLOR = Color(255, 255, 255, 24)
    }
}
