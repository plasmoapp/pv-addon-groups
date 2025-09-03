package su.plo.voice.groups

import com.google.common.collect.Sets
import kotlinx.serialization.json.Json
import su.plo.config.provider.ConfigurationProvider
import su.plo.config.provider.toml.TomlConfiguration
import su.plo.slib.api.McLib
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.injectPlasmoVoice
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.api.server.player.VoicePlayer
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.subcommand.*
import su.plo.voice.groups.group.Group
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

abstract class GroupsAddon : AddonInitializer {

    val voiceServer: PlasmoBaseVoiceServer by injectPlasmoVoice()

    lateinit var groupManager: GroupsManager

    fun getAddonFolder(minecraftServer: McLib): File =
        File(minecraftServer.configsFolder, "pv-addon-groups")

    private val activationName = "groups"

    override fun onAddonInitialize() {
        onConfigLoaded()
    }

    override fun onAddonShutdown() {
        groupManager.onVoiceServerShutdown(voiceServer)
    }

    abstract fun getVisibleOnlinePlayers(player: VoicePlayer? = null): Collection<VoicePlayer>

    protected fun onConfigLoaded() {

        val addonFolder = getAddonFolder(voiceServer.minecraftServer).also { it.mkdirs() }

        val config = try {

            voiceServer.languages.register(
                URI.create("https://github.com/plasmoapp/plasmo-voice-crowdin/archive/refs/heads/addons.zip").toURL(),
                "server/groups.toml",
                { resourcePath ->
                    getLanguageResource(resourcePath) ?: throw IOException("Can't load language resource")
                },
                File(addonFolder, "languages")
            )

            val configFile = File(addonFolder, "groups.toml")

            toml.load<Config>(Config::class.java, configFile, false)
                .also { toml.save(Config::class.java, it, configFile) }

        } catch (e: IOException) {
            throw IllegalStateException("Failed to load config", e)
        }

        val activation = voiceServer.activationManager.createBuilder(
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
            .setPermissionDefault(PermissionDefault.TRUE)
            .build()

        val sourceLine = voiceServer.sourceLineManager.createBuilder(
            this,
            activationName,
            "pv.activation.groups",
            "plasmovoice:textures/icons/speaker_group.png",
            config.sourceLineWeight
        ).apply {
            withPlayers(true)
        }.build()

        groupManager = GroupsManager(config, voiceServer, this, activation, sourceLine)

        File(addonFolder, "groups.json")
            .takeIf { it.isFile }
            ?.readText()
            ?.runCatching { Json.decodeFromString<GroupsManager.Data>(this) }
            ?.getOrNull()
            ?.also { fe -> fe.groups.forEach { group ->
                group.apply {
                    groupManager.groups[id] = Group(
                        sourceLine.playerSetManager!!.createBroadcastSet(),
                        id,
                        name,
                        password,
                        persistent,
                        Sets.newConcurrentHashSet(playersIds),
                        CopyOnWriteArrayList(bannedPlayers),
                        owner
                    )
                }
            } }
            ?.also { fe -> fe.groupByPlayer.mapNotNull {
                val group = groupManager.groups[it.value] ?: return@mapNotNull null
                groupManager.groupByPlayer[it.key] = group
            } }

        voiceServer.eventBus.register(this, ActivationListener(groupManager, activation))

        voiceServer.eventBus.register(this, groupManager)
    }

    // todo: waytoodank (refactor?)
    protected open fun createCommandHandler(minecraftServer: McLib) : CommandHandler =
        CommandHandler(this, minecraftServer)

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
            .addSubCommand(::KickCommand)
            .addSubCommand(::BanCommand)
            .addSubCommand(::UnbanCommand)
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
