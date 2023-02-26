package su.plo.voice.groups.group

import su.plo.voice.api.server.player.VoicePlayer
import java.util.UUID

import kotlinx.serialization.*
import su.plo.lib.api.chat.MinecraftTextClickEvent
import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.chat.MinecraftTextHoverEvent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.utils.extend.getVoicePlayer
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

    fun isBanned(playerUuid: UUID): Boolean = bannedPlayers.contains(playerUuid)

    fun hasPermission(source: MinecraftCommandSource): Boolean {
        if (permissionsFilter.isEmpty()) return true
        return permissionsFilter.any { source.hasPermission(it) }
    }

    private fun notifyPlayers(text: MinecraftTextComponent) {
        val component = MinecraftTextComponent.translatable("pv.addon.groups.format.group_name")
            .append(MinecraftTextComponent.literal(" "))
            .append(text)
        players.forEach { it.instance.sendMessage(component) }
    }

    fun notifyPlayersTranslatable(key: String, vararg args: Any?) =
        notifyPlayers(MinecraftTextComponent.translatable(key, args))

    @Serializable
    data class Data(
        @Serializable(with = UUIDSerializer::class)
        val ownerUUID: UUID?,
        val players: Set<@Serializable(with = UUIDSerializer::class) UUID>,
        val data: Group,
    )

    private val joinCommand = "/groups join $id"

    private val leaveCommand = "/groups leave"

    private fun joinButton() = MinecraftTextComponent.translatable("pv.addon.groups.button.join").let {
        if (password == null) {
            it.clickEvent(MinecraftTextClickEvent.runCommand(joinCommand))
        } else {
            it.clickEvent(MinecraftTextClickEvent.suggestCommand("$joinCommand "))
        }.hoverEvent(MinecraftTextHoverEvent.showText(MinecraftTextComponent.literal(joinCommand)))
    }

    private fun leaveButton() = MinecraftTextComponent.translatable("pv.addon.groups.button.leave")
        .clickEvent(MinecraftTextClickEvent.runCommand(leaveCommand))
        .hoverEvent(MinecraftTextHoverEvent.showText(MinecraftTextComponent.literal(leaveCommand)))

    fun asTextComponents(handler: CommandHandler, source: MinecraftCommandSource? = null): List<MinecraftTextComponent> = listOf(

        if (password != null) {
            MinecraftTextComponent.translatable("pv.addon.groups.icons.password_protected")
                .append(
                    MinecraftTextComponent.literal(" ")
                )
                .hoverEvent(
                    MinecraftTextHoverEvent.showText(
                    MinecraftTextComponent.translatable("pv.addon.groups.tooltip.password_protected")
                ))
        } else {
            MinecraftTextComponent.empty()
        }.append(
            MinecraftTextComponent.translatable("pv.addon.groups.format.group_name", name)
                .hoverEvent(
                    MinecraftTextHoverEvent.showText(
                    MinecraftTextComponent.translatable("pv.addon.groups.tooltip.group_uuid", id.toString())
                ))
                .clickEvent(MinecraftTextClickEvent.suggestCommand(id.toString()))
        ),

        if (owner == null) {
            MinecraftTextComponent.translatable("pv.addon.groups.format.only_players", onlinePlayerCount)
        } else {
            MinecraftTextComponent.translatable("pv.addon.groups.format.players_and_owner", onlinePlayerCount, owner!!.name)
        }.hoverEvent(MinecraftTextHoverEvent.showText(
            MinecraftTextComponent.literal(sortedOnlinePlayers.joinToString(", ") { it.instance.name })
        )),

        source?.getVoicePlayer(handler.voiceServer)
            ?.let { handler.groupManager.groupByPlayer[it.instance.uuid] }
            .let { if (id == it?.id) leaveButton() else joinButton() },
    )
}