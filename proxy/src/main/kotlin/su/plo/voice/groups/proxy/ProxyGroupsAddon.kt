package su.plo.voice.groups.proxy

import su.plo.slib.api.McLib
import su.plo.slib.api.proxy.event.command.McProxyCommandsRegisterEvent
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.BuildConstants
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.proxy.command.ProxyCommandHandler

@Addon(id = "pv-addon-groups", scope = AddonLoaderScope.PROXY, version = BuildConstants.VERSION, authors = ["KPidS"])
class ProxyGroupsAddon : GroupsAddon() {

    init {
        McProxyCommandsRegisterEvent.registerListener { commandManager, minecraftProxy ->
            commandManager.register(
                "groups",
                createCommandHandler(minecraftProxy)
                    .also { addSubcommandsToCommandHandler(it) }
            )
        }
    }

    override fun getVisibleOnlinePlayers(player: VoicePlayer?): Collection<VoicePlayer> =
        voiceServer.playerManager.players

    override fun createCommandHandler(minecraftServer: McLib): ProxyCommandHandler {
        return ProxyCommandHandler(this, minecraftServer)
    }
}
