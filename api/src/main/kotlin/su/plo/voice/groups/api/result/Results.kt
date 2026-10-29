package su.plo.voice.groups.api.result

import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.api.Group

sealed class CreateResult {
    data class Success(
        val group: Group,
    ) : CreateResult()

    data class NameLengthInvalid(
        val min: Int,
        val max: Int,
    ) : CreateResult()
}

sealed class JoinResult {
    data class Success(
        val group: Group,
    ) : JoinResult()

    object AlreadyInGroup : JoinResult()

    object Banned : JoinResult()

    object PasswordRequired : JoinResult()

    object WrongPassword : JoinResult()

    object NoPermission : JoinResult()
}

sealed class LeaveResult {
    data class Success(
        val group: Group,
    ) : LeaveResult()

    object NotInGroup : LeaveResult()
}

sealed class KickResult {
    object Success : KickResult()

    object NotInGroup : KickResult()
}

sealed class BanResult {
    object Success : BanResult()

    /**
     * The target is the group owner, transfer ownership before banning them.
     */
    object Owner : BanResult()
}

sealed class UnbanResult {
    object Success : UnbanResult()

    object NotBanned : UnbanResult()
}

sealed class InviteResult {
    object Success : InviteResult()

    object AlreadyInGroup : InviteResult()

    object Banned : InviteResult()

    object Self : InviteResult()
}

sealed class CancelInviteResult {
    object Success : CancelInviteResult()

    object NotInvited : CancelInviteResult()
}

sealed class DeclineInviteResult {
    object Success : DeclineInviteResult()

    object NotFound : DeclineInviteResult()
}

sealed class DeleteResult {
    object Success : DeleteResult()
}

sealed class TransferResult {
    object Success : TransferResult()

    object NotInGroup : TransferResult()

    object AlreadyOwner : TransferResult()
}

sealed class SetNameResult {
    data class Success(
        val oldName: String,
    ) : SetNameResult()

    object IdenticalValue : SetNameResult()

    data class NameLengthInvalid(
        val min: Int,
        val max: Int,
    ) : SetNameResult()
}

sealed class SetPasswordResult {
    object Success : SetPasswordResult()

    object IdenticalValue : SetPasswordResult()
}

sealed class SetPermissionsResult {
    /** @property removedPlayers members kicked for not passing the new filter. */
    data class Success(
        val removedPlayers: Collection<VoicePlayer>,
    ) : SetPermissionsResult()

    object IdenticalValue : SetPermissionsResult()
}

sealed class SetPersistentResult {
    object Success : SetPersistentResult()

    object IdenticalValue : SetPersistentResult()
}
