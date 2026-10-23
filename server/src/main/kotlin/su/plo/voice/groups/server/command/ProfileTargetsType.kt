package su.plo.voice.groups.server.command

import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import su.plo.slib.api.command.brigadier.ArgumentResolver
import su.plo.slib.api.command.brigadier.CustomArgumentType
import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.slib.api.entity.player.McGameProfile
import su.plo.voice.groups.command.ProfileCandidates

class ProfileTargetsType<N>(
    override val nativeType: ArgumentType<N>,
    private val lookup: ProfileCandidates,
    private val selectorProfiles: (selector: N, source: McBrigadierSource) -> List<McGameProfile>,
) : CustomArgumentType<ProfileTargetsResolver, N> {
    override fun parse(reader: StringReader): ProfileTargetsResolver {
        if (reader.canRead() && reader.peek() == '@') {
            val selector = nativeType.parse(reader)

            return ProfileTargetsResolver { source ->
                val selectedIds =
                    selectorProfiles(selector, source)
                        .mapTo(HashSet()) { it.id }

                lookup(source.source).filter { it.id in selectedIds }
            }
        }

        val start = reader.cursor
        while (reader.canRead() && reader.peek() != ' ') reader.skip()
        val name = reader.string.substring(start, reader.cursor)

        return ProfileTargetsResolver { source ->
            listOfNotNull(
                lookup(source.source)
                    .firstOrNull { it.name.equals(name, ignoreCase = true) },
            )
        }
    }
}

fun interface ProfileTargetsResolver : ArgumentResolver<List<McGameProfile>>
