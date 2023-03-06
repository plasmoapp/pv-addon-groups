package su.plo.voice.groups.proxy

import su.plo.voice.api.addon.AddonScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.proxy.event.VoiceProxyShutdownEvent
import su.plo.voice.api.proxy.event.command.CommandsRegisterEvent
import su.plo.voice.api.proxy.event.config.VoiceProxyConfigLoadedEvent
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.proxy.command.ProxyCommandHandler

@Addon(id = "groups", scope = AddonScope.PROXY, version = "1.0.0", authors = ["KPidS"])
class ProxyGroupsAddon : GroupsAddon() {

    @EventSubscribe
    fun onConfigLoaded(event: VoiceProxyConfigLoadedEvent) {
        super.onConfigLoaded(event.proxy)
    }

    @EventSubscribe
    fun onServerShutdown(event: VoiceProxyShutdownEvent) =
        groupManager?.onVoiceServerShutdown(event.proxy)

    @EventSubscribe
    fun onCommandsRegister(event: CommandsRegisterEvent) {
        event.commandManager.register(
            "groups",
            createCommandHandler(event.voiceProxy)
                .also { addSubcommandsToCommandHandler(it) }
        )
    }

    override fun createCommandHandler(voiceServer: PlasmoBaseVoiceServer): ProxyCommandHandler {
        return ProxyCommandHandler(voiceServer, this)
    }
}
