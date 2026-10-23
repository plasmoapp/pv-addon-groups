@file:Suppress("UnstableApiUsage")

package su.plo.voice.groups.client.channel

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import net.minecraft.client.Minecraft
import net.minecraft.network.Connection
import net.minecraft.resources.ResourceLocation
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.slib.mod.channel.ModClientChannelHandler
import su.plo.slib.mod.channel.ModClientChannelManager
import su.plo.voice.groups.logging.ADDON_LOGGER
import su.plo.voice.groups.protocol.ChannelMessage
import su.plo.voice.groups.protocol.ChannelRequest
import su.plo.voice.groups.protocol.channelMsgPack
import su.plo.voice.groups.protocol.packetType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val DEFAULT_TIMEOUT = 5.seconds

private val clientChannelManager = ModClientChannelManager.INSTANCE

private val logger = McLoggerFactory.createLogger(ADDON_LOGGER, "ChannelClient")

fun createChannelClient(channelKey: ResourceLocation): ChannelClient =
    ChannelClient(channelKey)
        .also { clientChannelManager.registerHandler(channelKey, it) }

class ChannelClient(
    val channelKey: ResourceLocation,
) : ModClientChannelHandler {
    val pending: MutableMap<String, Channel<ByteArray>> = ConcurrentHashMap()
    val messageHandlers: MutableMap<String, (ByteArray) -> Unit> = ConcurrentHashMap()

    override fun receive(
        payload: ByteArray,
        connection: Connection,
    ) {
        val message =
            try {
                channelMsgPack.decodeFromByteArray<ChannelMessage>(payload)
            } catch (_: Throwable) {
                return
            }

        val requestId = message.requestId
        if (requestId != null) {
            pending[requestId]?.trySend(message.payload)
            return
        }

        val handler = messageHandlers[message.type] ?: return
        try {
            handler(message.payload)
        } catch (e: Throwable) {
            logger.debug("Failed to handle \"{}\"", message.type, e)
        }
    }

    inline fun <reified Message> registerMessageHandler(crossinline handler: (Message) -> Unit) {
        messageHandlers[packetType<Message>()] = { payload -> handler(channelMsgPack.decodeFromByteArray<Message>(payload)) }
    }

    suspend inline fun <reified Request : ChannelRequest<Response>, reified Response> request(
        request: Request,
        timeout: Duration = DEFAULT_TIMEOUT,
    ): Response {
        val requestId = UUID.randomUUID().toString()
        val channel = Channel<ByteArray>(capacity = 1)

        pending[requestId] = channel

        try {
            send(ChannelMessage(packetType<Request>(), requestId, channelMsgPack.encodeToByteArray(request)))

            return channelMsgPack.decodeFromByteArray<Response>(withTimeout(timeout) { channel.receive() })
        } finally {
            pending.remove(requestId)
            channel.close()
        }
    }

    inline fun <reified Message> sendMessage(message: Message) =
        send(ChannelMessage(packetType<Message>(), payload = channelMsgPack.encodeToByteArray(message)))

    fun send(message: ChannelMessage) {
        val connection = Minecraft.getInstance().connection?.connection ?: error("Not connected")

        clientChannelManager.sendPacket(channelKey, channelMsgPack.encodeToByteArray(message), connection)
    }

    fun reset() {
        pending.values.forEach { it.close() }
        pending.clear()
    }
}
