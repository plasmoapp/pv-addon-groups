package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.components.TextFieldWidget
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.groups.client.gui.GroupsTabWidget.CardEntry
import su.plo.voice.groups.client.gui.gray

class SearchFilter {
    private var query = ""

    private val entries = mutableListOf<Pair<String, CardEntry>>()

    fun field(placeholder: McTextComponent): TextFieldWidget {
        entries.clear()

        return TextFieldWidget(0, 0, 0, 20, placeholder.gray()).apply {
            value = query
            setResponder { value ->
                query = value
                entries.forEach { (text, entry) -> entry.visible = matches(text) }
            }
        }
    }

    fun track(
        text: String,
        entry: CardEntry,
    ): CardEntry {
        entries += text to entry
        entry.visible = matches(text)

        return entry
    }

    private fun matches(text: String): Boolean = query.isBlank() || text.contains(query.trim(), ignoreCase = true)
}
