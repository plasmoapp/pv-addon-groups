package su.plo.voice.groups

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import su.plo.config.provider.ConfigurationProvider
import su.plo.config.provider.toml.TomlConfiguration
import su.plo.voice.api.addon.AddonScope
import su.plo.voice.api.addon.annotation.Addon
import su.plo.voice.api.event.EventSubscribe
import su.plo.voice.api.server.PlasmoVoiceServer
import su.plo.voice.api.server.event.command.CommandsRegisterEvent
import su.plo.voice.api.server.event.config.VoiceServerConfigLoadedEvent
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.subcommand.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

@Addon(id = "groups", scope = AddonScope.SERVER, version = "1.0.0", authors = ["KPidS"])
class GroupsAddon {

    var groupManager: GroupsManager? = null

    fun getAddonFolder(server: PlasmoVoiceServer): File = File(server.configFolder, "addons/groups")

    private val activationName = "groups"
    @EventSubscribe
    fun onConfigLoaded(event: VoiceServerConfigLoadedEvent) {

        val server = event.server

        val addonFolder = getAddonFolder(server).also { it.mkdirs() }

        val config = try {

            server.languages.register(
                { resourcePath: String -> getLanguageResource(resourcePath)
                    ?: throw Exception("Can't load language resource")
                },
                File(addonFolder, "languages")
            )

            val configFile = File(addonFolder, "config.toml")

            toml.load<Config>(Config::class.java, configFile, false)
                .also { toml.save(Config::class.java, it, configFile) }

        } catch (e: IOException) {
            throw IllegalStateException("Failed to load config", e)
        }

        val activation = server.activationManager.createBuilder(
            this,
            activationName,
            "pv.activation.radio",
            "plasmovoice:textures/icons/microphone_group.png",
            "pv.activation.radio",
            config.activationWeight
        )
            .setProximity(false)
            .setTransitive(true)
            .setStereoSupported(false)
            .build()

        val sourceLine = server.sourceLineManager.register(
            this,
            activationName,
            "pv.activation.radio",
            "plasmovoice:textures/icons/speaker_group.png",
            config.sourceLineWeight
        )

        groupManager = GroupsManager(config, server, this, activation, sourceLine)

        File(addonFolder, "groups.json")
            .takeIf { it.isFile }
            ?.readText()
            ?.runCatching { Json.decodeFromString<GroupsManager.Data>(this) }
            ?.getOrNull()
            ?.also { fe -> fe.groups.forEach { group ->
                group.ownerUUID
                    ?.let { server.minecraftServer.getGameProfile(it).orElse(null) }
                    ?.let { group.data.owner = it }
                groupManager!!.groups[group.data.id] = group.data
            } }
            ?.also { fe -> fe.groupByPlayer.mapNotNull {
                val group = groupManager!!.groups[it.value] ?: return@mapNotNull null
                groupManager!!.groupByPlayer[it.key] = group
            } }

        server.eventBus.register(this, ActivationListener(
            server, this, activation, sourceLine
        ))

        server.eventBus.register(this, groupManager!!)
    }

    @EventSubscribe
    fun onCommandsRegister(event: CommandsRegisterEvent) {
        event.commandManager.register(
            "groups",
            CommandHandler(event.voiceServer, this)
                .addSubCommand(::CreateCommand)
                .addSubCommand(::DeleteCommand)
                .addSubCommand(::BrowseCommand)
                .addSubCommand(::LeaveCommand)
                .addSubCommand(::JoinCommand)
                .addSubCommand(::TransferCommand)
                .addSubCommand(::InfoCommand)
                .addSubCommand(::InviteCommand)
        )
    }

    @Throws(IOException::class)
    private fun getLanguageResource(resourcePath: String): InputStream? {
        return javaClass.classLoader.getResourceAsStream(String.format("groups/%s", resourcePath))
    }

    companion object {
        private val toml = ConfigurationProvider.getProvider<ConfigurationProvider>(
            TomlConfiguration::class.java
        )
    }
}