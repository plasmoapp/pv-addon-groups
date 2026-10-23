package su.plo.voice.groups.repository

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import su.plo.voice.groups.group.GroupData
import su.plo.voice.groups.util.serializer.UUIDSerializer
import java.io.File
import java.util.UUID

class JsonGroupRepository(
    private val dataFile: File,
) {
    fun load(): Snapshot? {
        val text = dataFile.takeIf { it.isFile }?.readText() ?: return null

        return runCatching {
            Json.decodeFromJsonElement<Snapshot>(
                Json.parseToJsonElement(text).migrateLegacyPlayers(),
            )
        }.getOrNull()
    }

    fun save(snapshot: Snapshot) {
        dataFile.writeText(Json.encodeToString(snapshot))
    }

    private fun JsonElement.migrateLegacyPlayers(): JsonElement {
        val groups = jsonObject["groups"]?.jsonArray ?: return this

        val migratedGroups =
            mapOf(
                "groups" to JsonArray(groups.map { it.jsonObject.migrateLegacyGroup() }),
            )

        return JsonObject(jsonObject + migratedGroups)
    }

    // 1.x stored members as bare UUIDs
    // until a member reconnects (see GroupsConnectionListener), their name uses UUID string as a placeholder
    private fun JsonObject.migrateLegacyGroup(): JsonObject {
        val playerIds = this["playersIds"]?.jsonArray ?: return this
        val owner = this["owner"] as? JsonObject

        val playersById =
            playerIds.associate { element ->
                val id = element.jsonPrimitive.content
                val profile =
                    owner?.takeIf { it["uuid"]?.jsonPrimitive?.content == id }
                        ?: JsonObject(mapOf("uuid" to JsonPrimitive(id), "name" to JsonPrimitive(id)))

                id to profile
            }

        return JsonObject(this - "playersIds" + ("playersById" to JsonObject(playersById)))
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
