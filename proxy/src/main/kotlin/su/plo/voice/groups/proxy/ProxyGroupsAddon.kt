package su.plo.voice.groups.proxy

import su.plo.slib.api.proxy.McProxyLib
import su.plo.slib.api.proxy.command.McProxyCommand
import su.plo.slib.api.proxy.event.command.McProxyCommandsRegisterEvent
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.groups.BuildConstants
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.util.PlayerCollector

@Addon(
    id = "pv-addon-groups",
    scope = AddonLoaderScope.PROXY,
    version = BuildConstants.VERSION,
    authors = ["KPidS", "Apehum"],
)
class ProxyGroupsAddon : GroupsAddon() {
    private val playerCollector: PlayerCollector = {
        voiceServer.playerManager.players
    }

    init {
        McProxyCommandsRegisterEvent.registerListener { commandManager, minecraftProxy ->
            commandManager.register(
                "groups",
                createCommandHandler(minecraftProxy)
                    .also { addSubcommandsToCommandHandler(it) },
            )
        }
    }

    private fun createCommandHandler(minecraftProxy: McProxyLib) =
        object :
            CommandHandler(this, minecraftProxy, playerCollector),
            McProxyCommand {}
}
