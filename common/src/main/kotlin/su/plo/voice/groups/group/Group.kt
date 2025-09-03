package su.plo.voice.groups.group

import com.google.common.collect.Sets
import kotlinx.serialization.Serializable
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextClickEvent
import su.plo.slib.api.chat.style.McTextHoverEvent
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.api.server.audio.line.ServerPlayerSet
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.utils.extend.getVoicePlayer
import su.plo.voice.groups.utils.serializer.McGameProfileSerializer
import su.plo.voice.groups.utils.serializer.UUIDSerializer
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
open class GroupData(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    var name: String,
    var password: String? = null,
    var persistent: Boolean = false,
    val playersIds: MutableSet<@Serializable(with = UUIDSerializer::class) UUID> = Sets.newConcurrentHashSet(),
    val bannedPlayers: MutableList<@Serializable(with = McGameProfileSerializer::class) McGameProfile> = CopyOnWriteArrayList(),
    @Serializable(with = McGameProfileSerializer::class)
    var owner: McGameProfile? = null,
)

class Group(
    val playerSet: ServerPlayerSet,
    id: UUID,
    name: String,
    password: String? = null,
    persistent: Boolean = false,
    playersIds: MutableSet<UUID> = Sets.newConcurrentHashSet(),
    bannedPlayers: MutableList<McGameProfile> = CopyOnWriteArrayList(),
    owner: McGameProfile? = null,
) : GroupData(id, name, password, persistent, playersIds, bannedPlayers, owner) {

    var permissionsFilter: HashSet<String> = HashSet()

    val onlinePlayers: Collection<VoicePlayer>
        get() = playerSet.players

    val playerCount: Int
        get() = playersIds.size

    val onlinePlayerCount: Int
        get() = onlinePlayers.size

    val sortedOnlinePlayers: List<VoicePlayer>
        get() = onlinePlayers.sortedBy { it.instance.name }

    fun addPlayer(player: VoicePlayer) {
        playersIds.add(player.instance.uuid)
        onPlayerJoin(player)
    }

    fun removePlayer(player: VoicePlayer): Boolean =
        removePlayer(player.instance.uuid)

    fun removePlayer(playerUuid: UUID): Boolean =
        playersIds.remove(playerUuid).also {
            onPlayerQuit(playerUuid)
        }

    /**
     * Adds the player to online players
     */
    fun onPlayerJoin(player: VoicePlayer) {
        playerSet.addPlayer(player)
    }

    /**
     * Removes the player from online players
     */
    fun onPlayerQuit(player: VoicePlayer): Boolean =
        onPlayerQuit(player.instance.uuid)

    fun onPlayerQuit(playerUuid: UUID): Boolean =
        playerSet.removePlayer(playerUuid)

    fun isBanned(playerUuid: UUID): Boolean = bannedPlayers.any { it.id == playerUuid }

    fun banPlayer(player: VoicePlayer) {
        bannedPlayers.add(player.instance.gameProfile)
    }

    fun unbanPlayer(player: McGameProfile) =
        bannedPlayers.removeIf { it.id == player.id }

    fun isOwner(player: VoicePlayer): Boolean =
        owner?.id == player.instance.uuid

    fun hasPermission(source: McCommandSource): Boolean {
        if (permissionsFilter.isEmpty()) return true
        return permissionsFilter.any { source.hasPermission(it) }
    }

    private fun notifyPlayers(text: McTextComponent) {
        val component = McTextComponent.empty()
            .append(McTextComponent.translatable("pv.addon.groups.format.group_name", name))
            .append(McTextComponent.literal(" "))
            .append(text)
        onlinePlayers.forEach { it.instance.sendMessage(component) }
    }

    fun notifyPlayersTranslatable(key: String, vararg args: Any) =
        notifyPlayers(McTextComponent.translatable(key, *args))

    private val joinCommand = "/groups join $id"

    private val joinCommandWithPassword = "/groups join $id $password"

    private val leaveCommand = "/groups leave"

    fun joinButton() = McTextComponent.translatable("pv.addon.groups.button.join").let {
        if (password == null) {
            it.clickEvent(McTextClickEvent.runCommand(joinCommand))
        } else {
            it.clickEvent(McTextClickEvent.suggestCommand("$joinCommand "))
        }.hoverEvent(McTextHoverEvent.showText(McTextComponent.literal(joinCommand)))
    }

    fun joinButtonWithPassword() = McTextComponent.translatable("pv.addon.groups.button.join").let {
        if (password == null) {
            it.clickEvent(McTextClickEvent.runCommand(joinCommand))
                .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal(joinCommand)))
        } else {
            it.clickEvent(McTextClickEvent.runCommand(joinCommandWithPassword))
                .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal("$joinCommand *******")))
        }
    }

    fun inlineChatComponent(): McTextComponent =
        McTextComponent.translatable("pv.addon.groups.format.group_name", name)

    fun leaveButton(): McTextComponent = McTextComponent.translatable("pv.addon.groups.button.leave")
        .clickEvent(McTextClickEvent.runCommand(leaveCommand))
        .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal(leaveCommand)))
        .append(McTextComponent.literal("  "))
        .append(inviteButton())

    private fun inviteButton(): McTextComponent =
        McTextComponent.translatable("pv.addon.groups.button.invite")
            .clickEvent(McTextClickEvent.suggestCommand("/groups invite "))
            .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal("/groups invite")))

    fun asTextComponents(
        handler: CommandHandler,
        source: McCommandSource? = null,
        sourcePlayer: VoicePlayer? = source?.getVoicePlayer(handler.voiceServer),
    ): List<McTextComponent> = listOf(

        if (password != null) {
            McTextComponent.translatable("pv.addon.groups.icons.password_protected")
                .append(
                    McTextComponent.literal(" ")
                )
                .hoverEvent(
                    McTextHoverEvent.showText(
                        McTextComponent.translatable("pv.addon.groups.tooltip.password_protected")
                    )
                )
        } else {
            McTextComponent.empty()
        }.append(
            McTextComponent.translatable("pv.addon.groups.format.group_name", name)
                .hoverEvent(
                    McTextHoverEvent.showText(
                        McTextComponent.translatable("pv.addon.groups.tooltip.group_uuid", id.toString())
                    )
                )
                .clickEvent(McTextClickEvent.suggestCommand(id.toString()))
        ),

        if (owner == null || handler.addon.getVisibleOnlinePlayers(sourcePlayer).none { owner!!.id == it.instance.uuid }) {
            McTextComponent.translatable("pv.addon.groups.format.only_players", onlinePlayerCount)
        } else {
            McTextComponent.translatable(
                "pv.addon.groups.format.players_and_owner",
                onlinePlayerCount,
                owner!!.name
            )
        }.hoverEvent(
            McTextHoverEvent.showText(
                McTextComponent.literal(
                    sortedOnlinePlayers
                        .let {
                            val visiblePlayers = handler.addon.getVisibleOnlinePlayers(sourcePlayer)
                            sortedOnlinePlayers.filter { visiblePlayers.contains(it) }
                        }
                        .joinToString(", ") { it.instance.name }
                )
            )
        ),

        source?.getVoicePlayer(handler.voiceServer)
            ?.let { handler.groupManager.groupByPlayer[it.instance.uuid] }
            .let { if (id == it?.id) leaveButton() else joinButton() },
    )
}
