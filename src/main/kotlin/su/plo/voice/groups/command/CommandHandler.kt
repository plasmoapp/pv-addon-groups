package su.plo.voice.groups.command

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommand
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.groups.GroupsAddon
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

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        subCommands[arguments[0]]?.let {
            it.execute(source, arguments)
            return
        }

        source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.command.usage"))
        return
    }

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {

        if (arguments.isEmpty()) return subCommands.keys.stream()
            .filter { command -> hasPermission(source, command) }
            .collect(Collectors.toList())

        val subCommand = arguments[0]

        if (arguments.size == 1) return subCommands.keys.stream()
            .filter { command -> command.startsWith(subCommand) }
            .filter { command -> hasPermission(source, command) }
            .collect(Collectors.toList())

        subCommands[subCommand]?.let { return it.suggest(source, arguments) }

        return listOf()
    }

    override fun hasPermission(source: MinecraftCommandSource, arguments: Array<out String?>?): Boolean {
        return source.hasPermission("pv.addon.groups.*") ||
            subCommands.keys.stream().anyMatch {
                source.hasPermission("pv.addon.groups.$it")
            }
    }

    fun checkNotNullAndNoPermission(
        value: Any?,
        source: MinecraftCommandSource,
        permission: String
    ): Boolean {
        return ((value != null) && !hasPermission(source, permission))
            .also { if (it) noPermission(source, permission) }
    }

    fun hasPermission(source: MinecraftCommandSource, command: String): Boolean {
        return source.hasPermission("pv.addon.groups.*") ||
                source.hasPermission("pv.addon.groups.$command")
    }

    fun noPermission(source: MinecraftCommandSource, permission: String) {
        source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.command.error.no_permission", permission))
    }

    fun printDivider(source: MinecraftCommandSource) {
        source.sendMessage(MinecraftTextComponent.translatable("pv.addon.groups.divider"))
    }

    private fun registerPermissions(permissions: List<Pair<String, PermissionDefault>>) {
        permissions.forEach { voiceServer.minecraftServer.permissionsManager.register("pv.addon.groups.${it.first}", it.second) }
    }
}