package su.plo.voice.groups.proxy.command

import su.plo.lib.api.proxy.command.MinecraftProxyCommand
import su.plo.voice.api.server.PlasmoCommonVoiceServer
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.command.CommandHandler

class ProxyCommandHandler(voiceServer: PlasmoCommonVoiceServer, addon: GroupsAddon) :
    CommandHandler(voiceServer, addon),
    MinecraftProxyCommand
