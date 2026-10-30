package su.plo.voice.groups.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.minecraft.resources.ResourceLocation
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.AddonLoaderScope
import su.plo.voice.api.addon.InjectPlasmoVoice
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.client.event.socket.UdpClientClosedEvent
import su.plo.voice.api.client.event.socket.UdpClientConnectedEvent
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.groups.BuildConstants
import su.plo.voice.groups.client.channel.createChannelClient
import su.plo.voice.groups.client.service.GroupsClient
import su.plo.voice.groups.client.service.NetworkGroupsService
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.CHANNEL_KEY
import java.io.File

@Addon(
    id = BuildConstants.ADDON_ID,
    version = BuildConstants.VERSION,
    authors = ["KPidS", "Apehum"],
    scope = AddonLoaderScope.CLIENT,
)
class ClientGroupsAddon : AddonInitializer {
    @InjectPlasmoVoice
    private lateinit var voiceClient: PlasmoVoiceClient

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private lateinit var network: NetworkGroupsService

    override fun onAddonInitialize() {
        network = NetworkGroupsService(createChannelClient(ResourceLocation.parse(CHANNEL_KEY)), voiceClient)
        GroupsClient.service = network
        GroupsClient.loadConfig(
            File(voiceClient.configFolder.parentFile, "${BuildConstants.ADDON_ID}/client.toml"),
        )
    }

    @EventSubscribe
    fun onVoiceConnect(event: UdpClientConnectedEvent) {
        scope.launch {
            try {
                ADDON_LOGGER.info("Groups handshake status: {}", network.handshake())
            } catch (e: Throwable) {
                ADDON_LOGGER.debug("Groups handshake failed", e)
            }
        }
    }

    @EventSubscribe
    fun onVoiceDisconnect(event: UdpClientClosedEvent) {
        network.reset()
    }
}
