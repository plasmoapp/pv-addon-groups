package su.plo.voice.groups.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import su.plo.voice.groups.group.GroupData
import su.plo.voice.groups.util.serializer.UUIDSerializer
import java.io.File
import java.util.UUID

class JsonGroupRepository(
    private val dataFile: File,
) {
    fun load(): Snapshot? {
        val text = dataFile.takeIf { it.isFile }?.readText() ?: return null
        return runCatching { Json.decodeFromString<Snapshot>(text) }.getOrNull()
    }

    fun save(snapshot: Snapshot) {
        dataFile.writeText(Json.encodeToString(snapshot))
    }

    @Serializable
    data class Snapshot(
        val groups: List<GroupData>,
        val groupByPlayer: Map<
            @Serializable(with = UUIDSerializer::class)
            UUID,
            @Serializable(with = UUIDSerializer::class)
            UUID,
        >,
    )
}
