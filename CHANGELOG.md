0.27.1 - Memory, resource and security hardening

- Store the forbidden password list as sorted 64-bit hashes (~8 MB instead of roughly 100 MB)
- Bound every internal cache and use weak keys for player-keyed caches
- Close premium API connections and add timeouts to every request, including session lookups
- Add timeouts and a size limit to the forbidden password download, and never keep a partial file
- Cancel repeating tasks on disable instead of leaving them running
- Remove the log filter on disable and stop stacking filters on reload
- Fix the log filter not matching `/temslogin user pass-change`, which leaked passwords into the proxy log
- Stop leaking a thread per library load call and restore the context class loader after sending mail
- Use locale-independent casing so config parsing cannot break under locales such as Turkish
- Avoid throwing on malformed session server responses

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
