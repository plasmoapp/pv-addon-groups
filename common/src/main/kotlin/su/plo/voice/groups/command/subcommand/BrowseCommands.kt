package su.plo.voice.groups.command.subcommand

import com.mojang.brigadier.arguments.IntegerArgumentType
import dev.apehum.mcdsl.command.DslCommandBuilder
import dev.apehum.mcdsl.command.literalCommandBuilder
import su.plo.slib.api.chat.component.McTextComponent
import su.plo.slib.api.chat.style.McTextClickEvent
import su.plo.slib.api.chat.style.McTextHoverEvent
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.voice.groups.command.GroupsCommands
import su.plo.voice.groups.command.anyPermission
import su.plo.voice.groups.command.sender
import su.plo.voice.groups.command.tierPermissions
import su.plo.voice.groups.util.extend.asTextComponents
import su.plo.voice.groups.util.extend.checkAddonPermissionAndPrintError
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.printDivider
import su.plo.voice.groups.util.extend.sendFeedback
import su.plo.voice.groups.util.extend.sendTranslatable

private const val PER_PAGE = 3

fun GroupsCommands.browseCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("browse") {
        requires(anyPermission("browse"))

        val page by argumentOr("page", IntegerArgumentType.integer(1), 1)

        executes { browse(source, page) }
    }

fun GroupsCommands.browse(
    source: McBrigadierSource,
    page: Int,
) {
    if (source.checkAddonPermissionAndPrintError("browse")) return

    val chunks =
        groupManager.groups
            .let {
                when {
                    source.source.hasAddonPermission("browse.all") -> it
                    source.source.hasAddonPermission("browse.*") -> it
                    else -> it.filter { it.hasPermission(source.source) }
                }
            }.sortedByDescending { it.onlinePlayerCount }
            .chunked(PER_PAGE)
            .ifEmpty {
                source.printDivider()
                source.sendTranslatable("pv.addon.groups.command.browse.error.no_groups")
                source.printDivider()
                return
            }

    val chunk =
        chunks.getOrElse(page - 1) {
            source.sendTranslatable("pv.addon.groups.command.browse.error.page_not_found")
            return
        }

    source.printDivider()
    chunk.forEach { group ->
        source.sendFeedback(group.asTextComponents(this, source.source))
        source.printDivider()
    }
    source.sendFeedback(pagesInfo(page, chunks.size))
}

fun GroupsCommands.infoCommand(): DslCommandBuilder<McBrigadierSource> =
    literalCommandBuilder("info") {
        requires(tierPermissions("info", othersTier = "member"))

        executes {
            val (_, group) = requireGroup("info", othersTier = "member") ?: return@executes

            source.printDivider()
            source.sendFeedback(group.asTextComponents(this@infoCommand, sender))
            source.printDivider()
        }
    }

private fun pagesInfo(
    page: Int,
    chunksSize: Int,
): McTextComponent {
    val prevButton =
        if (page > 1) {
            val command = "/groups browse ${page - 1}"
            McTextComponent
                .translatable("pv.addon.groups.button.prev")
                .append(McTextComponent.literal(" "))
                .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal(command)))
                .clickEvent(McTextClickEvent.runCommand(command))
        } else {
            McTextComponent.empty()
        }

    val pageInfo = McTextComponent.translatable("pv.addon.groups.format.page", page, chunksSize)

    val nextButton =
        if (page < chunksSize) {
            val command = "/groups browse ${page + 1}"
            McTextComponent
                .literal(" ")
                .append(McTextComponent.translatable("pv.addon.groups.button.next"))
                .hoverEvent(McTextHoverEvent.showText(McTextComponent.literal(command)))
                .clickEvent(McTextClickEvent.runCommand(command))
        } else {
            McTextComponent.empty()
        }

    return prevButton
        .append(pageInfo)
        .append(nextButton)
}
