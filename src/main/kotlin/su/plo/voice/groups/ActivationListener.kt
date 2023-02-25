package su.plo.voice.groups

import su.plo.lib.api.server.permission.PermissionDefault
import su.plo.voice.api.event.EventPriority
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.audio.capture.BaseProximityServerActivation
import su.plo.voice.api.server.audio.capture.ServerActivation
import su.plo.voice.api.server.audio.line.ServerSourceLine
import su.plo.voice.api.server.event.audio.capture.ServerActivationRegisterEvent


class ActivationListener(
    private val voiceServer: PlasmoVoiceServer,
    private val addon: GroupsAddon,
    private val activation: ServerActivation,
    private val sourceLine: ServerSourceLine
) : BaseProximityServerActivation(voiceServer, "radio", PermissionDefault.TRUE) {

//    private val permissions = voiceServer.minecraftServer.permissionsManager

//    @EventSubscribe(priority = EventPriority.HIGHEST)
//    override fun onActivationRegister(event: ServerActivationRegisterEvent) {
//        val activation: ServerActivation = event.activation
//        if (activation.name != activationName) return
//        activation.permissions.forEach { permissions.register(it, defaultPermission) }
//    }
//
//    @EventSubscribe(priority = EventPriority.HIGHEST)
//    override fun onActivationUnregister(event: ServerActivationUnregisterEvent) {
//        val activation: ServerActivation = event.activation
//        if (activation.name != activationName) return
//        activation.permissions.forEach(permissions::unregister)
//    }
//
//    @EventSubscribe(priority = EventPriority.HIGHEST)
//    fun onPlayerSpeak(event: PlayerSpeakEvent) {
//
//        if (activation == null) return
//
//        val player = event.player as VoiceServerPlayer
//        val packet = event.packet
//
//        if (!activation!!.checkPermissions(player)) return
//
//        addon.channelManager?.sourceByPlayer?.get(player.info.playerId)
//            ?.run { sendAudioPacket(player, this, packet) }
//    }
////
//    @EventSubscribe(priority = EventPriority.HIGHEST)
//    fun onPlayerSpeakEnd(event: PlayerSpeakEndEvent) {
//        if (activation == null) return
//        val player = event.player as VoiceServerPlayer
//        val packet = event.packet
//
//        if (!activation!!.checkPermissions(player)) return
//
//        addon.channelManager?.sourceByPlayer?.get(player.info.playerId)
//            ?.run { sendAudioEndPacket(this, packet) }
//    }
//
//    private fun sendAudioPacket(
//        player: VoicePlayer,
//        source: ServerDirectSource,
//        packet: PlayerAudioPacket
//    ): Boolean {
//        val sourcePacket = SourceAudioPacket(
//            packet.sequenceNumber, source.state.toByte(),
//            packet.data,
//            source.id, 0.toShort()
//        )
//        if (source.sendAudioPacket(sourcePacket, packet.activationId)) {
//            selfActivationInfo.sendAudioInfo(player, source, packet.activationId, sourcePacket)
//            return true
//        }
//        return false
//    }
//
//    private fun sendAudioEndPacket(
//        source: ServerDirectSource,
//        packet: PlayerAudioEndPacket
//    ): Boolean {
//        val sourcePacket = SourceAudioEndPacket(source.id, packet.sequenceNumber)
//        return source.sendPacket(sourcePacket)
//    }

}