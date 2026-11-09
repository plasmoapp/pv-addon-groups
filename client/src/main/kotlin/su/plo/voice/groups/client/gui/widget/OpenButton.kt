package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.components.Button
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.groups.client.gui.addonIcon

class OpenButton(
    onPress: () -> Unit,
) : Button(0, 0, 20, 20, McTextComponent.empty(), { onPress() }, NO_TOOLTIP) {
    var rowHovered = false

    override fun isHoveredOrFocused(): Boolean = rowHovered || super.isHoveredOrFocused()

    override fun renderButton(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.renderButton(context, mouseX, mouseY, delta)
        context.blitIcon(
            addonIcon("arrow_right"),
            x + 2,
            y + 2,
            16,
            16,
            if (active) Colors.WHITE else Colors.GRAY,
        )
    }
}
