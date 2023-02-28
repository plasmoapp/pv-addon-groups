package su.plo.voice.groups

import com.google.inject.Inject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import su.plo.config.provider.ConfigurationProvider
import su.plo.config.provider.toml.TomlConfiguration
import su.plo.lib.api.server.command.MinecraftCommand
import su.plo.lib.api.server.command.MinecraftCommandManager
import su.plo.voice.api.PlasmoVoice
import su.plo.voice.api.server.PlasmoCommonVoiceServer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.subcommand.*
import su.plo.voice.groups.group.Group
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*

open class GroupsAddon {

    var groupManager: GroupsManager? = null

    fun getAddonFolder(server: PlasmoVoice): File =
        File(server.configFolder, "addons/groups")

    private val activationName = "groups"

    protected fun onConfigLoaded(server: PlasmoCommonVoiceServer) {

        val addonFolder = getAddonFolder(server).also { it.mkdirs() }

        val config = try {

            server.languages.register(
                { resourcePath: String -> getLanguageResource(resourcePath)
                    ?: throw Exception("Can't load language resource")
                },
                File(addonFolder, "languages")
            )

            val configFile = File(addonFolder, "groups.toml")

            toml.load<Config>(Config::class.java, configFile, false)
                .also { toml.save(Config::class.java, it, configFile) }

        } catch (e: IOException) {
            throw IllegalStateException("Failed to load config", e)
        }

        val activation = server.activationManager.createBuilder(
            this,
            activationName,
            "pv.activation.groups",
            "plasmovoice:textures/icons/microphone_group.png",
            "pv.activation.groups",
            config.activationWeight
        )
            .setProximity(false)
            .setTransitive(true)
            .setStereoSupported(false)
            .build()

        val sourceLine = server.sourceLineManager.registerPlayers(
            this,
            activationName,
            "pv.activation.groups",
            "plasmovoice:textures/icons/speaker_group.png",
            config.sourceLineWeight
        )

        val groupManager = GroupsManager(config, server.sourceManager, this, activation, sourceLine).also {
            this.groupManager = it
        }

        File(addonFolder, "groups.json")
            .takeIf { it.isFile }
            ?.readText()
            ?.runCatching { Json.decodeFromString<GroupsManager.Data>(this) }
            ?.getOrNull()
            ?.also { fe -> fe.groups.forEach { group ->
                group.data.apply {
                    groupManager.groups[id] = Group(
                        sourceLine.createBroadcastSet(),
                        id,
                        name,
                        password,
                        persistent
                    ).apply {
                        owner = group.owner
//                            ?.let { server.minecraftServer.getGameProfile(it).orElse(null) } // how
                    }
                }
            } }
            ?.also { fe -> fe.groupByPlayer.mapNotNull {
                val group = groupManager.groups[it.value] ?: return@mapNotNull null
                groupManager.groupByPlayer[it.key] = group
            } }

        server.eventBus.register(this, ActivationListener(
            server, groupManager, activation
        ))

        server.eventBus.register(this, groupManager)
    }

    // todo: waytoodank (refactor?)
    protected open fun createCommandHandler(voiceServer: PlasmoCommonVoiceServer) : CommandHandler =
        CommandHandler(voiceServer, this)

    protected fun addSubcommandsToCommandHandler(commandHandler: CommandHandler) {
        commandHandler
            .addSubCommand(::BrowseCommand)
            .addSubCommand(::CreateCommand)
            .addSubCommand(::JoinCommand)
            .addSubCommand(::LeaveCommand)
            .addSubCommand(::InviteCommand)
            .addSubCommand(::InfoCommand)
            .addSubCommand(::SetCommand)
            .addSubCommand(::UnsetCommand)
            .addSubCommand(::DeleteCommand)
            .addSubCommand(::TransferCommand)
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