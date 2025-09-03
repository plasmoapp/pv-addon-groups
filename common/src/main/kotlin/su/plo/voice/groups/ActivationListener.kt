package su.plo.voice.groups

import su.plo.voice.api.server.audio.capture.PlayerActivationInfo
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.source.ServerBroadcastSource
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.proto.packets.tcp.clientbound.SourceAudioEndPacket
import su.plo.voice.proto.packets.tcp.serverbound.PlayerAudioEndPacket
import su.plo.voice.proto.packets.udp.clientbound.SourceAudioPacket
import su.plo.voice.proto.packets.udp.serverbound.PlayerAudioPacket

class ActivationListener(
    private val groupManager: GroupsManager,
    activation: ServerActivation
) {

    init {
        activation.onPlayerActivation(this::onActivation)
        activation.onPlayerActivationEnd(this::onActivationEnd)
    }

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

    private fun sendAudioPacket(
        player: VoicePlayer,
        source: ServerBroadcastSource,
        packet: PlayerAudioPacket
    ): Boolean {
        val sourcePacket = SourceAudioPacket(
            packet.sequenceNumber, source.state.toByte(),
            packet.data,
            source.id, 0.toShort()
        )

        return source.sendAudioPacket(sourcePacket, PlayerActivationInfo(player, packet))
    }

    private fun sendAudioEndPacket(
        source: ServerBroadcastSource,
        packet: PlayerAudioEndPacket
    ): Boolean {
        val sourcePacket = SourceAudioEndPacket(source.id, packet.sequenceNumber)
        return source.sendPacket(sourcePacket)
    }
}
