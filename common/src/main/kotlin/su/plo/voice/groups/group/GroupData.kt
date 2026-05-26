package su.plo.voice.groups.group

import com.google.common.collect.Sets
import kotlinx.serialization.Serializable
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.groups.util.serializer.McGameProfileSerializer
import su.plo.voice.groups.util.serializer.UUIDSerializer
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
open class GroupData(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    var name: String,
    var password: String? = null,
    var persistent: Boolean = false,
    val playersIds: MutableSet<
        @Serializable(with = UUIDSerializer::class)
        UUID,
    > = Sets.newConcurrentHashSet(),
    val bannedPlayers: MutableList<
        @Serializable(with = McGameProfileSerializer::class)
        McGameProfile,
    > = CopyOnWriteArrayList(),
    @Serializable(with = McGameProfileSerializer::class)
    var owner: McGameProfile? = null,
)
