package su.plo.voice.groups.proxy.command

import su.plo.slib.api.McLib
import su.plo.slib.api.proxy.command.McProxyCommand
import su.plo.voice.groups.GroupsAddon
import su.plo.voice.groups.command.CommandHandler

class ProxyCommandHandler(addon: GroupsAddon, minecraftServer: McLib) :
    CommandHandler(addon, minecraftServer),
    McProxyCommand
