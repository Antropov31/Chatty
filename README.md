# Chatty (fork by [@Antropov31](https://github.com/Antropov31))

> ### 🍴 This is a fork
> This repository is a fork of the original [**Brikster/Chatty**](https://github.com/Brikster/Chatty),
> maintained by [**@Antropov31**](https://github.com/Antropov31).
>
> **What this fork fixes**
> On modern cores (Purpur / Paper for Minecraft 1.20.5+, tested on **Purpur 26.2**),
> upstream Chatty crashed on player join and on scheduled notifications with an
> `ExceptionInInitializerError`. The Adventure bridge (`NativeAudienceAdapter`)
> resolved every method handle in a single static initializer and let any failure
> escape, so one missing/removed Adventure method
> (`Audience#sendMessage(Identity, Component)`, which was removed from current
> Adventure) poisoned the whole class. That cascaded into endless
> `NoClassDefFoundError`s and kicked players.
>
> **The fix:** each method handle is now resolved independently and defensively.
> The identity-aware `sendMessage` overload is optional and transparently falls
> back to the plain `sendMessage(Component)` when it is absent, so the class never
> throws from `<clinit>`. Result: no more crashes, no more kicks, works on current
> and future server versions (including 26.2). The build was also updated to a
> Java 21-compatible Gradle, and a GitHub Actions workflow builds a ready-to-use
> plugin jar automatically (see the **Actions → Build Fixed Chatty Jar** tab).
>
> **Maintainer / fork developer:** [@Antropov31](https://github.com/Antropov31)
> · Original author & full credit: [Brikster](https://github.com/Brikster).

---

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/Brikster/Chatty)](https://github.com/Brikster/Chatty/releases/latest)
[![GitHub All Releases](https://img.shields.io/github/downloads/Brikster/Chatty/total)](https://github.com/Brikster/Chatty/releases)
[![GitHub code size in bytes](https://img.shields.io/github/languages/code-size/Brikster/Chatty)](https://github.com/Brikster/Chatty/archive/master.zip)
[![JitPack](https://jitpack.io/v/Brikster/Chatty.svg)](https://jitpack.io/#Brikster/Chatty)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/815bf25f21da4c81b9e26bd1159df072)](https://www.codacy.com/gh/Brikster/Chatty/dashboard?utm_source=github.com&utm_medium=referral&utm_content=Brikster/Chatty&utm_campaign=Badge_Grade)

> **Chatty v3** is a ground-up rewrite built on Kyori's Adventure library. It is
> approaching its first stable release (`3.0.0`); this branch holds its code.
>
> - **Stable builds** — the [Releases](https://github.com/Brikster/Chatty/releases) page (once `3.0.0` is tagged).
> - **Development builds** — the latest artifact from the "Actions" tab (see the "Artifacts" section).
>
> Chatty v2.* is deprecated and no longer maintained.
> Upgrading from v2? See [MIGRATION.md](MIGRATION.md) — v3 migrates your old config automatically.

Chatty is the modern chat management system for Bukkit-compatible servers. It's based on-top of Kyori's Adventure library, 
that makes it so powerful and stable.

**Key features**:
- Chat channels ("local" and "global" by default)
- Private messaging
- Moderation (CAPS, advertisements, swears)
- Notifications (chat, action bar and title)
- "Vanilla" messages configuring (join/quit/death)
- MiniMessage both legacy (&) styling format

## Building

Chatty uses Gradle to handle dependencies & building. You need JDK 11 or higher to compile Chatty (the CI builds with JDK 21).

### Compiling from source

```shell script
git clone https://github.com/Antropov31/Chatty.git
cd Chatty/
./gradlew build
```

Output jar will be placed into `/build/libs` directory.

### Automated builds

Every push to `v3` triggers the **Build Fixed Chatty Jar** workflow, which builds
the plugin and uploads a ready `Chatty-*.jar` as a downloadable artifact. Tagging
a commit `v*` also attaches the jar to a GitHub Release.

## Testing

Run the unit tests:

```shell script
./gradlew test
```

Run the end-to-end smoke test — it boots real Minecraft servers with the built
plugin and verifies that it enables cleanly on a fresh install, processes live
in-game chat, correctly migrates a legacy v2 configuration, still runs on a
legacy server (1.8.8), and coexists with DiscordSRV:

```shell script
./gradlew build
JAVA_HOME=/path/to/jdk-21 bash scripts/smoke-test.sh
```

The legacy-server scenario needs a Java 11 runtime; it is downloaded
automatically, or point `LEGACY_JAVA_HOME` at an existing one.

Both run automatically on every push via GitHub Actions.
