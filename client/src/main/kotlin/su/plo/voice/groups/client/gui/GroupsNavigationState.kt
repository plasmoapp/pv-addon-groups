package su.plo.voice.groups.client.gui

import su.plo.voice.groups.client.gui.page.GroupsPage
import su.plo.voice.groups.client.gui.page.MainPage

class GroupsNavigationState {
    val root: GroupsPage = MainPage()
    val pages = ArrayDeque<GroupsPage>()
    var scrollTop = 0.0

    val currentPage: GroupsPage
        get() = pages.lastOrNull() ?: root
}
