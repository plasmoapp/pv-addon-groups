package su.plo.voice.groups.command.subcommand

import com.mojang.brigadier.arguments.StringArgumentType
import dev.apehum.mcdsl.command.DslCommandBuilder
import dev.apehum.mcdsl.command.extension.suggests
import dev.apehum.mcdsl.command.literalCommandBuilder
import su.plo.slib.api.command.McCommandSource
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.groups.api.result.BanResult
import su.plo.voice.groups.api.result.KickResult
import su.plo.voice.groups.api.result.TransferResult
import su.plo.voice.groups.command.GroupsCommands
import su.plo.voice.groups.command.tierPermissions
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

        val newOwnerArgument by targets.profile(
            this,
            "player",
            lookup = { groupPlayers(it) },
            suggestions = { transferSuggestions(it) },
        )

        executes {
            val (_, group) = requireGroup("transfer") ?: return@executes
            val newOwner = newOwnerArgument ?: return@executes

            when (group.transferOwnership(newOwner.id)) {
                TransferResult.Success -> {}

                TransferResult.NotInGroup -> {
                    source.playerNotFoundError()
                }

                TransferResult.AlreadyOwner -> {
                    source.sendTranslatable("pv.addon.groups.command.transfer.error.already_owner", newOwner.name)
                }
            }
        }
    }

fun GroupsCommands.kickCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("kick") {
        requires(tierPermissions("kick"))

        val targetsArgument by targets.profiles(
            this,
            "players",
            lookup = { groupPlayers(it) },
            suggestions = { otherGroupPlayers(it) },
        )

        executes {
            val (player, group) = requireGroup("kick") ?: return@executes
            val players = targetsArgument.ifEmpty { return@executes }

            val single = players.size == 1
            players
                .filter { single || it.id != player.instance.uuid }
                .forEach { target ->
                    when (group.kick(target.id)) {
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

        val targetsArgument by targets.profiles(
            this,
            "players",
            lookup = { banTargets(it) },
            suggestions = { banSuggestions(it) },
        )

        executes {
            val (player, group) = requireGroup("ban") ?: return@executes
            val players = targetsArgument.ifEmpty { return@executes }

            val single = players.size == 1
            players.forEach { target ->
                if (target.id == player.instance.uuid) {
                    if (single) source.sendTranslatable("pv.addon.groups.command.ban.error.ban_self")
                    return@forEach
                }

                if (group.isBanned(target.id)) {
                    if (single) source.sendTranslatable("pv.addon.groups.command.ban.error.already_banned")
                    return@forEach
                }

                when (group.ban(target)) {
                    BanResult.Success -> {}

                    BanResult.Owner -> {
                        if (single) source.sendTranslatable("pv.addon.groups.command.ban.error.ban_owner")
                    }
                }
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

private fun GroupsCommands.groupPlayers(source: McCommandSource): Collection<McGameProfile> = senderGroup(source)?.group?.players.orEmpty()

private fun GroupsCommands.otherGroupPlayers(source: McCommandSource): List<McGameProfile> {
    val (player, group) = senderGroup(source) ?: return emptyList()

    return group.players.filter { it.id != player.instance.uuid }
}

private fun GroupsCommands.transferSuggestions(source: McCommandSource): List<McGameProfile> {
    val group = senderGroup(source)?.group ?: return emptyList()

    return group.players.filter { it.id != group.owner?.id }
}

private fun GroupsCommands.banTargets(source: McCommandSource): List<McGameProfile> {
    val (player, group) = senderGroup(source) ?: return emptyList()
    val onlinePlayers =
        playerCollector
            .getVisibleOnlinePlayers(player)
            .map { it.instance.gameProfile }

    return (group.players + onlinePlayers).distinctBy { it.id }
}

private fun GroupsCommands.banSuggestions(source: McCommandSource): List<McGameProfile> {
    val (player, group) = senderGroup(source) ?: return emptyList()

    return banTargets(source).filter {
        it.id != player.instance.uuid && it.id != group.owner?.id && !group.isBanned(it.id)
    }
}
