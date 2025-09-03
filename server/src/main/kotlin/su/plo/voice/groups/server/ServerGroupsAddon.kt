package su.plo.voice.groups.server

import su.plo.slib.api.server.event.command.McServerCommandsRegisterEvent
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.api.server.player.VoiceServerPlayer
import su.plo.voice.groups.BuildConstants
import su.plo.voice.groups.GroupsAddon

@Addon(id = "pv-addon-groups", scope = AddonLoaderScope.SERVER, version = BuildConstants.VERSION, authors = ["KPidS"])
class ServerGroupsAddon : GroupsAddon() {

    init {
        McServerCommandsRegisterEvent.registerListener { commandManager, minecraftServer ->
            commandManager.register(
                "groups",
                createCommandHandler(minecraftServer)
                    .also { addSubcommandsToCommandHandler(it) }
            )
        }
    }

    override fun getVisibleOnlinePlayers(player: VoicePlayer?): Collection<VoicePlayer> {
        val voicePlayer = player as? VoiceServerPlayer
            ?: return voiceServer.playerManager.players

        val voiceServer = voiceServer as PlasmoVoiceServer

        return voiceServer.playerManager.players
            .filter { voicePlayer.instance.canSee(it.instance) }
    }
}
