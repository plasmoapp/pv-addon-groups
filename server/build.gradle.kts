val plasmoVoiceVersion: String by rootProject

dependencies {
    compileOnly(project(":common"))

    compileOnly("su.plo.voice.api:server:$plasmoVoiceVersion")
}
