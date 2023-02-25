package su.plo.voice.groups.command

import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault

abstract class SubCommand(val handler: CommandHandler) {

    abstract val name: String

    abstract val permissions: List<Pair<String, PermissionDefault>>
    abstract fun suggest(
        source: MinecraftCommandSource,
        arguments: Array<out String>,
    ): List<String>
    abstract fun execute(
        source: MinecraftCommandSource,
        arguments: Array<out String>,
    )

//    abstract fun registerPermissions(): SubCommand
}