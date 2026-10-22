package su.plo.voice.groups.client.service

object GroupsClient {
    @JvmStatic
    val service: GroupsService by lazy { MockGroupsService() }
}
