package su.plo.voice.groups.client.gui.widget

import su.plo.lib.mod.client.gui.components.TextFieldWidget
import su.plo.lib.mod.client.gui.widget.GuiAbstractWidget
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.voice.groups.client.gui.gray

class SearchFilter {
    private var query = ""

    private val widgets = mutableListOf<Pair<String, GuiAbstractWidget>>()

    fun field(placeholder: McTextComponent): TextFieldWidget {
        widgets.clear()

        return TextFieldWidget(0, 0, 0, 20, placeholder.gray()).apply {
            value = query
            setResponder { value ->
                query = value
                widgets.forEach { (text, widget) -> widget.isVisible = matches(text) }
            }
        }
    }

    fun <W : GuiAbstractWidget> track(
        text: String,
        widget: W,
    ): W {
        widgets += text to widget
        widget.isVisible = matches(text)

        return widget
    }

    private fun matches(text: String): Boolean = query.isBlank() || text.contains(query.trim(), ignoreCase = true)
}
