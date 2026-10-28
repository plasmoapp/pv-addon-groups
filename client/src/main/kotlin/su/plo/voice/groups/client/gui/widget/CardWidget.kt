package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.components.CompositeWidget
import su.plo.lib.mod.client.gui.components.MeasurableKt
import su.plo.lib.mod.client.gui.components.WidgetContainerKt
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.voice.groups.client.gui.drawRowBackground

open class CardWidget(
    private val content: GuiAbstractWidget,
    private val padding: Int = PADDING,
) : CompositeWidget() {
    private val children = listOf(content)

    override fun getWidgets(): List<GuiAbstractWidget> = children

    override fun measure() {
        MeasurableKt.measure(content, width - padding * 2)
        height = content.height + padding * 2
    }

    override fun place() = WidgetContainerKt.place(content, x + padding, y + padding)

    override fun renderBackground(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
    ) = context.drawRowBackground(x, y, width, height)

    private companion object {
        const val PADDING = 2
    }
}
