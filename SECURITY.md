# Security Policy

TemsLogin is maintained for **internal use only**. Public support is not provided. Security reports are still welcome and will be handled privately.

## Reporting a vulnerability

Please do **not** open a public GitHub issue, pull request, or Discord channel message for a security problem.

Report it privately through either channel:

- **Email:** [mail@phantomsnetwork.org](mailto:mail@phantomsnetwork.org)
- **Discord:** [https://phantomsnetwork.org/discord](https://phantomsnetwork.org/discord) — contact a PhantomsNetwork administrator privately; do not post details in a public channel.

Please include:

- the affected TemsLogin version,
- the Velocity version,
- the smallest reproduction you can share,
- the impact you observed (authentication bypass, session/token issue, data exposure, crash, etc.),
- and any suggested fix.

Do not include real player credentials, password hashes, TOTP secrets, session tokens, database credentials, IP addresses or production addresses. Use a test proxy and dummy accounts where possible.

## What to expect

- Acknowledgment of the report as soon as practical.
- Private triage and a fix in a future release.
- Coordinated disclosure only after a fix is available. Please do not publish details before then.
- No bug bounty is offered.

## Scope

TemsLogin is a Velocity proxy authentication plugin. Relevant boundaries include:

- authentication and registration flows,
- premium autologin and premium/cracked account switching,
- session handling and session tokens,
- TOTP/2FA enrollment and verification,
- password hashing and password policy,
- Velocity forwarding and backend-server trust,
- Floodgate/Geyser integration,
- database queries, migrations and file handling.

The following are out of scope:

- vulnerabilities in Velocity, the JDK, MySQL, PostgreSQL, SQLite, Floodgate, Geyser, LimboAuth/NanoLimbo or other third-party software (report those upstream),
- issues that require an already-compromised proxy host, database or backend server,
- insecure backend/server configuration (for example, offline-mode backends without forwarding secrets),
- brute-force or credential-stuffing reports that do not demonstrate a bypass of the plugin's controls,
- support requests, configuration help or feature requests.
