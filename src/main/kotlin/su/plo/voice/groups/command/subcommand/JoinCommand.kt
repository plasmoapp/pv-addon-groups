package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.lib.api.server.player.MinecraftServerPlayer
import su.plo.voice.api.server.player.VoiceServerPlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.group.Group
import su.plo.voice.groups.utils.extend.*
import java.util.*

class JoinCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "join"

    override val permissions = listOf(
        "join" to PermissionDefault.TRUE,
        "join.all" to PermissionDefault.OP,
        "join.*" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {
        if (!handler.hasPermission(source, permissions.map { it.first }.toTypedArray())) return listOf()

        if (arguments.size == 2) {
            return listOf(handler.getTranslationByKey("pv.addon.groups.arg.groups_uuid", source))
        }

        if (arguments.size == 3) {

            val group = source.parseUuidOrPrintError(arguments[1])
                .let { it ?: return listOf() }
                .let { handler.groupManager.groups[it] ?: return listOf() }

            if (group.password == null) return listOf()

//            var passwordArg = arguments.getOrNull(2) ?: return listOf()

            return listOf(handler.getTranslationByKey("pv.addon.groups.arg.password", source))
        }

        return listOf()
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        if (source.checkAddonPermissionAndPrintError("join")) return

        val player = source.getVoicePlayer(handler.voiceServer) ?: return source.playerOnlyCommandError()

        val group = source.parseUuidOrPrintError(arguments[1])
            .let { it ?: return }
            .let { handler.groupManager.groups[it] ?: return source.groupNotFoundError() }

        handler.groupManager.groupByPlayer[player.instance.uuid]
            ?.also { if (group.id == it.id) {
                source.sendTranslatable("pv.addon.groups.command.join.error.already_joined")
                return
            }}
            ?.let {
                handler.groupManager.leave(player)
                it.notifyPlayersTranslatable("pv.addon.groups.notifications.player_left", player.instance.name)
            }

        if (checkCantJoin(group, player, source, arguments)) return

        group.notifyPlayersTranslatable("pv.addon.groups.notifications.player_join", player.instance.name)

        handler.groupManager.join(player, group)

        source.sendTranslatable("pv.addon.groups.command.join.success", group.name)
    }

    override fun checkCanExecute(source: MinecraftCommandSource): Boolean = source.hasAddonPermission("join")

    private fun checkCantJoin(
        group: Group,
        player: VoiceServerPlayer,
        source: MinecraftCommandSource,
        arguments: Array<out String>
    ): Boolean {

        if (source.hasAddonPermission("join.add") || source.hasAddonPermission("join.*")) {
            return false
        }

        if (group.isBanned(player.instance.uuid)) {
            source.sendTranslatable("pv.addon.groups.command.join.error.banned")
            return true
        }

        if (!group.hasPermission(source)) {
            source.sendTranslatable("pv.addon.groups.command.join.error.no_permission")
            return true
        }

        if (group.password != null) {

            var passwordArg = arguments.getOrNull(2) ?: run {
                source.sendTranslatable("pv.addon.groups.command.join.error.password_arg_missing")
                return true
            }

            if (group.password != passwordArg) {
                source.sendTranslatable("pv.addon.groups.command.join.error.wrong_password")
                return true
            }

        }

        return false
    }
}