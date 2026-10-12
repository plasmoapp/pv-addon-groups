package su.plo.voice.groups.command

import su.plo.slib.api.permission.PermissionDefault
import su.plo.slib.api.permission.PermissionManager

object GroupsPermissions {
    private val permissions =
        listOf(
            "browse" to PermissionDefault.TRUE,
            "browse.all" to PermissionDefault.OP,
            "browse.*" to PermissionDefault.OP,
            "create" to PermissionDefault.TRUE,
            "join" to PermissionDefault.TRUE,
            "join.all" to PermissionDefault.OP,
            "join.*" to PermissionDefault.OP,
            "leave" to PermissionDefault.TRUE,
            "invite.owner" to PermissionDefault.TRUE,
            "invite.member" to PermissionDefault.TRUE,
            "invite.*" to PermissionDefault.OP,
            "info.owner" to PermissionDefault.TRUE,
            "info.member" to PermissionDefault.TRUE,
            "info.*" to PermissionDefault.OP,
            "set.owner" to PermissionDefault.TRUE,
            "set.all" to PermissionDefault.OP,
            "set.*" to PermissionDefault.OP,
            "unset.owner" to PermissionDefault.TRUE,
            "unset.all" to PermissionDefault.OP,
            "unset.*" to PermissionDefault.OP,
            "delete.owner" to PermissionDefault.TRUE,
            "delete.all" to PermissionDefault.OP,
            "delete.*" to PermissionDefault.OP,
            "transfer.owner" to PermissionDefault.TRUE,
            "transfer.all" to PermissionDefault.OP,
            "transfer.*" to PermissionDefault.OP,
            "kick.owner" to PermissionDefault.TRUE,
            "kick.all" to PermissionDefault.OP,
            "kick.*" to PermissionDefault.OP,
            "ban.owner" to PermissionDefault.TRUE,
            "ban.all" to PermissionDefault.OP,
            "ban.*" to PermissionDefault.OP,
            "flag.name" to PermissionDefault.TRUE,
            "flag.password" to PermissionDefault.TRUE,
            "flag.permissions" to PermissionDefault.OP,
            "flag.persistent" to PermissionDefault.OP,
        )

    val commandPermissions: List<String> =
        permissions
            .map { it.first }
            .filterNot { it.startsWith("flag.") }

    val flags: List<String> =
        permissions
            .map { it.first }
            .filter { it.startsWith("flag.") }
            .map { it.removePrefix("flag.") }

    fun register(permissionManager: PermissionManager) {
        permissions.forEach { (name, default) ->
            permissionManager.register("pv.addon.groups.$name", default)
        }
    }
}
