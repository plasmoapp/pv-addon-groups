package su.plo.voice.groups

import su.plo.config.Config
import su.plo.config.ConfigField

@Config
class Config {
//    @ConfigField(path = "max_distance", comment = "Maximum priority distance")
//    @ConfigValidator(value = DistanceValidator::class, allowed = ["1-1024"])
//    val maxDistance = 128
//
//    @ConfigField(path = "default_distance", comment = "Default priority distance")
//    @ConfigValidator(value = DistanceValidator::class, allowed = ["1-1024"])
//    var defaultDistance = 48
//
    @ConfigField(path = "activation_weight")
    val activationWeight = 10
//
    @ConfigField(path = "sourceline_weight")
    val sourceLineWeight = 10

    @ConfigField(path = "default_group_name_format")
    val defaultGroupNameFormat = "%player%'s group"


//
//    @NoArgsConstructor
//    class DistanceValidator : Predicate<Any?> {
//        override fun test(o: Any?): Boolean {
//            if (o !is Long) return false
//            return o in 1..1024
//        }
//    }
}
