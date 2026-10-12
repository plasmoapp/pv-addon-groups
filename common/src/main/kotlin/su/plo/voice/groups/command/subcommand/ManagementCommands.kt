package su.plo.voice.groups.command.subcommand

import com.mojang.brigadier.arguments.StringArgumentType
import dev.apehum.mcdsl.command.DslCommandBuilder
import dev.apehum.mcdsl.command.extension.suggests
import dev.apehum.mcdsl.command.literalCommandBuilder
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.voice.groups.api.result.KickResult
import su.plo.voice.groups.api.result.TransferResult
import su.plo.voice.groups.command.GroupsCommands
import su.plo.voice.groups.command.tierPermissions
import su.plo.voice.groups.util.extend.getVoicePlayer
import su.plo.voice.groups.util.extend.playerNotFoundError
import su.plo.voice.groups.util.extend.sendTranslatable

fun GroupsCommands.deleteCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("delete") {
        requires(tierPermissions("delete"))

        executes {
            val (_, group) = requireGroup("delete") ?: return@executes

            groupManager.deleteGroup(group)
        }
    }

fun GroupsCommands.transferCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("transfer") {
        requires(tierPermissions("transfer"))

        val newOwnerArgument by targets.player(this, "player") { source ->
            val player = source.getVoicePlayer(voiceServer)

            playerCollector
                .getVisibleOnlinePlayers(player)
                .filter { it != player }
        }

        executes {
            val (player, group) = requireGroup("transfer") ?: return@executes
            val newOwner = newOwnerArgument ?: return@executes

            if (newOwner.instance.uuid == player.instance.uuid) {
                source.sendTranslatable("pv.addon.groups.command.transfer.error.already_owner")
                return@executes
            }

            when (group.transferOwnership(newOwner)) {
                TransferResult.Success -> {}

                TransferResult.NotInGroup -> {
                    source.playerNotFoundError()
                }
            }
        }
    }

fun GroupsCommands.kickCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("kick") {
        requires(tierPermissions("kick"))

        val targetsArgument by targets.players(this, "players") { source ->
            val (player, group) = senderGroup(source) ?: return@players emptyList()

            group.onlinePlayers.filter { it != player }
        }

        executes {
            val (player, group) = requireGroup("kick") ?: return@executes
            val players = targetsArgument.ifEmpty { return@executes }

            val single = players.size == 1
            players
                .filter { single || it != player }
                .forEach { target ->
                    when (group.kick(target)) {
                        KickResult.Success -> {}

                        KickResult.NotInGroup -> {
                            if (single) source.sendTranslatable("pv.addon.groups.command.kick.error.not_in_group")
                        }
                    }
                }
        }
    }

fun GroupsCommands.banCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("ban") {
        requires(tierPermissions("ban"))

        val targetsArgument by targets.players(this, "players") { source ->
            val (player, group) = senderGroup(source) ?: return@players emptyList()

            playerCollector
                .getVisibleOnlinePlayers(player)
                .filter { !group.isBanned(it.instance.uuid) && it != player }
        }

        executes {
            val (player, group) = requireGroup("ban") ?: return@executes
            val players = targetsArgument.ifEmpty { return@executes }

            val single = players.size == 1
            players
                .filter { single || it != player }
                .forEach { target ->
                    if (group.isBanned(target.instance.uuid)) {
                        if (single) source.sendTranslatable("pv.addon.groups.command.ban.error.already_banned")
                        return@forEach
                    }

                    group.ban(target)
                }
        }
    }

fun GroupsCommands.unbanCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("unban") {
        requires(tierPermissions("ban"))

        val playerName by argument("player", StringArgumentType.word()) {
            suggests { context ->
                val (player, group) = senderGroup(context.source.source) ?: return@suggests

                group.bannedPlayers
                    .filter { it.id != player.instance.uuid && it.name.startsWith(remaining, true) }
                    .forEach { suggest(it.name) }
            }
        }

        executes {
            val (_, group) = requireGroup("ban") ?: return@executes

            val target =
                group.bannedPlayers.firstOrNull { it.name == playerName }
                    ?: return@executes source.sendTranslatable("pv.addon.groups.command.unban.error.not_banned")

            group.unban(target)
        }
    }
