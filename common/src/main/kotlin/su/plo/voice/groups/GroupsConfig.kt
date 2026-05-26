package su.plo.voice.groups

import su.plo.config.Config
import su.plo.config.ConfigField

@Config
class GroupsConfig {
    @ConfigField(path = "activation_weight")
    val activationWeight = 10

    @ConfigField(path = "sourceline_weight")
    val sourceLineWeight = 10

    @ConfigField(path = "default_group_name_format")
    val defaultGroupNameFormat = "%player%'s group"

    @ConfigField(path = "minimum_name_length")
    val minimumNameLength = 3

    @ConfigField(path = "maximum_name_length")
    val maximumNameLength = 32
}
