package su.plo.voice.groups.command

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommand
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.utils.extend.hasAddonPermission
import su.plo.voice.groups.utils.extend.sendTranslatable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors

class CommandHandler(
    val voiceServer: PlasmoVoiceServer,
    val addon: GroupsAddon,
): MinecraftCommand {

    private val subCommands: MutableMap<String, SubCommand> = ConcurrentHashMap()

    val groupManager
        get() = addon.groupManager!!

    fun getTranslationByKey(key: String, source: MinecraftCommandSource): String {
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

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        val subCommand = arguments.getOrNull(0) ?: run {
            subCommands["browse"]?.execute(source, arguments)
            return
        }

        subCommands[subCommand]?.let {
            it.execute(source, arguments)
            return
        }

        source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.command.usage"))
    }

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {

        if (arguments.isEmpty()) return subCommands.keys.stream()
            .filter { command -> source.hasPermission(command) }
            .collect(Collectors.toList())

        val subCommand = arguments[0]

        if (arguments.size == 1) return subCommands
            .filter { it.key.startsWith(subCommand) && it.value.checkCanExecute(source) }
            .keys
            .toList()

        subCommands[subCommand]?.let { return it.suggest(source, arguments) }

        return listOf()
    }

    override fun hasPermission(source: MinecraftCommandSource, arguments: Array<out String?>?): Boolean =
        source.hasPermission("pv.addon.groups.*") ||
            subCommands.keys.stream().anyMatch {
                source.hasPermission("pv.addon.groups.$it")
            }

    private fun registerPermissions(permissions: List<Pair<String, PermissionDefault>>) {
        permissions.forEach { voiceServer.minecraftServer.permissionsManager.register("pv.addon.groups.${it.first}", it.second) }
    }
}