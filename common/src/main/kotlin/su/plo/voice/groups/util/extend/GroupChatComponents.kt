package su.plo.voice.groups.util.extend

import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextClickEvent
import su.plo.slib.api.chat.style.McTextHoverEvent
import su.plo.slib.api.command.McCommandSource
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group
import su.plo.voice.groups.command.CommandHandler

fun Group.notifyPlayersTranslatable(
    key: String,
    vararg args: Any,
) {
    val component =
        McTextComponent
            .empty()
            .append(McTextComponent.translatable("pv.addon.groups.format.group_name", name))
            .append(McTextComponent.literal(" "))
            .append(McTextComponent.translatable(key, *args))
    onlinePlayers.forEach { it.instance.sendMessage(component) }
}

private val Group.joinCommand get() = "/groups join $id"
private val Group.joinCommandWithPassword get() = "/groups join $id $password"
private const val LEAVE_COMMAND = "/groups leave"

fun Group.joinButton(): McTextComponent =
    McTextComponent.translatable("pv.addon.groups.button.join").let {
        if (password == null) {
            it.clickEvent(McTextClickEvent.runCommand(joinCommand))
        } else {
            it.clickEvent(McTextClickEvent.suggestCommand("$joinCommand "))
        }.hoverEvent(McTextHoverEvent.showText(McTextComponent.literal(joinCommand)))
    }

fun Group.joinButtonWithPassword(): McTextComponent =
    McTextComponent.translatable("pv.addon.groups.button.join").let {
        if (password == null) {
            it
                .clickEvent(McTextClickEvent.runCommand(joinCommand))
                .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal(joinCommand)))
        } else {
            it
                .clickEvent(McTextClickEvent.runCommand(joinCommandWithPassword))
                .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal("$joinCommand *******")))
        }
    }

fun Group.inlineChatComponent(): McTextComponent = McTextComponent.translatable("pv.addon.groups.format.group_name", name)

fun Group.leaveButton(): McTextComponent =
    McTextComponent
        .translatable("pv.addon.groups.button.leave")
        .clickEvent(McTextClickEvent.runCommand(LEAVE_COMMAND))
        .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal(LEAVE_COMMAND)))
        .append(McTextComponent.literal("  "))
        .append(inviteButton())

private fun inviteButton(): McTextComponent =
    McTextComponent
        .translatable("pv.addon.groups.button.invite")
        .clickEvent(McTextClickEvent.suggestCommand("/groups invite "))
        .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal("/groups invite")))

fun Group.asTextComponents(
    handler: CommandHandler,
    source: McCommandSource? = null,
    sourcePlayer: VoicePlayer? = source?.getVoicePlayer(handler.voiceServer),
): List<McTextComponent> =
    listOf(
        if (password != null) {
            McTextComponent
                .translatable("pv.addon.groups.icons.password_protected")
                .append(
                    McTextComponent.literal(" "),
                ).hoverEvent(
                    McTextHoverEvent.showText(
                        McTextComponent.translatable("pv.addon.groups.tooltip.password_protected"),
                    ),
                )
        } else {
            McTextComponent.empty()
        }.append(
            McTextComponent
                .translatable("pv.addon.groups.format.group_name", name)
                .hoverEvent(
                    McTextHoverEvent.showText(
                        McTextComponent.translatable("pv.addon.groups.tooltip.group_uuid", id.toString()),
                    ),
                ).clickEvent(McTextClickEvent.suggestCommand(id.toString())),
        ),
        if (owner == null || handler.playerCollector.getVisibleOnlinePlayers(sourcePlayer).none { owner!!.id == it.instance.uuid }) {
            McTextComponent.translatable("pv.addon.groups.format.only_players", onlinePlayerCount)
        } else {
            McTextComponent.translatable(
                "pv.addon.groups.format.players_and_owner",
                onlinePlayerCount,
                owner!!.name,
            )
        }.hoverEvent(
            McTextHoverEvent.showText(
                McTextComponent.literal(
                    onlinePlayers.sortedBy { it.instance.name }
                        .let { sortedPlayers ->
                            val visiblePlayers = handler.playerCollector.getVisibleOnlinePlayers(sourcePlayer)
                            sortedPlayers.filter { visiblePlayers.contains(it) }
                        }.joinToString(", ") { it.instance.name },
                ),
            ),
        ),
        source
            ?.getVoicePlayer(handler.voiceServer)
            ?.let { handler.groupManager.getPlayerGroup(it.instance.uuid) }
            .let { if (id == it?.id) leaveButton() else joinButton() },
    )
