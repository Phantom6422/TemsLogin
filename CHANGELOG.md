0.29.4 - Premium autologin toggle

- Add the `premium-autologin` option. Set it to `false` when premium players cannot reach Mojang's
  authentication servers; they will log in with a password instead of being forced into online mode.

0.29.3 - Reload concurrency fix

- Make limbo/lobby routing reload thread-safe so a configuration reload cannot race with logins

0.29.2 - Security and reliability fixes

- Fail closed when TemsLogin cannot load or enable instead of leaving the proxy running without authentication
- Mark players unauthenticated before any PostLogin database access, and disconnect them if session setup fails
- Disconnect instead of falling back to the default lobby when initial-server selection fails
- Refuse login when an account has 2FA enabled but the proxy has no TOTP provider (for example when Protocolize is missing)
- Fix a null dereference when a lobby connection request completes exceptionally
- Do not register a NanoLimbo server whose start failed
- Rebuild limbo/lobby routing after a configuration reload
- Disable database migration after it succeeds so it is not repeated on every restart
- Fix a null dereference in /setemail for password-less accounts and the wrong last-seen value in /temslogin user alts
- Reject unparseable proxy version strings instead of throwing a NumberFormatException

0.29.1 - Velocity 4.x support

- Support the whole Velocity 4.x line (4.0.0+) instead of requiring 4.3.0
- Build against the Velocity 4.0.0 API so the plugin stays compatible with older 4.x proxies
- Reject Velocity 3.x and 5.x at startup
- Add unit tests for the supported-version check

0.29.0 - Velocity-only

- Drop Paper, Spigot and Bukkit support, including the PacketEvents protocol layer, the Bukkit/Paper
  platform classes, `plugin.yml`/`paper-plugin.yml`, the Yggdrasil key resource and the FastLogin-derived code
- Remove the Paper, Bukkit/Spigot, bStats and PacketEvents build dependencies and repositories
- Require Velocity 4.3.0 or newer and refuse to enable on older proxies with a clear error
- Keep the common authentication code and the Velocity platform layer unchanged

0.28.0 - Velocity 4.3.2-SNAPSHOT support

- Build against Velocity API 4.3.2-SNAPSHOT
- Read the NanoLimbo forwarding configuration reflectively, removing the dependency on the unpublished
  `velocity-proxy` artifact
- CI now starts Velocity 4.3.2-SNAPSHOT-48 with the plugin

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
