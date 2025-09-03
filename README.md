![](https://i.imgur.com/2EpSlt0.png)

<div>
    <a href="https://modrinth.com/mod/plasmo-voice">Plasmo Voice</a>
    <span> | </span>
    <a href="https://modrinth.com/plugin/pv-addon-groups">Modrinth</a>
    <span> | </span>
    <a href="https://github.com/plasmoapp/pv-addon-groups/">GitHub</a>
    <span> | </span>
    <a href="https://discord.com/invite/uueEqzwCJJ">Discord</a>
     <span> | </span>
    <a href="https://www.patreon.com/plasmomc">Patreon</a>
</div>

# pv-addon-groups

Server-side [Plasmo Voice](https://modrinth.com/mod/plasmo-voice) add-on.

Create group voice chat channels. Keep talking with players far away.

Groups are managed via chat commands. GUI client side add-on coming soon™.

Groups can be password protected, or only allow player with certain permissions.

## Installation

You can install this add-on as either (Paper, Fabric, Forge) OR (Bungee, Velocity) mod/plugin

If you install it as a Bungee or Velocity plugin then groups will work for players an all servers.

Players can talk while beeing on different servers.

## Basic usage

### Create a group and invite players

1. Open Plasmo Voice menu `V` (by default), go to the `Activation` tab and configure the `Groups` activation.
2. Use a command to create a group `/groups create [...flags]`
3. Invite players to the group `/groups invite <player>`
3. Use the activation to talk in the group.

### Browsing groups

You can browse public groups using a command: `/groups` or `/groups browse`

## Flags

You can use flags when creating a group to change the settings.

For example: `/groups create name: The Boys password: qwerty1245 persistent: true permissions: group.theboys`

All flags are optional and the order doesn't matter.

`name:` and `password:` are quite obvious.

`persistent:` makes it so that the group is not automatically deleted when it's empty or after the server restart.

`permissions:` only allow players with a certain permission to join.

You can set multiple permissions: `permissions: group.admin, group.moderator`

### Flag permissions

| Flag          | Permission                         | Default |
|---------------|------------------------------------|---------|
| `name`        | `pv.addon.groups.flag.name`        | True    |
| `password`    | `pv.addon.groups.flag.password`    | True    |
| `persistent`  | `pv.addon.groups.flag.persistent`  | OP      |
| `permissions` | `pv.addon.groups.flag.permissions` | OP      |

## Commands

`/groups` or `/groups browse [page]` — Browse groups.

`/groups create [...flags]` — Create a group.

`/groups invite <player>` — Invite a player to the group.

`/groups join <group UUID> [password]` — Join the group. Not recommended to use manually. Used from browse or invite. 

`/groups info` — Show info about the current group.

`/groups leave` — Leave the current group.

`/groups set <flag> <value>` — Set a flag value for a current group.

`/groups unset <flag>` — Set flag to a default value.

`/groups delete` — Delete the current group.

`/groups transfer <player>` — Transfer the ownership of the group to a different player.

`/groups kick <player>` — Kick a player from the group.

`/groups ban <player>` — Ban a player from the group.

`/groups unban <player>` — Unban a player from the group.

## Permissions

| Permission                       | Description                                                         | Default |
|----------------------------------|---------------------------------------------------------------------|---------|
| `pv.activation.groups`           | Use groups activation                                               | True    |
| `pv.addon.groups.browse`         | Use `/groups browse`                                                | True    |
| `pv.addon.groups.browse.all`     | Groups are visible even if player doesn't have a permission to join | OP      |
| `pv.addon.groups.create`         | Use `/groups create`                                                | True    |
| `pv.addon.groups.invite.member`  | Use `/groups invite` if member                                      | True    |
| `pv.addon.groups.invite.owner`   | Use `/groups invite` if owner                                       | True    |
| `pv.addon.groups.join`           | Use `/groups join`                                                  | True    |
| `pv.addon.groups.join.all`       | Use `/groups join` and bypass password and permission check         | OP      |
| `pv.addon.groups.info.member`    | Use `/groups info` if member                                        | True    |
| `pv.addon.groups.info.owner`     | Use `/groups info` if owner                                         | True    |
| `pv.addon.groups.leave`          | Use `/groups leave`                                                 | True    |
| `pv.addon.groups.set.owner`      | Use `/groups set` if owner                                          | True    |
| `pv.addon.groups.set.all`        | Use `/groups set` in any group                                      | OP      |
| `pv.addon.groups.unset.owner`    | Use `/groups unset` if owner                                        | True    |
| `pv.addon.groups.unset.all`      | Use `/groups unset` in any group                                    | OP      |
| `pv.addon.groups.delete.owner`   | Use `/groups delete` if owner                                       | True    |
| `pv.addon.groups.delete.all`     | Use `/groups delete` in any group                                   | OP      |
| `pv.addon.groups.transfer.owner` | Use `/groups transfer` if owner                                     | True    |
| `pv.addon.groups.transfer.all`   | Use `/groups transfer` in any group                                 | OP      |
| `pv.addon.groups.kick.owner`     | Use `/groups kick` if owner                                         | True    |
| `pv.addon.groups.kick.all`       | Use `/groups kick` in any group                                     | OP      |
| `pv.addon.groups.ban.owner`      | Use `/groups ban/unban` if owner                                    | True    |
| `pv.addon.groups.ban.all`        | Use `/groups ban/unban` in any group                                | OP      |

