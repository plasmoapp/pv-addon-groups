//? if fabric {
package su.plo.voice.groups.client.entrypoint

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.ModInitializer
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.groups.client.ClientGroupsAddon
import su.plo.voice.groups.server.ServerGroupsAddon

class FabricGroupsLoader :
    ClientModInitializer,
    ModInitializer {
    override fun onInitialize() {
        PlasmoVoiceServer.getAddonsLoader().load(ServerGroupsAddon())
    }

    override fun onInitializeClient() {
        PlasmoVoiceClient.getAddonsLoader().load(ClientGroupsAddon())
    }
}
//?}
