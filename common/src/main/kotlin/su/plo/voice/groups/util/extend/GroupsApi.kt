package su.plo.voice.groups.util.extend

import su.plo.voice.groups.api.GroupCreateBuilder
import su.plo.voice.groups.api.GroupsApi
import su.plo.voice.groups.api.result.CreateResult

fun GroupsApi.createGroup(block: GroupCreateBuilder.() -> Unit): CreateResult = createGroup().apply(block).create()
