package su.plo.voice.groups.client.gui

import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import su.plo.lib.mod.client.gui.components.AbstractScrollbar
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.lib.mod.client.gui.widget.GuiWidgetListener
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.lib.mod.client.render.texture.ModPlayerSkins
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.client.config.hotkey.Hotkeys
import su.plo.voice.client.config.VoiceClientConfig
import su.plo.voice.client.gui.settings.VoiceSettingsScreen
import su.plo.voice.client.gui.settings.tab.TabWidget
import su.plo.voice.groups.client.gui.page.GroupsPage
import su.plo.voice.groups.client.service.GroupsClient
import su.plo.voice.groups.client.service.GroupsService
import su.plo.voice.groups.client.service.GroupsStateListener
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupsState

class GroupsTabWidget(
    parent: VoiceSettingsScreen,
    voiceClient: PlasmoVoiceClient,
    config: VoiceClientConfig,
    private val navigation: GroupsNavigationState,
) : TabWidget(parent, voiceClient, config) {
    private val stateListener: GroupsStateListener = { Minecraft.getInstance().execute(::onStateChanged) }

    private var shownPage: GroupsPage? = null
    private var attached = false
    private var fetchingState = false

    val service: GroupsService
        get() = GroupsClient.service

    val state: GroupsState?
        get() = service.state

    val hotkeys: Hotkeys
        get() = voiceClient.hotkeys

    val volumes: VoiceClientConfig.Voice.SourceLineVolumes
        get() = config.voice.volumes

    override fun init() {
        super.init()
        if (!attached) {
            attached = true
            service.addListener(stateListener)
        }

        val state = state
        if (state == null) {
            fetchState()
            addEntry(HintEntry { tr("loading") })
            return
        }

        resetInvalidPages(state)

        val page = navigation.currentPage
        val restoreScroll = shownPage == null
        page.attach(this)
        if (page !== shownPage) {
            shownPage = page
            page.onOpen()
        }
        page.init()

        if (restoreScroll) setScrollTop(navigation.scrollTop)
    }

    override fun removed() {
        attached = false
        service.removeListener(stateListener)
        navigation.scrollTop = scrollTop
        super.removed()
    }

    fun open(page: GroupsPage) {
        navigation.pages.addLast(page)
        navigation.scrollTop = 0.0
        if (!attached) return

        setScrollTop(0.0)
        init()
    }

    fun back(count: Int = 1) {
        repeat(count) { navigation.pages.removeLastOrNull() }
        navigation.scrollTop = 0.0
        if (!attached) return

        setScrollTop(0.0)
        init()
    }

    fun refresh() {
        if (!attached) return

        val scroll = scrollTop
        init()
        setScrollTop(scroll)
    }

    fun <T> launch(
        call: suspend GroupsService.() -> T,
        onFailure: () -> Unit = {},
        onResult: (T) -> Unit,
    ) {
        GroupsClient.scope.launch {
            val result = runCatching { service.call() }

            Minecraft.getInstance().execute {
                result
                    .onSuccess(onResult)
                    .onFailure {
                        ADDON_LOGGER.warn("Groups request failed", it)
                        toast(tr("error.request"))
                        onFailure()
                    }
            }
        }
    }

    fun toast(message: McTextComponent) {
        val converter = RenderUtil.getTextConverter()
        SystemToast.addOrUpdate(
            Minecraft.getInstance().toasts,
            TOAST_ID,
            converter.convert(tr("error.title")),
            converter.convert(message),
        )
    }

    private fun fetchState() {
        if (fetchingState) return
        fetchingState = true

        launch({ fetchState() }, onFailure = { fetchingState = false }) {
            fetchingState = false
            refresh()
        }
    }

    private fun onStateChanged() {
        if (!attached) return
        val state = state ?: return
        val reset = resetInvalidPages(state)
        if (reset) setScrollTop(0.0)
        navigation.currentPage.onStateChanged()
        if (reset) refresh()
    }

    private fun resetInvalidPages(state: GroupsState): Boolean {
        val invalidIndex = navigation.pages.indexOfFirst { !it.isValid(state) }
        if (invalidIndex < 0) return false

        while (navigation.pages.size > invalidIndex) navigation.pages.removeLast()
        return true
    }

    private fun loadHead(player: GroupPlayer?) {
        if (player == null) return

        if (player.profile.properties.isEmpty()) {
            ModPlayerSkins.loadSkin(player.id, player.name, voiceClient.backgroundExecutor)
        } else {
            ModPlayerSkins.loadSkin(player.profile)
        }
    }

    private fun renderTrailing(
        context: GuiRenderContext,
        widgets: List<GuiAbstractWidget>,
        right: Int,
        y: Int,
        height: Int,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ): Int {
        var x = right
        for (widget in widgets.asReversed()) {
            x -= widget.width
            widget.x = x
            widget.y = y + (height - widget.height) / 2
            widget.render(context, mouseX, mouseY, delta)
            x -= 4
        }
        return x
    }

    private fun GuiRenderContext.drawLabels(
        title: McTextComponent,
        subtitle: McTextComponent?,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ): Int {
        val fontHeight = RenderUtil.getFontHeight()
        val textHeight = if (subtitle == null) fontHeight else fontHeight * 2 + LINE_GAP
        val top = y + (height - textHeight) / 2 + 1
        drawOrderedString(title, width, x, top, Colors.WHITE)
        subtitle?.let { drawOrderedString(it, width, x, top + fontHeight + LINE_GAP, Colors.GRAY) }
        return top
    }

    inner class CardEntry(
        private val head: GroupPlayer?,
        private val title: McTextComponent,
        private val subtitle: McTextComponent?,
        private val trailing: List<GuiAbstractWidget> = emptyList(),
        private val headSize: Int = 24,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(headSize + 6) {
        init {
            loadHead(head)
        }

        override fun render(
            context: GuiRenderContext,
            index: Int,
            x: Int,
            y: Int,
            entryWidth: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            delta: Float,
        ) {
            val rowHeight = height - 2
            val padding = (rowHeight - headSize) / 2
            context.drawRowBackground(x, y, entryWidth, rowHeight)
            context.drawHead(head, x + padding, y + padding, headSize)

            val textX = x + padding + headSize + 5
            val trailingX = renderTrailing(context, trailing, x + entryWidth - padding, y, rowHeight, mouseX, mouseY, delta)
            context.drawLabels(title, subtitle, textX, y, trailingX - textX, rowHeight)
        }

        override fun widgets(): List<GuiWidgetListener> = trailing
    }

    inner class MemberEntry(
        private val player: GroupPlayer,
        private val title: McTextComponent,
        private val status: McTextComponent,
        private val trailing: List<GuiAbstractWidget>,
        private val titleTooltip: McTextComponent? = null,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(30) {
        init {
            loadHead(player)
        }

        override fun render(
            context: GuiRenderContext,
            index: Int,
            x: Int,
            y: Int,
            entryWidth: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            delta: Float,
        ) {
            context.drawHead(player, x, y + (height - 24) / 2, 24)

            val textX = x + 30
            val trailingX = renderTrailing(context, trailing, x + entryWidth, y, height, mouseX, mouseY, delta)
            val titleY = context.drawLabels(title, status, textX, y, trailingX - textX, height)

            val titleHovered =
                mouseX >= textX &&
                    mouseX < textX + RenderUtil.getTextWidth(title) &&
                    mouseY >= titleY &&
                    mouseY < titleY + RenderUtil.getFontHeight()
            if (titleTooltip != null && titleHovered) setTooltip(titleTooltip, mouseX, mouseY)
        }

        override fun widgets(): List<GuiWidgetListener> = trailing
    }

    inner class HintEntry(
        private val value: () -> McTextComponent,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(HINT_HEIGHT) {
        override fun render(
            context: GuiRenderContext,
            index: Int,
            x: Int,
            y: Int,
            entryWidth: Int,
            mouseX: Int,
            mouseY: Int,
            hovered: Boolean,
            delta: Float,
        ) {
            context.drawCenteredString(value(), x + entryWidth / 2, y + 4, Colors.GRAY)
        }
    }

    private companion object {
        const val LINE_GAP = 2
        const val HINT_HEIGHT = 16

        val TOAST_ID = SystemToast.SystemToastId()
    }
}
