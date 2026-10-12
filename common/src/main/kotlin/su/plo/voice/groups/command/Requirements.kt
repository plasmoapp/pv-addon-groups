package su.plo.voice.groups.command

import su.plo.slib.api.command.brigadier.McBrigadierSource
import su.plo.voice.groups.util.extend.hasAddonPermission
import su.plo.voice.groups.util.extend.hasFlagPermission
import java.util.function.Predicate

fun anyPermission(vararg permissions: String): Predicate<McBrigadierSource> =
    { source ->
        permissions.any { source.source.hasAddonPermission(it) }
    }

fun flagPermission(flag: String): Predicate<McBrigadierSource> =
    {
        it.source.hasFlagPermission(flag)
    }

fun tierPermissions(
    permission: String,
    othersTier: String = "all",
): Predicate<McBrigadierSource> = anyPermission("$permission.owner", "$permission.$othersTier", "$permission.*")
