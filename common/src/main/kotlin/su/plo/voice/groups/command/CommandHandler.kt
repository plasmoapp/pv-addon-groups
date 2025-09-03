package su.plo.voice.groups.command

import su.plo.slib.api.McLib
import su.plo.slib.api.command.McCommand
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.utils.extend.sendTranslatable
import java.util.concurrent.ConcurrentHashMap

open class CommandHandler(
    val addon: GroupsAddon,
    val minecraftServer: McLib,
): McCommand {

    private val subCommands: MutableMap<String, SubCommand> = ConcurrentHashMap()

    val groupManager
        get() = addon.groupManager

    val voiceServer
        get() = addon.voiceServer

    fun getTranslationByKey(key: String, source: McCommandSource): String {
        return voiceServer.languages.getServerLanguage(source)[key] ?: key
    }

    fun <T : SubCommand> addSubCommand(subCommand: (handler: CommandHandler) -> T): CommandHandler {
        subCommand(this)
            .also { subCommands[it.name] = it }
            .also { registerPermissions(it.permissions) }
        return this
    }

    val flags = listOf(
        "name" to PermissionDefault.TRUE,
        "password" to PermissionDefault.TRUE,
        "permissions" to PermissionDefault.OP,
        "persistent" to PermissionDefault.OP,
    ).toMap()

    init {
        flags.map { "flag.${it.key}" to it.value }
            .also { registerPermissions(it) }
    }

    override fun execute(source: McCommandSource, arguments: Array<String>) {

        val subCommand = arguments.getOrNull(0) ?: run {
            subCommands["browse"]?.execute(source, arguments)
            return
        }

        subCommands[subCommand]?.let {
            it.execute(source, arguments)
            return
        }

        source.sendTranslatable(
            "pv.addon.groups.error.unknown_subcommand",
            subCommands.keys.joinToString(", ")
        )
    }

    override fun suggest(source: McCommandSource, arguments: Array<String>): List<String> {

        if (arguments.isEmpty()) return subCommands
            .filter { it.value.checkCanExecute(source) }
            .keys
            .toList()

        val subCommand = arguments.getOrNull(0) ?: return listOf()

        if (arguments.size == 1) return subCommands
            .filter { it.key.startsWith(subCommand) && it.value.checkCanExecute(source) }
            .keys
            .toList()

        subCommands[subCommand]?.let { return it.suggest(source, arguments) }

        return listOf()
    }

    override fun hasPermission(source: McCommandSource, arguments: Array<String>?): Boolean =
        source.hasPermission("pv.addon.groups.*") ||
            subCommands.keys.stream().anyMatch {
                source.hasPermission("pv.addon.groups.$it")
            }

    fun registerPermissions(permissions: List<Pair<String, PermissionDefault>>) {
        permissions.forEach { minecraftServer.permissionManager.register("pv.addon.groups.${it.first}", it.second) }
    }
}
