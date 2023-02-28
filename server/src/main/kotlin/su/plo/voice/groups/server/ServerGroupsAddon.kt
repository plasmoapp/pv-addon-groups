package su.plo.voice.groups.server

import su.plo.voice.api.addon.AddonScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.event.VoiceServerShutdownEvent
import su.plo.voice.api.server.event.command.CommandsRegisterEvent
import su.plo.voice.api.server.event.config.VoiceServerConfigLoadedEvent
import su.plo.voice.groups.GroupsAddon

@Addon(id = "groups", scope = AddonScope.SERVER, version = "1.0.0", authors = ["KPidS"])
class ServerGroupsAddon : GroupsAddon() {

    @EventSubscribe
    fun onConfigLoaded(event: VoiceServerConfigLoadedEvent) =
        super.onConfigLoaded(event.server)

    @EventSubscribe
    fun onServerShutdown(event: VoiceServerShutdownEvent) =
        groupManager?.onVoiceServerShutdown(event.server)

    @EventSubscribe
    fun onCommandsRegister(event: CommandsRegisterEvent) {
        event.commandManager.register(
            "groups",
            createCommandHandler(event.voiceServer)
                .also { addSubcommandsToCommandHandler(it) }
        )
    }
}
