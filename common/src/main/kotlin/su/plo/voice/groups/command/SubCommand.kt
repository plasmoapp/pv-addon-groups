package su.plo.voice.groups.command

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault

abstract class SubCommand(val handler: CommandHandler) {

    abstract val name: String

    abstract val permissions: List<Pair<String, PermissionDefault>>

    abstract fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String>

    abstract fun execute(
        source: McCommandSource,
        arguments: Array<String>,
    )

    open fun checkCanExecute(source: McCommandSource): Boolean = true
}
