# TemsLogin

TemsLogin is an authentication plugin for **Velocity 4.x** proxies. It provides hybrid cracked/premium logins,
premium autologin, password registration, session logins, TOTP 2FA, Floodgate (Geyser) support and automatic
LibreLogin data migration.

> A rebranded fork of [LibreLogin](https://github.com/kyngs/LibreLogin) by
> [kyngs](https://github.com/kyngs) and contributors, maintained by
> [Phantom6422](https://github.com/Phantom6422) for [PhantomsNetwork](https://phantomsnetwork.org) internal use.

## Install

Download the [latest release](https://github.com/Phantom6422/TemsLogin/releases/latest), place
`TemsLogin-<version>.jar` in `plugins/`, start the proxy, and edit `plugins/temslogin/config.conf` and
`messages.conf`. Configuration keys and messages are documented on the
[LibreLogin wiki](https://github.com/kyngs/LibreLogin/wiki).

Only the latest release is kept on GitHub. Backend servers must run in offline mode with Velocity forwarding
enabled, and the limbo server must be secured. If premium players cannot reach Mojang's authentication
servers, set `premium-autologin: false` in `config.conf` so they can use password login instead.

## Platforms

- Velocity 4.x (4.0.0+)

Paper, Spigot, Bukkit and BungeeCord are not supported.

## Commands

The main command is `/temslogin` with alias `/librelogin`.

| Command | Description |
| --- | --- |
| `/temslogin` | Show the command list |
| `/temslogin about` | Show plugin information |
| `/temslogin reload configuration` | Reload the configuration |
| `/temslogin reload messages` | Reload the messages |
| `/temslogin user info <player>` | Show account information |
| `/temslogin user migrate <player> <new-name>` | Migrate a player's data |
| `/temslogin user unregister <player>` | Remove a player's password |
| `/temslogin user delete <player>` | Delete a player's data |
| `/temslogin user premium <player>` | Enable premium autologin |
| `/temslogin user cracked <player>` | Disable premium autologin |
| `/temslogin user register <player> <password>` | Register a player |
| `/temslogin user login <player>` | Authorize a player |
| `/temslogin user 2faoff <player>` | Disable a player's 2FA |
| `/temslogin user emailoff <player>` | Remove a player's email |
| `/temslogin user setemail <player> <email>` | Set a player's email |
| `/temslogin user pass-change <player> <password>` | Change a player's password |
| `/temslogin user alts <player>` | List accounts sharing an IP |
| `/temslogin email test <email>` | Send a test email |
| `/temslogin dump` | Write a diagnostic dump |

Player commands (`/login`, `/register`, `/premium`, `/cracked`, `/2fa`, `/changepassword`, email commands)
are documented on the wiki. Permissions use `temslogin.*` (for example `temslogin.user.info`,
`temslogin.reload.configuration`); `temslogin.admin` grants all of them. Legacy `librelogin.*` and
`librepremium.*` nodes are not checked.

## Build

```bash
./gradlew shadowJar
```

Requires JDK 25+. The shaded jar is written to `velocity/build/libs/TemsLogin-<version>.jar`.

## Credits

- [kyngs](https://github.com/kyngs) — author of [LibreLogin](https://github.com/kyngs/LibreLogin)
- [Raixo](https://github.com/RAIXOCZ) — original BungeeCord port
- [Fejby](https://github.com/Fejby) — Floodgate testing

## License

MPL-2.0. See [LICENSE](LICENSE).
