package su.plo.voice.groups.client.gui

import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.sounds.SoundEvents
import su.plo.lib.mod.client.gui.components.AbstractScrollbar
import su.plo.lib.mod.client.gui.components.Button
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.lib.mod.client.gui.widget.GuiWidgetListener
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.gui.GuiCursor
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.lib.mod.client.render.texture.ModPlayerSkins
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.client.config.hotkey.Hotkeys
import su.plo.voice.client.config.VoiceClientConfig
import su.plo.voice.client.extension.TextKt
import su.plo.voice.client.gui.settings.VoiceSettingsScreen
import su.plo.voice.client.gui.settings.tab.TabWidget
import su.plo.voice.groups.client.gui.page.GroupsPage
import su.plo.voice.groups.client.gui.widget.ContextMenuButton
import su.plo.voice.groups.client.gui.widget.OpenButton
import su.plo.voice.groups.client.service.GroupsClient
import su.plo.voice.groups.client.service.GroupsService
import su.plo.voice.groups.client.service.GroupsStateListener
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.ActionError
import su.plo.voice.groups.protocol.ActionResult
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupsState
import java.awt.Color

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

    val screen: VoiceSettingsScreen
        get() = parent

    val hotkeys: Hotkeys
        get() = voiceClient.hotkeys

    val volumes: VoiceClientConfig.Voice.SourceLineVolumes
        get() = config.voice.volumes

    val rowWidth: Int
        get() = containerWidth

    val fieldWidth: Int
        get() = ELEMENT_WIDTH + 24

    override fun init() {
        val openMenuKey = (screen.overlay as? ContextMenuButton)?.key
        if (openMenuKey != null) screen.overlay = null
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
        openMenuKey?.let(::reopenMenu)

        if (restoreScroll) setScrollTop(navigation.scrollTop)
    }

    override fun removed() {
        attached = false
        service.removeListener(stateListener)
        navigation.scrollTop = scrollTop
        if (screen.overlay is ContextMenuButton) screen.overlay = null
        super.removed()
    }

    override fun mouseClicked(
        mouseX: Double,
        mouseY: Double,
        button: Int,
    ): Boolean {
        (screen.overlay as? ContextMenuButton)?.let { menu ->
            menu.menuClicked(mouseX, mouseY)
            return true
        }

        return super.mouseClicked(mouseX, mouseY, button)
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

    fun <R : ActionResult> perform(
        button: GuiAbstractWidget?,
        call: suspend GroupsService.() -> R,
        onFailure: (R) -> Unit = { toast(it) },
        onSettled: () -> Unit = {},
        onSuccess: () -> Unit = { refresh() },
    ) {
        val page = navigation.currentPage
        button?.isActive = false

        launch(
            call,
            onFailure = {
                onSettled()
                button?.isActive = true
            },
        ) { result ->
            onSettled()
            if (result is ActionError) button?.isActive = true
            when {
                navigation.currentPage !== page -> toast(result)
                result is ActionError -> onFailure(result)
                else -> onSuccess()
            }
        }
    }

    fun toast(result: ActionResult) {
        if (result is ActionError) toast(result.message())
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

    private fun reopenMenu(key: Any) {
        screen.overlay =
            entries
                .asSequence()
                .flatMap { it.widgets() }
                .filterIsInstance<ContextMenuButton>()
                .firstOrNull { it.key == key }
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

        var visible: Boolean = true
            set(value) {
                field = value
                setHeight(if (value) headSize + 6 else 0)
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
            if (!visible) return

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

    inner class HeaderEntry(
        private val title: McTextComponent,
        private val backButton: Button,
        private val trailing: List<GuiAbstractWidget>,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(28) {
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
            backButton.x = x
            backButton.y = y + 2
            backButton.render(context, mouseX, mouseY, delta)

            renderTrailing(context, trailing, x + entryWidth, y + 2, 20, mouseX, mouseY, delta)

            context.drawCenteredString(title, x + entryWidth / 2, y + 8, Colors.WHITE)
        }

        override fun widgets(): List<GuiWidgetListener> = listOf(backButton) + trailing
    }

    inner class LinkEntry(
        private val title: McTextComponent,
        private val subtitle: McTextComponent,
        private val onClick: () -> Unit,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(30) {
        private val openButton = OpenButton(onClick)

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
            context.drawRowBackground(x, y, entryWidth, rowHeight)
            if (hovered) {
                context.fill(x, y, x + entryWidth, y + rowHeight, HOVER_COLOR)
                context.requestCursor(GuiCursor.POINTING_HAND)
            }

            openButton.rowHovered = hovered
            val padding = (rowHeight - openButton.height) / 2
            val trailingX =
                renderTrailing(
                    context,
                    listOf(openButton),
                    x + entryWidth - padding,
                    y,
                    rowHeight,
                    mouseX,
                    mouseY,
                    delta,
                )
            context.drawLabels(title, subtitle, x + 4, y, trailingX - x - 4, rowHeight)
        }

        override fun mouseClicked(
            mouseX: Double,
            mouseY: Double,
            button: Int,
        ): Boolean {
            if (super.mouseClicked(mouseX, mouseY, button)) return true
            if (button != 0) return false

            Minecraft.getInstance().soundManager.play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1f, 0.25f),
            )
            onClick()
            return true
        }

        override fun widgets(): List<GuiWidgetListener> = listOf(openButton)
    }

    inner class SectionEntry(
        private val title: McTextComponent,
        private val trailing: List<GuiAbstractWidget>,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(24) {
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
            renderTrailing(context, trailing, x + entryWidth, y, height, mouseX, mouseY, delta)

            val titleY = y + (height - RenderUtil.getFontHeight()) / 2
            context.drawCenteredString(title, x + entryWidth / 2, titleY, Colors.WHITE)
        }

        override fun widgets(): List<GuiWidgetListener> = trailing
    }

    inner class LabeledEntry(
        private val label: McTextComponent,
        private val widget: GuiAbstractWidget,
        private val tooltip: McTextComponent? = null,
        private val lockedReason: McTextComponent? = null,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(24) {
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
            context.drawString(label, x, y + height / 2 - RenderUtil.getFontHeight() / 2, Colors.WHITE)
            widget.x = x + entryWidth - widget.width
            widget.y = y + (height - widget.height) / 2
            widget.render(context, mouseX, mouseY, delta)
            if (tooltip != null && hovered && mouseX < widget.x) setTooltip(tooltip, mouseX, mouseY)

            val widgetHovered =
                mouseX >= widget.x &&
                    mouseX < widget.x + widget.width &&
                    mouseY >= widget.y &&
                    mouseY < widget.y + widget.height
            if (lockedReason != null && widgetHovered) {
                context.requestCursor(GuiCursor.NOT_ALLOWED)
                setTooltip(lockedReason, mouseX, mouseY)
            }
        }

        override fun widgets(): List<GuiWidgetListener> = listOf(widget)
    }

    inner class MessageEntry(
        private val messages: List<McTextComponent>,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(0) {
        private var wrappedWidth = -1
        private var lines: List<String> = emptyList()

        init {
            wrap(containerWidth)
        }

        private fun wrap(entryWidth: Int) {
            if (entryWidth == wrappedWidth) return
            wrappedWidth = entryWidth

            lines =
                messages.flatMap { message ->
                    TextKt.getStringSplitToWidth(
                        RenderUtil.getFormattedString(message),
                        (entryWidth - MESSAGE_PADDING * 2).toFloat(),
                        true,
                        true,
                    )
                }
            setHeight(lines.size * lineHeight + 12)
        }

        override fun updatePosition(
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
            wrap(entryWidth)
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
            context.drawRowBackground(x, y, entryWidth, height)
            lines.forEachIndexed { line, value ->
                val lineX = x + (entryWidth - RenderUtil.getStringWidth(value)) / 2
                context.drawString(value, lineX, y + 7 + line * lineHeight, Colors.WHITE)
            }
        }
    }

    inner class HintEntry(
        private val value: () -> McTextComponent,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(HINT_HEIGHT) {
        var visible: Boolean = true
            set(value) {
                field = value
                setHeight(if (value) HINT_HEIGHT else 0)
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
            if (!visible) return
            context.drawCenteredString(value(), x + entryWidth / 2, y + 4, Colors.GRAY)
        }
    }

    inner class SpacerEntry(
        height: Int,
    ) : AbstractScrollbar<VoiceSettingsScreen>.Entry(height) {
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
        ) = Unit
    }

    private companion object {
        const val LINE_GAP = 2
        const val HINT_HEIGHT = 16
        const val MESSAGE_PADDING = 8

        val lineHeight: Int
            get() = RenderUtil.getFontHeight() + LINE_GAP

        val TOAST_ID = SystemToast.SystemToastId()

        val HOVER_COLOR = Color(255, 255, 255, 24)
    }
}
