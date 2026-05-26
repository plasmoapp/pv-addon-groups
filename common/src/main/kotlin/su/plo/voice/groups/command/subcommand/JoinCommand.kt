package su.plo.voice.groups.command.subcommand

import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.groups.api.result.JoinResult
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.group.GroupImpl
import su.plo.voice.groups.util.extend.checkAddonPermissionAndPrintError
import su.plo.voice.groups.util.extend.getVoicePlayer
import su.plo.voice.groups.util.extend.groupNotFoundError
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.parseUuidOrPrintError
import su.plo.voice.groups.util.extend.playerOnlyCommandError
import su.plo.voice.groups.util.extend.sendTranslatable

class JoinCommand(
    handler: CommandHandler,
) : SubCommand(handler) {
    override val name = "join"

    override val permissions =
        listOf(
            "join" to PermissionDefault.TRUE,
            "join.all" to PermissionDefault.OP,
            "join.*" to PermissionDefault.OP,
        )

    override fun suggest(
        source: McCommandSource,
        arguments: Array<String>,
    ): List<String> {
        if (!handler.hasPermission(source, permissions.map { it.first }.toTypedArray())) return listOf()

        if (arguments.size == 2) {
            return listOf(handler.getTranslationByKey("pv.addon.groups.arg.groups_uuid", source))
        }

        if (arguments.size == 3) {
            val group =
                source
                    .parseUuidOrPrintError(arguments[1])
                    .let { it ?: return listOf() }
                    .let { handler.groupManager.getGroup(it) ?: return listOf() }

            if (group.password == null) return listOf()

            return listOf(handler.getTranslationByKey("pv.addon.groups.arg.password", source))
        }

        return listOf()
    }

    override fun execute(
        source: McCommandSource,
        arguments: Array<String>,
    ) {
        if (source.checkAddonPermissionAndPrintError("join")) return

        val player =
            source.getVoicePlayer(handler.voiceServer) ?: run {
                source.playerOnlyCommandError()
                return
            }

        val uuidArg =
            arguments.getOrNull(1) ?: kotlin.run {
                source.sendTranslatable("pv.addon.groups.command.join.error.usage")
                return
            }

        val group =
            source
                .parseUuidOrPrintError(uuidArg)
                .let { it ?: return }
                .let {
                    handler.groupManager.getGroup(it) ?: run {
                        source.groupNotFoundError()
                        return
                    }
                }

        if (source.hasAddonPermission("join.*")) {
            handler.groupManager.joinUnchecked(player, group as GroupImpl)
            source.sendTranslatable("pv.addon.groups.command.join.success", group.name)
            return
        }

        when (handler.groupManager.join(player, group, arguments.getOrNull(2))) {
            is JoinResult.Success -> source.sendTranslatable("pv.addon.groups.command.join.success", group.name)
            JoinResult.AlreadyInGroup -> source.sendTranslatable("pv.addon.groups.command.join.error.already_joined")
            JoinResult.Banned -> source.sendTranslatable("pv.addon.groups.command.join.error.banned")
            JoinResult.PasswordRequired -> source.sendTranslatable("pv.addon.groups.command.join.error.password_arg_missing")
            JoinResult.WrongPassword -> source.sendTranslatable("pv.addon.groups.command.join.error.wrong_password")
            JoinResult.NoPermission -> source.sendTranslatable("pv.addon.groups.command.join.error.no_permission")
        }
    }

    override fun checkCanExecute(source: McCommandSource): Boolean = source.hasAddonPermission("join")
}
