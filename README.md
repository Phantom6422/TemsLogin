# TemsLogin

TemsLogin is an authentication plugin for cracked and premium Minecraft servers with hybrid (mixed) logins,
built for Velocity proxies and for Paper, Spigot and Bukkit servers.

> **Note:** This plugin is originally forked from [kyngs/LibreLogin](https://github.com/kyngs/LibreLogin) by
> [kyngs](https://github.com/kyngs). This fork is only for [PhantomsNetwork](https://phantomsnetwork.org) internal
> use, so we do not provide support for others.

> [!WARNING]
> **This fork has been heavily modified with the help of AI.** Expect bugs and unintended behaviour, including
> in authentication and data migration paths. Test every update on a staging server before using it in
> production, and keep backups of your database and data folder.

## Quick information

<img src="https://img.shields.io/badge/Java%20version-%2025+-blue?style=for-the-badge&logo=java&logoColor=white"
alt="Plugin requires Java 25 or newer"></img>

## Documentation

TemsLogin is a fork of LibreLogin, so the **[LibreLogin wiki](https://github.com/kyngs/LibreLogin/wiki)** is the
documentation for this plugin. Configuration keys, messages and behaviour are the same, with these differences:

- the command is `/temslogin` (`/librelogin` still works as an alias),
- permissions are under `temslogin.*` instead of `librelogin.*` / `librepremium.*`,
- the data folder is `plugins/temslogin` (`plugins/TemsLogin` on Paper/Spigot).

<a href="https://github.com/kyngs/LibreLogin/wiki">
<img src="https://img.shields.io/badge/Documentation-LibreLogin%20Wiki-555555?style=for-the-badge&logo=wikipedia"
alt="Documentation on the upstream LibreLogin wiki"></img>
</a>

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

Migration is automatic on the first start:

- If `plugins/temslogin` does not exist, the old folder (`plugins/librelogin`, `plugins/librepremium`,
  `plugins/LibreLogin` or `plugins/LibrePremium`) is renamed, which moves configuration, messages and the
  SQLite database in one go.
- If `plugins/temslogin` already exists, missing files are copied from the legacy folder instead:
  `config.conf`, `messages.conf`, `forbidden-passwords.txt` and any SQLite `*.db` files. Existing files are
  never overwritten, so your current TemsLogin configuration always wins.
- The configuration and messages revision numbers are upgraded by the built-in migrators on load, exactly as
  a normal plugin update would.
- The database schema and table name (`librepremium_data`) are intentionally unchanged, so an existing database
  keeps working. MySQL/PostgreSQL users just keep pointing at the same database in the imported `config.conf`.
- If you publish this plugin yourself, remove the upstream bStats ids first. Telemetry is disabled in this fork.

## Building from Source

```bash
# Requires Java 25 or newer
./gradlew shadowJar
```

The compiled jar will be available in `Plugin/build/libs/TemsLogin-<version>.jar`, for example
`TemsLogin-0.27.0-SNAPSHOT.jar`.

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
