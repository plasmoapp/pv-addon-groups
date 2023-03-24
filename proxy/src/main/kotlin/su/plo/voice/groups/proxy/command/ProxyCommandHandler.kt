package su.plo.voice.groups.proxy.command

import su.plo.lib.api.proxy.command.MinecraftProxyCommand
import su.plo.lib.api.server.MinecraftCommonServerLib
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.command.CommandHandler

class ProxyCommandHandler(addon: GroupsAddon, minecraftServer: MinecraftCommonServerLib) :
    CommandHandler(addon, minecraftServer),
    MinecraftProxyCommand
