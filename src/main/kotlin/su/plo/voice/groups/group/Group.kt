package su.plo.voice.groups.group

import kotlinx.serialization.Serializable
import su.plo.lib.api.chat.MinecraftTextClickEvent
import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.chat.MinecraftTextHoverEvent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.voice.api.server.audio.line.ServerPlayersSet
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.utils.extend.getVoicePlayer
import su.plo.voice.groups.utils.serializer.UUIDSerializer
import su.plo.voice.proto.data.player.MinecraftGameProfile
import java.util.*

@Serializable
data class GroupOfflineData(
    @Serializable(with = UUIDSerializer::class)
    val ownerUUID: UUID?,
    val players: Set<@Serializable(with = UUIDSerializer::class) UUID>,
    val data: GroupData
)

@Serializable
open class GroupData(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    var name: String,
    var password: String? = null,
    var persistent: Boolean = false,
)

class Group(
    val playersSet: ServerPlayersSet,
    id: UUID,
    name: String,
    password: String? = null,
    persistent: Boolean = false,
) : GroupData(id, name, password, persistent) {
    var owner: MinecraftGameProfile? = null
    val bannedPlayers: HashSet<@Serializable(with = UUIDSerializer::class) UUID> = HashSet()
    var permissionsFilter: HashSet<String> = HashSet()

    val players: Collection<VoicePlayer> = playersSet.players

    val onlinePlayers: List<VoicePlayer>
        get() = playersSet.players.filter { it.instance.isOnline }

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
        val component = MinecraftTextComponent.translatable("pv.addon.groups.format.group_name", name)
            .append(MinecraftTextComponent.literal(" "))
            .append(text)
        players.forEach { it.instance.sendMessage(component) }
    }

    fun notifyPlayersTranslatable(key: String, vararg args: Any?) =
        notifyPlayers(MinecraftTextComponent.translatable(key, *args))

    private val joinCommand = "/groups join $id"

    private val joinCommandWithPassword = "/groups join $id $password"

    private val leaveCommand = "/groups leave"

    fun joinButton() = MinecraftTextComponent.translatable("pv.addon.groups.button.join").let {
        if (password == null) {
            it.clickEvent(MinecraftTextClickEvent.runCommand(joinCommand))
        } else {
            it.clickEvent(MinecraftTextClickEvent.suggestCommand("$joinCommand "))
        }.hoverEvent(MinecraftTextHoverEvent.showText(MinecraftTextComponent.literal(joinCommand)))
    }

    fun joinButtonWithPassword() = MinecraftTextComponent.translatable("pv.addon.groups.button.join").let {
        if (password == null) {
            it.clickEvent(MinecraftTextClickEvent.runCommand(joinCommand))
                .hoverEvent(MinecraftTextHoverEvent.showText(MinecraftTextComponent.literal(joinCommand)))
        } else {
            it.clickEvent(MinecraftTextClickEvent.runCommand(joinCommandWithPassword))
                .hoverEvent(MinecraftTextHoverEvent.showText(MinecraftTextComponent.literal("$joinCommand *******")))
        }
    }

    fun inlineChatComponent(): MinecraftTextComponent = MinecraftTextComponent.translatable("pv.addon.groups.format.group_name", name)

    fun leaveButton(): MinecraftTextComponent = MinecraftTextComponent.translatable("pv.addon.groups.button.leave")
        .clickEvent(MinecraftTextClickEvent.runCommand(leaveCommand))
        .hoverEvent(MinecraftTextHoverEvent.showText(MinecraftTextComponent.literal(leaveCommand)))

    fun asOfflineData() =
        GroupOfflineData(
            owner?.id,
            players.map { it.instance.uuid }.toSet(),
            this
        )

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
