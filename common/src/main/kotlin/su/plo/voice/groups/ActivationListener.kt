package su.plo.voice.groups

import su.plo.voice.api.event.EventPriority
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.api.server.audio.capture.SelfActivationInfo
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.source.ServerDirectSource
import su.plo.voice.api.server.event.audio.source.ServerSourcePacketEvent
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.proto.packets.tcp.clientbound.SourceAudioEndPacket
import su.plo.voice.proto.packets.tcp.clientbound.SourceInfoPacket
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket

class ActivationListener(
    voiceServer: PlasmoBaseVoiceServer,
    private val groupManager: GroupsManager,
    activation: ServerActivation
) {

    init {
        activation.onPlayerActivation(this::onActivation)
        activation.onPlayerActivationEnd(this::onActivationEnd)
    }

    private val selfActivationInfo = SelfActivationInfo(voiceServer.udpConnectionManager)

    private fun onActivation(player: VoicePlayer, packet: PlayerAudioPacket): ServerActivation.Result {
        groupManager.sourceByPlayer[player.instance.uuid]?.let {
            if (sendAudioPacket(player, it, packet)) {
                return ServerActivation.Result.HANDLED
            }
        }

        return ServerActivation.Result.IGNORED
    }

    fun onActivationEnd(player: VoicePlayer, packet: PlayerAudioEndPacket): ServerActivation.Result {
        groupManager.sourceByPlayer[player.instance.uuid]?.let {
            if (sendAudioEndPacket(it, packet)) {
                return ServerActivation.Result.HANDLED
            }
        }

        return ServerActivation.Result.IGNORED
    }

    @EventSubscribe(priority = EventPriority.HIGHEST)
    fun onSourceSendPacket(event: ServerSourcePacketEvent) {
        val source = event.source as? ServerDirectSource ?: return
        val sender = source.sender.orElse(null) ?: return

        if (!selfActivationInfo.lastPlayerActivationIds
                .containsKey(sender.instance.uuid)
        ) {
            return
        }

        if (event.packet is SourceInfoPacket) {
            selfActivationInfo.updateSelfSourceInfo(
                sender,
                source,
                (event.packet as SourceInfoPacket).sourceInfo
            )
        } else if (event.packet is SourceAudioEndPacket) {
            sender.sendPacket(event.packet)
        }
    }

    private fun sendAudioPacket(
        player: VoicePlayer,
        source: ServerDirectSource,
        packet: PlayerAudioPacket
    ): Boolean {
        val sourcePacket = SourceAudioPacket(
            packet.sequenceNumber, source.state.toByte(),
            packet.data,
            source.id, 0.toShort()
        )
        if (source.sendAudioPacket(sourcePacket, packet.activationId)) {
            selfActivationInfo.sendAudioInfo(player, source, packet.activationId, sourcePacket)
            return true
        }
        return false
    }

    private fun sendAudioEndPacket(
        source: ServerDirectSource,
        packet: PlayerAudioEndPacket
    ): Boolean {
        val sourcePacket = SourceAudioEndPacket(source.id, packet.sequenceNumber)
        return source.sendPacket(sourcePacket)
    }
}
