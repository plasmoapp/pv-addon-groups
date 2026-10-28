package su.plo.voice.groups.client.gui.page

import su.plo.slib.api.chat.style.McTextStyle
import su.plo.voice.groups.client.gui.groupStatus
import su.plo.voice.groups.client.gui.pvIcon
import su.plo.voice.groups.client.gui.text
import su.plo.voice.groups.client.gui.tr
import su.plo.voice.groups.protocol.GroupSummary
import su.plo.voice.groups.protocol.JoinResult
import su.plo.voice.groups.protocol.WrongPassword

class PasswordPage(
    private val group: GroupSummary,
) : GroupsPage() {
    private var password = ""
    private var wrongPassword = false
    private var passwordVisible = false

    override fun init() {
        val title = tr("main.join.title", group.name)
        val page = addCategory(title, leading = listOf(backButton()))
        page.add(card(group.owner, text(group.name), groupStatus(group)))

        val field =
            textField(tr("password.placeholder"), password) {
                if (it == password) return@textField
                password = it
                wrongPassword = false
            }
        field.setFormatter { value, _ -> text(if (passwordVisible) value else "*".repeat(value.length)) }

        page.add(
            row(
                field,
                iconButton({ pvIcon(if (passwordVisible) "eye" else "eye_disabled") }, tr("password.toggle")) {
                    passwordVisible = !passwordVisible
                },
                button(tr("join")) { button ->
                    tab.leaveCurrentThen(button, title, pops = 1, onFailure = ::onJoinFailed) {
                        join(group.id, password)
                    }
                },
            ),
        )
        page.add(hint { if (wrongPassword) tr("password.wrong").withStyle(McTextStyle.RED) else tr("password.hint") })
    }

    private fun onJoinFailed(result: JoinResult) {
        if (result != WrongPassword) {
            tab.toast(result)
            return
        }

        wrongPassword = true
        tab.refresh()
    }
}
