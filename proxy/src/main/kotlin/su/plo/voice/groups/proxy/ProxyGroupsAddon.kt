package su.plo.voice.groups.proxy

import su.plo.slib.api.event.command.McBrigadierCommandsRegisterEvent
import su.plo.slib.api.proxy.channel.McProxyChannelHandler
import su.plo.slib.api.proxy.player.McProxyPlayer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.proxy.PlasmoVoiceProxy
import su.plo.voice.api.proxy.player.VoiceProxyPlayer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.BuildConstants
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.channel.GroupsChannel
import su.plo.voice.groups.channel.InviteSearch
import su.plo.voice.groups.command.GroupsCommands
import su.plo.voice.groups.protocol.CHANNEL_KEY
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

    private val minecraftProxy
        get() = (voiceServer as PlasmoVoiceProxy).minecraftServer

    private var channel: GroupsChannel? = null
    private var channelHandler: McProxyChannelHandler? = null

    init {
        val commands = GroupsCommands(this, playerCollector, ProxyTargetArgument { voiceServer })

        McBrigadierCommandsRegisterEvent.registerListener { registry ->
            registry.register(commands.build())
        }
    }

    override fun onAddonInitialize() {
        super.onAddonInitialize()

        val channel =
            GroupsChannel(
                this,
                playerCollector,
                InviteSearch(playerCollector, ::serverName),
            )
        val channelHandler =
            McProxyChannelHandler { source, _, data ->
                val player = source as? McProxyPlayer ?: return@McProxyChannelHandler false
                channel.server.receive(player, data)
                true
            }
        minecraftProxy.channelManager.registerChannelHandler(CHANNEL_KEY, channelHandler)

        this.channel = channel
        this.channelHandler = channelHandler
    }

    override fun onAddonShutdown() {
        channelHandler?.let { minecraftProxy.channelManager.unregisterChannelHandler(CHANNEL_KEY, it) }
        channel?.shutdown()
        channelHandler = null
        channel = null

        super.onAddonShutdown()
    }

    private fun serverName(player: VoicePlayer): String? =
        (player as? VoiceProxyPlayer)
            ?.instance
            ?.server
            ?.serverInfo
            ?.name
}
