package su.plo.voice.groups.client.gui.page

import net.minecraft.client.Minecraft
import su.plo.lib.mod.client.gui.components.Button
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextStyle
import su.plo.voice.client.gui.settings.widget.CompositeRowWidget
import su.plo.voice.groups.client.gui.addonIcon
import su.plo.voice.groups.client.gui.gray
import su.plo.voice.groups.client.gui.pvIcon
import su.plo.voice.groups.client.gui.text
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.protocol.GroupAction
import su.plo.voice.groups.protocol.GroupDetails
import su.plo.voice.groups.protocol.GroupsState
import su.plo.voice.groups.protocol.UpdateSettingsRequest
import java.util.UUID

class SettingsPage(
    private val groupId: UUID,
) : GroupsPage() {
    private var draft: Draft? = null
    private var passwordVisible = false
    private var savedAt = 0L
    private var saving = false

    override fun isValid(state: GroupsState): Boolean =
        state.current?.let { it.summary.id == groupId && it.actions.any { action -> action in ACTIONS } } == true

    override fun init() {
        val details = details() ?: return
        val draft = draft ?: Draft(details).also { draft = it }
        val actions = details.actions

        addHeader(tr("settings.title"), SaveButton(), onBack = { close() })

        if (GroupAction.SET_NAME in actions) {
            val capabilities = state.capabilities
            val nameLengthHint =
                tab.HintEntry {
                    tr(
                        "settings.name.length",
                        capabilities.minNameLength,
                        capabilities.maxNameLength,
                    ).withStyle(McTextStyle.RED)
                }
            nameLengthHint.visible = !isNameValid()

            add(
                tab.LabeledEntry(
                    tr("settings.name"),
                    textField(tr("settings.name.placeholder"), draft.name, tab.fieldWidth) {
                        draft.name = it
                        nameLengthHint.visible = !isNameValid()
                    },
                ),
            )
            add(nameLengthHint)
        }

        if (GroupAction.SET_PASSWORD in actions) {
            add(tab.LabeledEntry(tr("settings.password"), passwordField()))
        }

        if (GroupAction.SET_PERMISSIONS in actions) {
            add(
                tab.LabeledEntry(
                    tr("settings.permissions"),
                    textField(
                        tr("settings.permissions.placeholder"),
                        draft.permissions,
                        tab.fieldWidth,
                    ) { draft.permissions = it },
                ),
            )
        }

        if (GroupAction.SET_PERSISTENT in actions) {
            addToggle(
                tr("settings.persistent"),
                tr("settings.persistent.description"),
                draft.persistent,
            ) { draft.persistent = it }
        }

        if (GroupAction.UNBAN in actions) {
            add(
                tab.LinkEntry(
                    tr("banned.title"),
                    tr("settings.banned", details.banned.orEmpty().size).gray(),
                ) { tab.open(BannedPage(groupId)) },
            )
        }

        if (GroupAction.DELETE in actions) {
            addRow(
                button(tr("settings.delete").withStyle(McTextStyle.RED), width = 0) {
                    confirm(
                        tr("settings.delete"),
                        listOf(tr("settings.delete.message", details.summary.name)),
                        tr("settings.delete.confirm").withStyle(McTextStyle.RED),
                        pops = 2,
                    ) { delete(groupId) }
                },
            )
        }
    }

    private fun details(): GroupDetails? = tab.state?.current?.takeIf { it.summary.id == groupId }

    private fun changes(): UpdateSettingsRequest? {
        val details = details() ?: return null
        val draft = draft ?: return null
        val permissions =
            draft.permissions
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()

        fun <T> T.changed(
            action: GroupAction,
            current: T,
        ): T? = takeIf { action in details.actions && it != current }

        return UpdateSettingsRequest(
            groupId,
            name = draft.name.changed(GroupAction.SET_NAME, details.summary.name),
            password = draft.password.changed(GroupAction.SET_PASSWORD, details.password.orEmpty()),
            permissions = permissions.changed(GroupAction.SET_PERMISSIONS, details.permissions.orEmpty()),
            persistent = draft.persistent.changed(GroupAction.SET_PERSISTENT, details.summary.persistent),
        ).takeIf { it != UpdateSettingsRequest(groupId) }
    }

    private fun isNameValid(): Boolean {
        val name = draft?.name ?: return true
        val capabilities = state.capabilities

        return name.length in capabilities.minNameLength..capabilities.maxNameLength
    }

    private fun close() {
        if (changes() == null) {
            tab.back()
            return
        }

        confirm(
            tr("settings.unsaved.title"),
            listOf(tr("settings.unsaved.message", details()?.summary?.name.orEmpty())),
            tr("settings.unsaved.confirm").withStyle(McTextStyle.RED),
            pops = 2,
            action = null,
        )
    }

    private fun passwordField(): GuiAbstractWidget {
        val draft = checkNotNull(draft)
        val field = textField(tr("settings.password.placeholder"), draft.password) { draft.password = it }
        field.setFormatter { value, _ -> text(if (passwordVisible) value else "*".repeat(value.length)) }

        val visibility =
            iconButton({ pvIcon(if (passwordVisible) "eye" else "eye_disabled") }, tr("password.toggle")) {
                passwordVisible = !passwordVisible
            }
        val copy =
            iconButton(addonIcon("copy"), tr("settings.password.copy")) {
                Minecraft.getInstance().keyboardHandler.clipboard = field.value
            }

        return CompositeRowWidget(
            0,
            0,
            tab.fieldWidth,
            20,
            2,
            field,
            visibility,
            copy,
        )
    }

    private class Draft(
        details: GroupDetails,
    ) {
        var name = details.summary.name
        var password = details.password.orEmpty()
        var permissions = details.permissions.orEmpty().joinToString(", ")
        var persistent = details.summary.persistent
    }

    private inner class SaveButton :
        Button(
            0,
            0,
            fitWidth(tr("settings.save"), tr("settings.saving"), tr("settings.saved")),
            20,
            McTextComponent.empty(),
            NO_ACTION,
            NO_TOOLTIP,
        ) {
        override fun onPress() {
            val request = changes() ?: return
            saving = true
            tab.perform(
                this,
                { updateSettings(request) },
                onSettled = { saving = false },
            ) {
                savedAt = System.currentTimeMillis()
                draft = details()?.let(::Draft)
                tab.refresh()
            }
        }

        override fun render(
            context: GuiRenderContext,
            mouseX: Int,
            mouseY: Int,
            delta: Float,
        ) {
            val dirty = changes() != null
            val recentlySaved = System.currentTimeMillis() - savedAt < SAVED_LABEL_MILLIS
            isActive = dirty && isNameValid() && !saving
            text =
                when {
                    saving -> tr("settings.saving").gray()
                    dirty -> tr("settings.save").withStyle(McTextStyle.YELLOW)
                    recentlySaved -> tr("settings.saved").withStyle(McTextStyle.GREEN)
                    else -> tr("settings.save")
                }
            super.render(context, mouseX, mouseY, delta)
        }
    }

    companion object {
        val ACTIONS =
            setOf(
                GroupAction.SET_NAME,
                GroupAction.SET_PASSWORD,
                GroupAction.SET_PERMISSIONS,
                GroupAction.SET_PERSISTENT,
                GroupAction.UNBAN,
                GroupAction.DELETE,
            )

        private const val SAVED_LABEL_MILLIS = 3000
    }
}
