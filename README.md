# TemsLogin

TemsLogin is an authentication plugin for cracked and premium Minecraft servers with hybrid (mixed) logins,
built for Velocity proxies and for Paper, Spigot and Bukkit servers.

> **Note:** This plugin is originally forked from [kyngs/LibreLogin](https://github.com/kyngs/LibreLogin) by
> [kyngs](https://github.com/kyngs). This fork is only for [PhantomsNetwork](https://phantomsnetwork.org) internal
> use, so we do not provide support for others.

## Quick information

<img src="https://img.shields.io/badge/Java%20version-%2025+-blue?style=for-the-badge&logo=java&logoColor=white"
alt="Plugin requires Java 25 or newer"></img>

## Basic set of features

- AutoLogin for premium players
- TOTP 2FA (Authy, Google Authenticator...) [details](https://github.com/kyngs/LibreLogin/wiki/2FA)
- Session system
- Name validation (including case sensitivity check)
- Automatic data migration for premium players
- Migration of a player's data by using one command
- Geyser (Bedrock) support using [Floodgate](https://github.com/kyngs/LibreLogin/wiki/Floodgate)

## Considerations

- When using on a proxy, you need to secure your limbo
- Bukkit, Spigot and Paper servers require [PacketEvents](https://modrinth.com/plugin/packetevents) 2.14.0 or newer

## Platforms

- [x] Velocity (3.x & 4.x, including 4.3.0)
- [x] Paper (1.20.5+ / 26.x, tested up to 26.3)
- [x] Spigot / Bukkit (1.20.5+ / 26.x, tested up to 26.3)
- [ ] BungeeCord *(Dropped in 0.25+ due to Adventure 5 migration)*

## Commands and permissions

The main administrative command is `/temslogin` (`/librelogin` is kept as an alias). Every administrative
permission now lives under `temslogin.*`, for example `temslogin.user.info` or `temslogin.reload.configuration`.
`temslogin.admin` grants all of them.

Permissions used by older versions (`librelogin.*` and `librepremium.*`) are no longer checked, so update your
LuckPerms groups after upgrading, for example:

```bash
/lp group admin permission set temslogin.admin true
```

## Migrating from LibreLogin

- Configuration files are migrated automatically from `plugins/librelogin`, `plugins/LibrePremium` or
  `plugins/LibreLogin` to the new data folder.
- The database schema and table name are intentionally unchanged, so an existing database keeps working.
- If you publish this plugin yourself, remove the upstream bStats ids first. Telemetry is disabled in this fork.

## Building from Source

```bash
# Requires Java 25 or newer
./gradlew shadowJar
```

The compiled jar will be available in `Plugin/build/libs/TemsLogin.jar`.

## Special thanks

- [kyngs](https://github.com/kyngs) - author of [LibreLogin](https://github.com/kyngs/LibreLogin), which this plugin is based on
- [Raixo](https://github.com/RAIXOCZ) - for developing the original bungeecord port
- [FastLogin contributors](https://github.com/games647/FastLogin) - for their work, which was used as a base for the
  paper port
- [Fejby](https://github.com/Fejby) - for providing Floodgate test server and helping with testing

## License

TemsLogin is a derivative work of LibreLogin and is licensed under the Mozilla Public License 2.0.
The original copyright notices and license headers are kept in every source file.

- [License text](LICENSE)
- Upstream project: [kyngs/LibreLogin](https://github.com/kyngs/LibreLogin)

Modifications made by this fork are also available under the MPL-2.0. If you distribute a modified version,
you must keep the license and make the source of the modified files available, as required by the MPL-2.0.
