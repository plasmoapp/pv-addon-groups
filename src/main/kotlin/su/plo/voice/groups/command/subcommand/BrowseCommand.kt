package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.chat.MinecraftTextHoverEvent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.group.Group
import java.util.*

class BrowseCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "browse"

    val perPage = 3

    override val permissions = listOf(
        "browse" to PermissionDefault.TRUE,
        "browse.all" to PermissionDefault.OP,
        "browse.*" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {
        return listOf()
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        if (!handler.hasPermission(source, "browse"))
            return handler.noPermission(source, "browse")

        val page = arguments.getOrNull(1)?.toIntOrNull() ?: 1

        val groups = handler.groupManager.groups.values.sortedBy { it.playerCount }
            .also {
                handler.printDivider(source)
            }
            .forEach {
                printGroup(it, source)
                handler.printDivider(source)
            }
//            .let {
//                it.sub(page.minus(1).times(perPage)..page)
//            }
//        val




    }

    private fun printGroup(group: Group, source: MinecraftCommandSource) {

        val passwordProtected = if (group.password != null) {
            MinecraftTextComponent.translatable("pv.addon.groups.icons.password_protected")
                .append(
                    MinecraftTextComponent.literal(" ")
                )
                .hoverEvent(MinecraftTextHoverEvent.showText(
                    MinecraftTextComponent.translatable("pv.addon.groups.tooltip.password_protected")
                ))
        } else {
            MinecraftTextComponent.empty()
        }

        val name = passwordProtected
            .append(
                MinecraftTextComponent.translatable("pv.addon.groups.format.group_name", group.name)
            )
            .hoverEvent(MinecraftTextHoverEvent.showText(
                MinecraftTextComponent.translatable("pv.addon.groups.format.group_uuid", group.id.toString())
            ))

        val players = if (group.owner == null) {
            MinecraftTextComponent.translatable("pv.addon.groups.format.only_players", group.playerCount)
        } else {
            MinecraftTextComponent.translatable("pv.addon.groups.format.players_and_owner", group.playerCount, group.owner!!.info.playerNick)
        }

        source.sendMessage(name)
        source.sendMessage(players)
    }

}