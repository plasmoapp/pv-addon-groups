package su.plo.voice.groups.client.gui

import net.minecraft.client.Minecraft
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.resources.ResourceLocation
import su.plo.lib.mod.client.gui.widget.GuiWidget
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.lib.mod.client.render.texture.ModPlayerSkins
import su.plo.voice.groups.protocol.GroupPlayer

fun pvIcon(name: String): ResourceLocation = ResourceLocation.parse("plasmovoice:textures/icons/$name.png")

fun addonIcon(name: String): ResourceLocation = ResourceLocation.parse("pv_addon_groups:textures/icons/$name.png")

fun GuiRenderContext.drawHead(
    player: GroupPlayer?,
    x: Int,
    y: Int,
    size: Int,
) {
    val skin = player?.let { ModPlayerSkins.getSkin(it.id, it.name) } ?: DefaultPlayerSkin.getDefaultTexture()

    blit(
        skin,
        x,
        y,
        size,
        size,
        8f,
        8f,
        8,
        8,
        64,
        64,
    )
    blit(
        skin,
        x,
        y,
        size,
        size,
        40f,
        8f,
        8,
        8,
        64,
        64,
    )
}

fun GuiRenderContext.drawRowBackground(
    x: Int,
    y: Int,
    width: Int,
    height: Int,
) {
    val texture =
        if (Minecraft.getInstance().level == null) {
            GuiWidget.MENU_LIST_BACKGROUND_LOCATION
        } else {
            GuiWidget.INWORLD_MENU_LIST_BACKGROUND_LOCATION
        }

    blit(
        texture,
        x,
        y,
        0f,
        0f,
        width,
        height,
        32,
        32,
    )
}
