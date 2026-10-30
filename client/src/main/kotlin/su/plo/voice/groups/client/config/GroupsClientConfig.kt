package su.plo.voice.groups.client.config

import su.plo.config.Config
import su.plo.config.ConfigField

@Config
class GroupsClientConfig {
    @ConfigField
    var activationSetupDone = false
}
