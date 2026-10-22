package su.plo.voice.groups.client.gui

import su.plo.voice.api.client.PlasmoVoiceClient
import su.plo.voice.client.config.VoiceClientConfig
import su.plo.voice.client.gui.settings.VoiceSettingsScreen
import su.plo.voice.client.gui.settings.tab.TabWidget

class GroupsTabWidget(
    parent: VoiceSettingsScreen,
    voiceClient: PlasmoVoiceClient,
    config: VoiceClientConfig,
) : TabWidget(parent, voiceClient, config)
