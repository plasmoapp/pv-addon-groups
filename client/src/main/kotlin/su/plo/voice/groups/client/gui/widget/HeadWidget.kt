package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.components.DisplayWidget
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.voice.groups.client.gui.drawHead
import su.plo.voice.groups.protocol.GroupPlayer

class HeadWidget(
    private val player: GroupPlayer?,
    size: Int,
) : DisplayWidget(size, size) {
    override fun renderButton(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) = context.drawHead(player, x, y, width)
}
