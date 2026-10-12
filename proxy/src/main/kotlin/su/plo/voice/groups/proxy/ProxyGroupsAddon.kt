package su.plo.voice.groups.proxy

import su.plo.slib.api.event.command.McBrigadierCommandsRegisterEvent
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.groups.BuildConstants
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.command.GroupsCommands
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
        val commands = GroupsCommands(this, playerCollector, ProxyTargetArgument { voiceServer })

        McBrigadierCommandsRegisterEvent.registerListener { registry ->
            registry.register(commands.build())
        }
    }
}
