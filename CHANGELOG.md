0.27.0 - TemsLogin rebrand, Paper/Spigot/Bukkit support, security fixes

- Migrate configuration, messages, forbidden passwords and SQLite databases from LibreLogin automatically
- Name the built jar `TemsLogin-<version>.jar`

- Rebrand the plugin to TemsLogin (plugin id, data folder, `/temslogin` command, `temslogin.*` permissions)
- Keep `/librelogin` as a command alias and migrate existing config/message folders automatically
- Restore Paper, Spigot and Bukkit support, tested against Minecraft 26.3
- Require PacketEvents 2.14.0+ on server platforms
- Flush and fail closed on 2FA attempts to prevent code brute forcing
- Format TOTP codes without dropping leading zeros
- Fix Paper session verification accepting HTTP errors as a successful join
- Fix Paper encryption continuing after a failed nonce verification
- Make password/token comparisons constant time
- Fix `MessageDigest` providers sharing a non-thread-safe digest instance
- Enforce SMTP TLS (implicit TLS on 465, required STARTTLS elsewhere)
- Escape user controlled values in HTML emails
- Reject passwords longer than 72 bytes instead of silently truncating them with BCrypt
- Disable upstream bStats telemetry

0.26.1 - Velocity 4.3.0 support and update check fix

- Support Velocity 4.3.0
- Fix update checks failing on release tags prefixed with `v`
- Add explicit premium API timeout handling
- Harden Floodgate detection

0.25.0 - Minecraft 26.x support

- Support Minecraft 26.x
- Add RegEx name validation
- Allow to retrieve the plugin version from LibreLoginProvider
- Drop BungeeCord/Waterfall support
- Port to Adventure 5.x
- Unmount Player's vehicle on disconnect to prevent teleporting it to the limbo on rejoin
