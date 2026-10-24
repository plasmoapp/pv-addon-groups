//? if neoforge {
/*package su.plo.voice.groups.client.entrypoint

import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.groups.client.ClientGroupsAddon
import su.plo.voice.groups.server.ServerGroupsAddon

@Mod("pv_addon_groups")
class NeoForgeGroupsLoader(
    modBus: IEventBus,
) {
    init {
        PlasmoVoiceServer.getAddonsLoader().load(ServerGroupsAddon())

        modBus.addListener(FMLClientSetupEvent::class.java) {
            PlasmoVoiceClient.getAddonsLoader().load(ClientGroupsAddon())
        }
    }
}
*///?}
