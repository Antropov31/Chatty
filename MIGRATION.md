# Migrating from Chatty v2 to v3

Chatty v3 is a ground-up rewrite. Its configuration is split into several
files (`settings.yml`, `chats.yml`, `pm.yml`, `moderation.yml`, `vanilla.yml`,
`notifications.yml`, `replacements.yml`, `messages.yml`, `proxy.yml`) instead of
the single v2 `config.yml`.

## Automatic migration

When v3 starts and finds a legacy v2 `config.yml` in `plugins/Chatty/`, it:

1. Renames the whole folder to `Chatty_old_<timestamp>/` (your old config is
   never deleted).
2. Generates fresh v3 config files.
3. Migrates everything it can map unambiguously into those files.
4. Logs a summary of what still needs manual attention.

**Migrated automatically:**

| v2 | v3 |
|----|----|
| `chats.<id>` — format, display-name, symbol, range, cooldown, permission | `chats.yml` → `chats.<id>` |
| `general.priority` | `settings.yml` → `listener-priority` |
| `general.keep-old-recipients` | `settings.yml` → `respect-foreign-recipients` |
| `general.hide-vanished-recipients` | `settings.yml` → `hide-vanished-recipients` |
| `json.mentions.enable` / `.format` | `settings.yml` → `mentions.enable` / `mentions.target-format` |
| `moderation.caps` / `advertisement` / `swear` | `moderation.yml` |
| `pm.enable`, `pm.allow-console`, `pm.format.*` | `pm.yml` |
| `miscellaneous.vanilla.{join,quit,death}` | `vanilla.yml` |

Notes:
- Chat `range` values below `-2` are clamped to `-2`. v2 BungeeCord chats
  (`-3`) need Redis-based cross-server setup — see `proxy.yml`.
- Chat `cooldown: -1` (disabled) becomes `0`.
- Chats disabled in v2 (`enable: false`) are not migrated.
- PM format placeholders are translated: `{sender-*}` → `{from-*}`,
  `{recipient-*}` → `{to-*}`.
- Mention format placeholder `{player}` is translated to `{username}`.

## Needs manual attention

These are **not** migrated automatically and stay at v3 defaults:

- **Sounds** — v2 used Bukkit sound names (`CLICK`, `ORB_PICKUP`); v3 uses
  Adventure sound keys. Re-set the `sound` options where needed.
- **Interactive replacements** (`json.replacements`) — v3 uses a simpler
  MiniMessage format in `replacements.yml`.
- **Notifications** (`notifications.*`) — re-create them in `notifications.yml`.
- **Locale messages** — v2 `locale/*.yml` → v3 `messages.yml` and `lang/`.
- **Per-chat commands / aliases** and **per-chat moderation toggles** — no
  direct v3 equivalent.
- **Cross-server chat** — v3 uses Redis; configure `proxy.yml`.

## Tips

- v3 still understands legacy `&` color codes, plus MiniMessage and hex
  formats — your v2 formats keep working after migration.
- Review the migrated files and the startup log before going live.
- Keep the `Chatty_old_<timestamp>/` backup until you are happy with v3.
