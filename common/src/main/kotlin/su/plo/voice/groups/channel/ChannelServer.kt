package su.plo.voice.groups.channel

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import su.plo.slib.api.entity.player.McPlayer
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.voice.groups.BuildConstants
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.ChannelMessage
import su.plo.voice.groups.protocol.ChannelRequest
import su.plo.voice.groups.protocol.channelMsgPack
import su.plo.voice.groups.protocol.packetType
import java.util.concurrent.ConcurrentHashMap

fun interface ChannelRequestHandler<Request, Response> {
    suspend operator fun invoke(
        player: McPlayer,
        request: Request,
    ): Response
}

private val logger = McLoggerFactory.createLogger(ADDON_LOGGER, "ChannelServer")

class ChannelServer(
    val channelKey: String,
) {
    private val handlers: MutableMap<String, Channel<ReceivedMessage>> = ConcurrentHashMap()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    inline fun <reified Request : ChannelRequest<Response>, reified Response> registerRequestHandler(
        handler: ChannelRequestHandler<Request, Response?>,
    ) = registerHandler(packetType<Request>()) { message ->
        val request = channelMsgPack.decodeFromByteArray<Request>(message.payload)
        val response = handler(message.player, request) ?: return@registerHandler
        val requestId = message.requestId ?: return@registerHandler

        send(message.player, ChannelMessage(packetType<Response>(), requestId, channelMsgPack.encodeToByteArray(response)))
    }

    inline fun <reified Message> sendMessage(
        player: McPlayer,
        message: Message,
    ) = send(player, ChannelMessage(packetType<Message>(), payload = channelMsgPack.encodeToByteArray(message)))

    fun send(
        player: McPlayer,
        message: ChannelMessage,
    ) = player.sendPacket(channelKey, channelMsgPack.encodeToByteArray(message))

    fun registerHandler(
        type: String,
        handler: suspend (ReceivedMessage) -> Unit,
    ) {
        val channel = Channel<ReceivedMessage>(Channel.UNLIMITED)
        require(handlers.putIfAbsent(type, channel) == null) { "Handler for $type is already registered" }

        scope.launch {
            for (message in channel) {
                try {
                    handler(message)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    logger.error("Failed to handle \"$type\" from ${message.player.name}", e)
                }
            }
        }
    }

    fun receive(
        player: McPlayer,
        data: ByteArray,
    ) {
        val message =
            try {
                channelMsgPack.decodeFromByteArray<ChannelMessage>(data)
            } catch (e: Throwable) {
                logger.warn("Failed to decode a message from ${player.name}", e)
                return
            }

        val channel = handlers[message.type] ?: return
        channel.trySend(ReceivedMessage(player, message.requestId, message.payload))
    }

    fun shutdown() {
        scope.cancel()
        handlers.values.forEach { it.close() }
        handlers.clear()
    }

    class ReceivedMessage(
        val player: McPlayer,
        val requestId: String?,
        val payload: ByteArray,
    )
}
