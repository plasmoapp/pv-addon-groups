- Requires Plasmo Voice 2.2.0+.
- Fabric and NeoForge mods now ship with optional client-side GUI addon, available as a separate tab in the Plasmo Voice menu ([screenshots](https://github.com/plasmoapp/pv-addon-groups#gui)). [#28](https://github.com/plasmoapp/pv-addon-groups/issues/28)
  - Browse, create, join and leave groups.
  - Invite players and accept or decline incoming invites.
  - Manage members: transfer ownership, kick and ban.
  - Group settings: name, password, permissions, persistence, banned players and group deletion.
  - One-time setup card that helps configure groups activation.
  - `2.0.0+` on the server is required for the GUI to work.
- Added groups API published to maven central, see [README API](https://github.com/plasmoapp/pv-addon-groups#api) section on how to set up dependency.
- Commands now use Brigadier:
  - `/groups invite` now accepts target selectors on server platforms (Paper/Fabric/NeoForge), e.g. `/groups invite @a[distance=..10]`.
  - `/groups join` and `/groups leave` now run at executor's context, so `/execute as <player> run groups leave` will make target player to leave from the current group. [#21](https://github.com/plasmoapp/pv-addon-groups/issues/21)
- `ban`, `kick` and `transfer` commands now work on offline members.
- Vanish support on proxies. [#27](https://github.com/plasmoapp/pv-addon-groups/issues/27)
  - Currently, [PremiumVanish](https://www.spigotmc.org/resources/premiumvanish-stay-hidden-bungee-velocity-support.14404/) and [SayanVanish](https://github.com/Syrent/SayanVanish) are supported.
  If you want other vanish plugins to be supported, you can either implement [McPlayerVisibilityCheckEvent](https://slib.plasmoverse.com/api-common/su.plo.slib.api.event.player/-mc-player-visibility-check-event/index.html) and fire [McPlayerVisibilityChangedEvent](https://slib.plasmoverse.com/api-common/su.plo.slib.api.event.player/-mc-player-visibility-changed-event/index.html) in your own addon,
  or open an issue in https://github.com/plasmoapp/mc-slib with plugin you want to be supported.
