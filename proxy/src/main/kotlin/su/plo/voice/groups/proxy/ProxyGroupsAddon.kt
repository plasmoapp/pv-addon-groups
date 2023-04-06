package su.plo.voice.groups.proxy

import su.plo.lib.api.proxy.event.command.ProxyCommandsRegisterEvent
import su.plo.lib.api.server.MinecraftCommonServerLib
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.proxy.event.config.VoiceProxyConfigReloadedEvent
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.proxy.command.ProxyCommandHandler

@Addon(id = "pv-addon-groups", scope = AddonLoaderScope.PROXY, version = "1.0.3", authors = ["KPidS"])
class ProxyGroupsAddon : GroupsAddon() {

    init {
        ProxyCommandsRegisterEvent.registerListener { commandManager, minecraftProxy ->
            commandManager.register(
                "groups",
                createCommandHandler(minecraftProxy)
                    .also { addSubcommandsToCommandHandler(it) }
            )
        }
    }

    @EventSubscribe
    fun onConfigReloaded(event: VoiceProxyConfigReloadedEvent) {
        logger.warn("/vreload not supported")
    }

    override fun createCommandHandler(minecraftServer: MinecraftCommonServerLib): ProxyCommandHandler {
        return ProxyCommandHandler(this, minecraftServer)
    }
}
