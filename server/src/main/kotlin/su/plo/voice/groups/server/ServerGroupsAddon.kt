package su.plo.voice.groups.server

import su.plo.slib.api.event.command.McBrigadierCommandsRegisterEvent
import su.plo.slib.api.server.channel.McServerChannelHandler
import su.plo.slib.api.server.channel.McServerChannelRegistry
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.api.server.player.VoiceServerPlayer
import su.plo.voice.groups.BuildConstants
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.channel.GroupsChannel
import su.plo.voice.groups.command.GroupsCommands
import su.plo.voice.groups.protocol.CHANNEL_KEY
import su.plo.voice.groups.server.command.ServerTargetArgument
import su.plo.voice.groups.util.PlayerCollector

@Addon(
    id = "pv-addon-groups",
    scope = AddonLoaderScope.SERVER,
    version = BuildConstants.VERSION,
    authors = ["KPidS", "Apehum"],
)
class ServerGroupsAddon : GroupsAddon() {
    private val playerCollector: PlayerCollector = ServerPlayerCollector()

    private val minecraftServer
        get() = (voiceServer as PlasmoVoiceServer).minecraftServer

    private var channel: GroupsChannel? = null
    private var channelHandler: McServerChannelHandler? = null

    init {
        McServerChannelRegistry.register(CHANNEL_KEY)

        val commands =
            GroupsCommands(
                this,
                playerCollector,
                ServerTargetArgument { voiceServer as PlasmoVoiceServer },
            )

        McBrigadierCommandsRegisterEvent.registerListener { registry ->
            registry.register(commands.build())
        }
    }

    override fun onAddonInitialize() {
        super.onAddonInitialize()

        val channel = GroupsChannel(this, playerCollector)
        val channelHandler = McServerChannelHandler(channel.server::receive)
        minecraftServer.channelManager.registerChannelHandler(CHANNEL_KEY, channelHandler)

        this.channel = channel
        this.channelHandler = channelHandler
    }

    override fun onAddonShutdown() {
        channelHandler?.let { minecraftServer.channelManager.unregisterChannelHandler(CHANNEL_KEY, it) }
        channel?.shutdown()
        channelHandler = null
        channel = null

        super.onAddonShutdown()
    }

    private inner class ServerPlayerCollector : PlayerCollector {
        override fun getVisibleOnlinePlayers(player: VoicePlayer?): Collection<VoicePlayer> {
            val voicePlayer =
                player as? VoiceServerPlayer
                    ?: return voiceServer.playerManager.players

            val voiceServer = voiceServer as PlasmoVoiceServer

            return voiceServer.playerManager.players
                .filter { voicePlayer.instance.canSee(it.instance) }
        }
    }
}
