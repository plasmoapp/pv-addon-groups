package su.plo.voice.groups

import com.google.common.collect.Sets
import su.plo.config.provider.ConfigurationProvider
import su.plo.config.provider.toml.TomlConfiguration
import su.plo.slib.api.permission.PermissionDefault
import su.plo.voice.api.addon.AddonInitializer
import su.plo.voice.api.addon.InjectPlasmoVoice
import su.plo.voice.api.server.PlasmoBaseVoiceServer
import su.plo.voice.api.server.audio.line.BaseServerSourceLine
import su.plo.voice.groups.command.CommandHandler
import su.plo.voice.groups.command.subcommand.BanCommand
import su.plo.voice.groups.command.subcommand.BrowseCommand
import su.plo.voice.groups.command.subcommand.CreateCommand
import su.plo.voice.groups.command.subcommand.DeleteCommand
import su.plo.voice.groups.command.subcommand.InfoCommand
import su.plo.voice.groups.command.subcommand.InviteCommand
import su.plo.voice.groups.command.subcommand.JoinCommand
import su.plo.voice.groups.command.subcommand.KickCommand
import su.plo.voice.groups.command.subcommand.LeaveCommand
import su.plo.voice.groups.command.subcommand.SetCommand
import su.plo.voice.groups.command.subcommand.TransferCommand
import su.plo.voice.groups.command.subcommand.UnbanCommand
import su.plo.voice.groups.command.subcommand.UnsetCommand
import su.plo.voice.groups.group.GroupImpl
import su.plo.voice.groups.group.GroupsManager
import su.plo.voice.groups.listener.ActivationListener
import su.plo.voice.groups.listener.GroupsConnectionListener
import su.plo.voice.groups.listener.GroupsNotificationListener
import su.plo.voice.groups.repository.JsonGroupRepository
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList

open class GroupsAddon : AddonInitializer {
    @InjectPlasmoVoice
    lateinit var voiceServer: PlasmoBaseVoiceServer

    lateinit var groupManager: GroupsManager
    private lateinit var repository: JsonGroupRepository

    private var connectionListener: GroupsConnectionListener? = null

    override fun onAddonInitialize() {
        onConfigLoaded()
        GroupsNotificationListener.register()
    }

    override fun onAddonShutdown() {
        val persistent = groupManager.groupsById.values.filter { it.persistent }
        repository.save(
            JsonGroupRepository.Snapshot(
                groups = persistent,
                groupByPlayer = groupManager.groupByPlayer.map { it.key to it.value.id }.toMap(),
            ),
        )
    }

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

    private fun onConfigLoaded() {
        val addonFolder =
            voiceServer.minecraftServer.configsFolder
                .resolve("pv-addon-groups")
                .also { it.mkdirs() }

        val config =
            try {
                voiceServer.languages.register(
                    URI.create("https://github.com/plasmoapp/plasmo-voice-crowdin/archive/refs/heads/addons.zip").toURL(),
                    "server/groups.toml",
                    { resourcePath ->
                        getLanguageResource(resourcePath) ?: throw IOException("Can't load language resource")
                    },
                    addonFolder.resolve("languages"),
                )

                val configFile = addonFolder.resolve("groups.toml")

                toml
                    .load(GroupsConfig::class.java, configFile, false)
                    .also { toml.save(it, configFile) }
            } catch (e: IOException) {
                throw IllegalStateException("Failed to load config", e)
            }

        val activation =
            voiceServer.activationManager
                .createBuilder(
                    this,
                    ACTIVATION_NAME,
                    "pv.activation.groups",
                    "plasmovoice:textures/icons/microphone_group.png",
                    "pv.activation.groups",
                    config.activationWeight,
                ).setProximity(false)
                .setTransitive(true)
                .setStereoSupported(false)
                .setPermissionDefault(PermissionDefault.TRUE)
                .build()

        val sourceLine =
            voiceServer.sourceLineManager
                .createBuilder(
                    this,
                    ACTIVATION_NAME,
                    "pv.activation.groups",
                    "plasmovoice:textures/icons/speaker_group.png",
                    config.sourceLineWeight,
                ).apply {
                    withPlayers(true)
                }.build()

        repository = JsonGroupRepository(addonFolder.resolve("groups.json"))
        groupManager = GroupsManager(config, voiceServer, sourceLine)
        loadGroups(sourceLine)

        ActivationListener(groupManager, activation)
        registerConnectionListener()
    }

    private fun registerConnectionListener() {
        val eventBus = voiceServer.eventBus
        connectionListener?.let { eventBus.unregister(this, it) }
        connectionListener = GroupsConnectionListener(groupManager).also { eventBus.register(this, it) }
    }

    private fun loadGroups(sourceLine: BaseServerSourceLine) {
        val snapshot = repository.load() ?: return

        snapshot.groups.forEach { group ->
            groupManager.groupsById[group.id] =
                GroupImpl(
                    groupManager,
                    sourceLine.playerSetManager!!.createBroadcastSet(),
                    group.id,
                    group.name,
                    group.password,
                    group.persistent,
                    Sets.newConcurrentHashSet(group.playersIds),
                    CopyOnWriteArrayList(group.bannedPlayers),
                    group.owner,
                )
        }

        snapshot.groupByPlayer.forEach { (playerId, groupId) ->
            val group = groupManager.groupsById[groupId] ?: return@forEach
            groupManager.groupByPlayer[playerId] = group
        }
    }

    @Throws(IOException::class)
    private fun getLanguageResource(resourcePath: String): InputStream? =
        javaClass.classLoader.getResourceAsStream(String.format("groups/%s", resourcePath))

    companion object {
        private val toml =
            ConfigurationProvider.getProvider<ConfigurationProvider>(
                TomlConfiguration::class.java,
            )

        private const val ACTIVATION_NAME = "groups"
    }
}
