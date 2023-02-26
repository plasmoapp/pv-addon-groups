package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.chat.MinecraftTextClickEvent
import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.chat.MinecraftTextHoverEvent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.group.Group
import su.plo.voice.groups.utils.extend.*
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

        if (source.checkAddonPermissionAndPrintError("browse")) return

        val page = arguments.getOrNull(1)?.toIntOrNull() ?: 1

        handler.groupManager.groups.values.sortedByDescending { it.playerCount }
            .also { source.printDivider() }
            .forEach {
                source.printDivider()
                source.sendMessage(it.asTextComponents(handler, source))
            }
    }

}