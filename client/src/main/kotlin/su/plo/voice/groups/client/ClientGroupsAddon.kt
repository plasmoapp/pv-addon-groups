package su.plo.voice.groups.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.minecraft.resources.ResourceLocation
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.client.event.socket.UdpClientClosedEvent
import su.plo.voice.api.client.event.socket.UdpClientConnectedEvent
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.groups.BuildConstants
import su.plo.voice.groups.client.channel.GroupsChannelClient
import su.plo.voice.groups.client.channel.createChannelClient
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.CHANNEL_KEY

@Addon(
    id = BuildConstants.ADDON_ID,
    version = BuildConstants.VERSION,
    authors = ["KPidS", "Apehum"],
    scope = AddonLoaderScope.CLIENT,
)
class ClientGroupsAddon : AddonInitializer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private lateinit var channel: GroupsChannelClient

    override fun onAddonInitialize() {
        channel = GroupsChannelClient(createChannelClient(ResourceLocation.parse(CHANNEL_KEY)))
    }

    @EventSubscribe
    fun onVoiceConnect(event: UdpClientConnectedEvent) {
        scope.launch {
            try {
                val response = channel.handshake() ?: return@launch
                ADDON_LOGGER.info("Groups handshake done, server protocol {}", response.protocolVersion)
            } catch (e: Throwable) {
                ADDON_LOGGER.debug("Groups handshake failed", e)
            }
        }
    }

    @EventSubscribe
    fun onVoiceDisconnect(event: UdpClientClosedEvent) {
        channel.reset()
    }
}
