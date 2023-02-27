package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand

class SetCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "create"

    override val permissions = listOf(
        "set.owner" to PermissionDefault.TRUE,
        "set.*" to PermissionDefault.OP,
        "set.all" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {
        return listOf()
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {


    }
}