package su.plo.voice.groups.command.subcommand

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.apehum.mcdsl.command.DslCommandBuilder
import dev.apehum.mcdsl.command.extension.suggests
import dev.apehum.mcdsl.command.literalCommandBuilder
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.slib.api.entity.player.McPlayer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group
import su.plo.voice.groups.api.result.InviteResult
import su.plo.voice.groups.api.result.JoinResult
import su.plo.voice.groups.api.result.LeaveResult
import su.plo.voice.groups.command.GroupsCommands
import su.plo.voice.groups.command.anyPermission
import su.plo.voice.groups.command.sender
import su.plo.voice.groups.command.tierPermissions
import su.plo.voice.groups.group.GroupImpl
import su.plo.voice.groups.util.extend.groupNotFoundError
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.inlineChatComponent
import su.plo.voice.groups.util.extend.notInGroupError
import su.plo.voice.groups.util.extend.parseUuidOrPrintError
import su.plo.voice.groups.util.extend.sendTranslatable

fun GroupsCommands.joinCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("join") {
        requires(anyPermission("join"))

        val groupId by argument("group", StringArgumentType.word()) {
            suggests { context ->
                joinableGroups(context.source.source)
                    .filter { it.id.toString().startsWith(remaining, true) }
                    .forEach { suggest(it.id.toString(), LiteralMessage(it.name)) }
            }
        }
        val password by argumentOr("password", StringArgumentType.greedyString(), "")

        executes {
            val player = requireExecutor() ?: return@executes

            val group =
                source
                    .parseUuidOrPrintError(groupId)
                    ?.let { groupManager.getGroup(it) ?: return@executes source.groupNotFoundError() }
                    ?: return@executes

            if (sender.hasAddonPermission("join.*")) {
                groupManager.joinUnchecked(player, group as GroupImpl)
                sendSelfOrOther(
                    player,
                    group,
                    "pv.addon.groups.command.join.success",
                    "pv.addon.groups.notifications.player_join",
                )
                return@executes
            }

            val result =
                groupManager.join(
                    player,
                    group,
                    password.ifEmpty { null },
                    ignorePermissionsFilter = sender.hasAddonPermission("join.all"),
                )

            when (result) {
                is JoinResult.Success -> {
                    sendSelfOrOther(
                        player,
                        group,
                        "pv.addon.groups.command.join.success",
                        "pv.addon.groups.notifications.player_join",
                    )
                }

                JoinResult.AlreadyInGroup -> {
                    source.sendTranslatable("pv.addon.groups.command.join.error.already_joined")
                }

                JoinResult.Banned -> {
                    source.sendTranslatable("pv.addon.groups.command.join.error.banned")
                }

                JoinResult.PasswordRequired -> {
                    source.sendTranslatable("pv.addon.groups.command.join.error.password_arg_missing")
                }

                JoinResult.WrongPassword -> {
                    source.sendTranslatable("pv.addon.groups.command.join.error.wrong_password")
                }

                JoinResult.NoPermission -> {
                    source.sendTranslatable("pv.addon.groups.command.join.error.no_permission")
                }
            }
        }
    }

fun GroupsCommands.leaveCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("leave") {
        requires(anyPermission("leave"))

        executes {
            val player = requireExecutor() ?: return@executes

            when (val result = groupManager.leave(player)) {
                is LeaveResult.Success -> {
                    sendSelfOrOther(
                        player,
                        result.group,
                        "pv.addon.groups.command.leave.success",
                        "pv.addon.groups.notifications.player_left",
                    )
                }

                LeaveResult.NotInGroup -> {
                    source.notInGroupError()
                }
            }
        }
    }

fun GroupsCommands.inviteCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("invite") {
        requires(tierPermissions("invite", othersTier = "member"))

        val targetsArgument by targets.players(this, "players") { source ->
            val (player, group) = senderGroup(source) ?: return@players emptyList()

            playerCollector
                .getVisibleOnlinePlayers(player)
                .filter { it !in group.onlinePlayers && it != player }
        }

        executes {
            val (player, group) = requireGroup("invite", othersTier = "member") ?: return@executes
            val players = targetsArgument.ifEmpty { return@executes }

            val single = players.size == 1
            players.forEach { target ->
                when (group.invite(player, target)) {
                    InviteResult.Success -> {
                        source.sendTranslatable("pv.addon.groups.command.invite.success", target.instance.name)
                    }

                    InviteResult.Self -> {
                        if (single) source.sendTranslatable("pv.addon.groups.command.invite.error.invite_self")
                    }

                    InviteResult.AlreadyInGroup -> {
                        if (single) source.sendTranslatable("pv.addon.groups.command.invite.error.already_joined")
                    }

                    InviteResult.Banned -> {
                        if (single) source.sendTranslatable("pv.addon.groups.command.invite.error.banned")
                    }
                }
            }
        }
    }

private fun GroupsCommands.joinableGroups(source: McCommandSource): Collection<Group> =
    if (source.hasAddonPermission("join.all") || source.hasAddonPermission("join.*")) {
        groupManager.groups
    } else {
        groupManager.groups.filter { it.hasPermission(source) }
    }

private fun CommandContext<McBrigadierSource>.sendSelfOrOther(
    player: VoicePlayer,
    group: Group,
    selfKey: String,
    otherKey: String,
) {
    val senderUuid = (sender as? McPlayer)?.uuid
    if (senderUuid == player.instance.uuid) {
        source.sendTranslatable(selfKey, group.name)
        return
    }

    player.instance.sendTranslatable(selfKey, group.name)

    // already got this from GroupsNotificationListener's broadcast
    if (group.onlinePlayers.any { it.instance.uuid == senderUuid }) return

    source.sendFeedback(
        McTextComponent
            .empty()
            .append(group.inlineChatComponent())
            .append(McTextComponent.literal(" "))
            .append(McTextComponent.translatable(otherKey, player.instance.name)),
    )
}
