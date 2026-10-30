package su.plo.voice.groups.client.gui

import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.components.toasts.SystemToast
import su.plo.lib.mod.client.gui.components.CompositeColumnWidget
import su.plo.lib.mod.client.gui.components.TextWidget
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.lib.mod.client.render.Colors
import su.plo.lib.mod.client.render.RenderUtil
import su.plo.lib.mod.client.render.texture.ModPlayerSkins
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.client.audio.capture.ClientActivationManager
import su.plo.voice.api.client.config.hotkey.Hotkeys
import su.plo.voice.client.config.VoiceClientConfig
import su.plo.voice.client.gui.settings.VoiceSettingsScreen
import su.plo.voice.client.gui.settings.tab.AbstractHotKeysTabWidget
import su.plo.voice.client.gui.settings.widget.ActivationThresholdWidget
import su.plo.voice.client.gui.settings.widget.OptionWidget
import su.plo.voice.groups.client.gui.page.GroupsPage
import su.plo.voice.groups.client.gui.widget.ContextMenuButton
import su.plo.voice.groups.client.service.GroupsClient
import su.plo.voice.groups.client.service.GroupsService
import su.plo.voice.groups.client.service.GroupsStateListener
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.ActionError
import su.plo.voice.groups.protocol.ActionResult
import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupsState

class GroupsTabWidget(
    parent: VoiceSettingsScreen,
    voiceClient: PlasmoVoiceClient,
    config: VoiceClientConfig,
    private val navigation: GroupsNavigationState,
) : AbstractHotKeysTabWidget(parent, voiceClient, config) {
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

    val activations: ClientActivationManager
        get() = voiceClient.activationManager

    val activationConfigs: VoiceClientConfig.Activations
        get() = config.activations

    val fieldWidth: Int
        get() = ELEMENT_WIDTH + 24

    val optionWidth: Int
        get() = ELEMENT_WIDTH

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
            addSection().add(TextWidget(tr("loading"), Colors.GRAY, true))
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

        if (restoreScroll) restoreScroll(navigation.scrollTop)
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

    public override fun addSection(): CompositeColumnWidget = super.addSection()

    fun open(page: GroupsPage) {
        navigation.pages.addLast(page)
        navigation.scrollTop = 0.0
        if (!attached) return

        scrollTop = 0.0
        init()
    }

    fun back(count: Int = 1) {
        repeat(count) { navigation.pages.removeLastOrNull() }
        navigation.scrollTop = 0.0
        if (!attached) return

        scrollTop = 0.0
        init()
    }

    fun refresh() {
        if (!attached) return

        val scroll = scrollTop
        init()
        restoreScroll(scroll)
    }

    fun isShown(page: GroupsPage): Boolean = attached && navigation.currentPage === page

    fun thresholdOption(): OptionWidget<ActivationThresholdWidget> {
        val entry = config.voice.activationThreshold
        val slider =
            ActivationThresholdWidget(
                parent,
                entry,
                voiceClient.audioCapture,
                voiceClient.deviceManager,
                parent.testController,
                0,
                0,
                ELEMENT_WIDTH - 24,
                20,
            )

        return OptionWidget(
            parent,
            McTextComponent.translatable("gui.plasmovoice.devices.activation_threshold"),
            slider,
            entry,
            McTextComponent.translatable("gui.plasmovoice.devices.activation_threshold.tooltip"),
            null,
            slider.buttons,
        )
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
            //~ if >=1.21.2 '.toasts' -> '.toastManager'
            //~ if >=26.2 '.toastManager' -> '.gui.toastManager()'
            Minecraft.getInstance().toasts,
            TOAST_ID,
            converter.convert(tr("error.title")),
            converter.convert(message),
        )
    }

    fun loadHead(player: GroupPlayer?) {
        if (player == null) return

        if (player.profile.properties.isEmpty()) {
            ModPlayerSkins.loadSkin(player.id, player.name, voiceClient.backgroundExecutor)
        } else {
            ModPlayerSkins.loadSkin(player.profile)
        }
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
        if (reset) scrollTop = 0.0
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
            descendants()
                .filterIsInstance<ContextMenuButton>()
                .firstOrNull { it.key == key }
    }

    private fun restoreScroll(value: Double) {
        place()
        scrollTop = value
    }

    private companion object {
        val TOAST_ID = SystemToast.SystemToastId()
    }
}
