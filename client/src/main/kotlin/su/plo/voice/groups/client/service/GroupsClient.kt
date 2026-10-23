package su.plo.voice.groups.client.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object GroupsClient {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @JvmStatic
    lateinit var service: GroupsService
}
