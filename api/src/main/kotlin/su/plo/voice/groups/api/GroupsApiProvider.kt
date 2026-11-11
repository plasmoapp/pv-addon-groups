package su.plo.voice.groups.api

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
object GroupsApiProvider {
    var instance: GroupsApi? = null

    fun require(): GroupsApi = instance ?: throw IllegalStateException("GroupsApi is not initialized")
}
