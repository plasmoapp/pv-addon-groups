package su.plo.voice.groups.client.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import su.plo.config.provider.ConfigurationProvider
import su.plo.config.provider.toml.TomlConfiguration
import su.plo.voice.groups.client.config.GroupsClientConfig
import su.plo.voice.groups.logging.ADDON_LOGGER
import java.io.File
import java.io.IOException

object GroupsClient {
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @JvmStatic
    lateinit var service: GroupsService

    lateinit var config: GroupsClientConfig
        private set

    private lateinit var configFile: File

    private val toml =
        ConfigurationProvider.getProvider<ConfigurationProvider>(
            TomlConfiguration::class.java,
        )

    fun loadConfig(file: File) {
        configFile = file
        config =
            try {
                toml.load(GroupsClientConfig::class.java, file, true)
            } catch (e: IOException) {
                ADDON_LOGGER.warn("Failed to load client config", e)
                GroupsClientConfig()
            }
    }

    fun saveConfig() {
        scope.launch {
            try {
                toml.save(config, configFile)
            } catch (e: IOException) {
                ADDON_LOGGER.warn("Failed to save client config", e)
            }
        }
    }
}
