package su.plo.voice.groups.client.service

import su.plo.voice.groups.protocol.GroupPlayer
import su.plo.voice.groups.protocol.GroupsState

typealias GroupsStateListener = (GroupsState) -> Unit

interface GroupsService {
    val available: Boolean

    val self: GroupPlayer

    val state: GroupsState?

    fun addListener(listener: GroupsStateListener)

    fun removeListener(listener: GroupsStateListener)

    suspend fun fetchState(): GroupsState
}
