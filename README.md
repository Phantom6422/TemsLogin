# TemsLogin

Authentication plugin for cracked and premium Minecraft servers with hybrid logins, built for Velocity 4.x.

TemsLogin is a fork of [LibreLogin](https://github.com/kyngs/LibreLogin) by [kyngs](https://github.com/kyngs),
maintained for [PhantomsNetwork](https://phantomsnetwork.org). This is an internal fork; no external support is provided.

## Download

[![Latest release](https://img.shields.io/github/v/release/Phantom6422/TemsLogin?style=for-the-badge)](https://github.com/Phantom6422/TemsLogin/releases/latest)

Download the latest jar from the [Releases page](https://github.com/Phantom6422/TemsLogin/releases/latest).
Only the latest release is published; older versions remain available as git tags.

## Requirements

- Velocity 4.x (4.0.0 or newer)
- Java 25 or newer
- Backend servers in offline mode with Velocity forwarding enabled
- A secured limbo server

Paper, Spigot, Bukkit and BungeeCord are not supported.

## Features

- Premium autologin and hybrid cracked/premium accounts
- Password registration and login
- Session system
- TOTP 2FA
- Floodgate (Geyser) support
- Automatic LibreLogin data migration

## Documentation

Configuration and messages are documented on the
[LibreLogin wiki](https://github.com/kyngs/LibreLogin/wiki). Differences in this fork:

- command: `/temslogin` (`/librelogin` is an alias)
- permissions: `temslogin.*` (`temslogin.admin` grants all)
- data folder: `plugins/temslogin`

## Building

```bash
# Requires Java 25 or newer
./gradlew shadowJar
```

The jar is written to `Plugin/build/libs/TemsLogin-<version>.jar`.

## License

Licensed under the [Mozilla Public License 2.0](LICENSE). TemsLogin is a derivative work of LibreLogin;
see [NOTICE.md](NOTICE.md) for attribution.
