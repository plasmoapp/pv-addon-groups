package su.plo.voice.groups.command.subcommand

import su.plo.lib.api.chat.MinecraftTextClickEvent
import su.plo.lib.api.chat.MinecraftTextComponent
import su.plo.lib.api.chat.MinecraftTextHoverEvent
import su.plo.lib.api.server.command.MinecraftCommandSource
import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.SubCommand
import su.plo.voice.groups.utils.extend.*

class BrowseCommand(handler: CommandHandler): SubCommand(handler) {

    override val name = "browse"

    private val perPage = 3

    override val permissions = listOf(
        "browse" to PermissionDefault.TRUE,
        "browse.all" to PermissionDefault.OP,
        "browse.*" to PermissionDefault.OP,
    )

    override fun suggest(source: MinecraftCommandSource, arguments: Array<out String>): List<String> {

        if (arguments.size == 2) {
            return listOf(handler.getTranslationByKey("pv.addon.groups.arg.page", source))
        }

        return listOf()
    }

    override fun execute(source: MinecraftCommandSource, arguments: Array<out String>) {

        if (source.checkAddonPermissionAndPrintError("browse")) return

        val page = arguments.getOrNull(1)?.toIntOrNull() ?: 1

        val chunks = handler.groupManager.groups.values
            .let { when {
                source.hasAddonPermission("browse.all") -> it
                source.hasAddonPermission("browse.*") -> it
                else -> it.filter { it.hasPermission(source) }
            }}
            .sortedByDescending { it.onlinePlayerCount }
            .chunked(perPage)
            .ifEmpty {
                source.printDivider()
                source.sendTranslatable("pv.addon.groups.command.browse.error.no_groups")
                source.printDivider()
                return
            }

        chunks
            .getOrElse(page - 1) {
                source.sendTranslatable("pv.addon.groups.command.browse.error.page_not_found")
                return
            }
            .let { chunk ->
                source.printDivider()
                chunk.forEach { group ->
                    source.sendMessage(group.asTextComponents(handler, source))
                    source.printDivider()
                }
                source.sendMessage(
                    getPagesInfo(page, chunks.size)
                )
            }
    }

    private fun getPagesInfo(
        page: Int,
        chunksSize: Int
    ): MinecraftTextComponent {

        val prevButton = if (page > 1) {
            val command = "/groups browse ${page - 1}"
            MinecraftTextComponent.translatable("pv.addon.groups.button.prev")
                .append(MinecraftTextComponent.literal(" "))
                .hoverEvent(MinecraftTextHoverEvent.showText(MinecraftTextComponent.literal(command)))
                .clickEvent(MinecraftTextClickEvent.runCommand(command))
        } else {
            MinecraftTextComponent.empty()
        }

        val pageInfo = MinecraftTextComponent.translatable("pv.addon.groups.format.page", page, chunksSize)

        val nextButton = if (page < chunksSize) {
            val command = "/groups browse ${page + 1}"
            MinecraftTextComponent.literal(" ")
                .append(MinecraftTextComponent.translatable("pv.addon.groups.button.next"))
                .hoverEvent(MinecraftTextHoverEvent.showText(MinecraftTextComponent.literal(command)))
                .clickEvent(MinecraftTextClickEvent.runCommand(command))
        } else {
            MinecraftTextComponent.empty()
        }

        return prevButton
            .append(pageInfo)
            .append(nextButton)
    }

    override fun checkCanExecute(source: MinecraftCommandSource): Boolean = source.hasAddonPermission("browse")
}