package su.plo.voice.groups.group

import su.plo.voice.api.server.player.VoicePlayer
import java.util.UUID

import kotlinx.serialization.*
import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.voice.groups.utils.serializer.UUIDSerializer
import su.plo.voice.proto.data.player.MinecraftGameProfile

@Serializable
class Group(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    var name: String,
    var password: String? = null,
    var persistent: Boolean = false,
) {
    @Transient
    var owner: MinecraftGameProfile? = null
    @Transient
    val players: HashSet<VoicePlayer> = HashSet()
    val bannedPlayers: HashSet<@Serializable(with = UUIDSerializer::class) UUID> = HashSet()
    var permissionsFilter: HashSet<String> = HashSet()

    val onlinePlayers: List<VoicePlayer>
        get() = players.filter { it.instance.isOnline }

    val playerCount: Int get() = players.size

    val onlinePlayerCount: Int get() = onlinePlayers.size

    val sortedOnlinePlayers: List<VoicePlayer>
        get() = onlinePlayers.sortedBy { it.instance.name }

    fun notifyPlayers(text: MinecraftTextComponent) {
        val component = MinecraftTextComponent.translatable("pv.addon.groups.format.group_name")
            .append(MinecraftTextComponent.literal(" "))
            .append(text)
        players.forEach { it.instance.sendMessage(component) }
    }

    fun notifyPlayersTranslatable(key: String, args: Any? = null) =
        notifyPlayers(MinecraftTextComponent.translatable(key, args))

    @Serializable
    data class Data(
        @Serializable(with = UUIDSerializer::class)
        val ownerUUID: UUID?,
        val players: Set<@Serializable(with = UUIDSerializer::class) UUID>,
        val data: Group,
    )
}