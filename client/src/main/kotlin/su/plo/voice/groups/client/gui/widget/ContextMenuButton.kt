package su.plo.voice.groups.client.gui.widget

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Minecraft
import su.plo.lib.mod.client.gui.components.Button
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiCursor
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.groups.client.gui.GroupsTabWidget
import su.plo.voice.groups.client.gui.addonIcon
import java.awt.Color

class ContextMenuButton(
    private val tab: GroupsTabWidget,
    val key: Any,
    private val items: List<Pair<McTextComponent, () -> Unit>>,
) : Button(0, 0, 20, 20, McTextComponent.empty(), {}, NO_TOOLTIP) {
    private val menuWidth = items.maxOf { (label, _) -> RenderUtil.getTextWidth(label) } + 10
    private val menuHeight = items.size * ITEM_HEIGHT + 1
    private var menuX = 0
    private var menuY = 0

    private val open: Boolean
        get() = tab.screen.overlay === this

    override fun onPress() {
        tab.screen.overlay = if (open) null else this
    }

    override fun keyPressed(
        keyCode: Int,
        modifiers: Int,
    ): Boolean {
        if (open && keyCode == InputConstants.KEY_ESCAPE) {
            tab.screen.overlay = null
            return true
        }

        return super.keyPressed(keyCode, modifiers)
    }

    override fun renderButton(
        context: GuiRenderContext,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        super.renderButton(context, mouseX, mouseY, delta)
        context.blitIcon(
            addonIcon("more"),
            x + 2,
            y + 2,
            16,
            16,
            if (active) Colors.WHITE else Colors.GRAY,
        )
        if (!open) return

        menuX = x + width - menuWidth
        menuY =
            if (y + height + menuHeight > Minecraft.getInstance().window.guiScaledHeight) {
                y - menuHeight
            } else {
                y + height
            }

        tab.screen.deferredRender {
            //? if <1.21.6 {
            context.stack.pushPose()
            context.stack.translate(0f, 0f, 10f)
            //?}

            val hoveredIndex = itemAt(tab.screen.mouseX.toDouble(), tab.screen.mouseY.toDouble())
            if (hoveredIndex != null) context.requestCursor(GuiCursor.POINTING_HAND)

            context.fill(menuX, menuY, menuX + menuWidth, menuY + menuHeight, FRAME_COLOR)

            items.forEachIndexed { index, (label, _) ->
                val itemY = menuY + 1 + index * ITEM_HEIGHT
                val background = if (index == hoveredIndex) HOVER_COLOR else Colors.BLACK

                context.fill(
                    menuX + 1,
                    itemY,
                    menuX + menuWidth - 1,
                    itemY + ITEM_HEIGHT - 1,
                    background,
                )

                context.drawString(
                    label,
                    menuX + 5,
                    itemY + (ITEM_HEIGHT - 1 - RenderUtil.getFontHeight()) / 2 + 1,
                    Colors.TEXT,
                )
            }

            //? if <1.21.6
            context.stack.popPose()
        }
    }

    fun menuClicked(
        mouseX: Double,
        mouseY: Double,
    ) {
        tab.screen.overlay = null
        val index = itemAt(mouseX, mouseY) ?: return

        playDownSound()

        val (_, onClick) = items[index]
        onClick()
    }

    private fun itemAt(
        mouseX: Double,
        mouseY: Double,
    ): Int? {
        if (mouseX < menuX || mouseX >= menuX + menuWidth || mouseY < menuY + 1) return null
        return ((mouseY - menuY - 1) / ITEM_HEIGHT).toInt().takeIf { it < items.size }
    }

    private companion object {
        private const val ITEM_HEIGHT = 17

        private val FRAME_COLOR = Color(0x464646)
        private val HOVER_COLOR = Color(0x323232)
    }
}
