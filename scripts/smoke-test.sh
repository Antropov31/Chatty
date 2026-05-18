#!/usr/bin/env bash
#
# End-to-end smoke test for Chatty.
#
# Boots a real Paper server with the built plugin and verifies two scenarios:
#   A. fresh install        -> the plugin enables and generates v3 configs
#   B. legacy v2 migration  -> a v2 config.yml is migrated into v3 files
#
# Requirements: bash, curl, python3, and a JDK 21 (point JAVA_HOME at it).
# Build the plugin first (./gradlew build); the workflow does this for CI.
#
# Usage:  JAVA_HOME=/path/to/jdk-21 bash scripts/smoke-test.sh
#
set -euo pipefail

MC_VERSION="${MC_VERSION:-1.21.4}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# Kept under build/ (git-ignored) so logs survive for inspection / CI artifacts.
WORK="$ROOT/build/smoke-test"
SERVER="$WORK/server"
SERVER_PID=""
HOLDER_PID=""

JAVA_BIN="java"
[ -n "${JAVA_HOME:-}" ] && JAVA_BIN="$JAVA_HOME/bin/java"

cleanup() {
    [ -n "$SERVER_PID" ] && kill -9 "$SERVER_PID" 2>/dev/null || true
    [ -n "$HOLDER_PID" ] && kill "$HOLDER_PID" 2>/dev/null || true
}
trap cleanup EXIT

rm -rf "$WORK"
mkdir -p "$WORK"

step() { printf '\n\033[1m=== %s ===\033[0m\n' "$1"; }
fail() { printf '\n\033[31m✗ SMOKE TEST FAILED: %s\033[0m\n' "$*" >&2; exit 1; }

# --- locate the plugin jar -------------------------------------------------

JAR="$(ls -t "$ROOT"/build/libs/Chatty-*.jar 2>/dev/null | head -1 || true)"
[ -n "$JAR" ] || fail "plugin jar not found in build/libs — run ./gradlew build first"
echo "Plugin jar: $JAR"
"$JAVA_BIN" -version 2>&1 | head -1

# --- download Paper --------------------------------------------------------

step "Downloading Paper $MC_VERSION"
BUILD="$(curl -fsSL "https://api.papermc.io/v2/projects/paper/versions/$MC_VERSION" \
    | python3 -c 'import sys, json; print(json.load(sys.stdin)["builds"][-1])')"
PAPER_JAR="$WORK/paper.jar"
curl -fsSL -o "$PAPER_JAR" \
    "https://api.papermc.io/v2/projects/paper/versions/$MC_VERSION/builds/$BUILD/downloads/paper-$MC_VERSION-$BUILD.jar"
echo "Paper $MC_VERSION build $BUILD"

# --- server scaffolding ----------------------------------------------------

mkdir -p "$SERVER/plugins"
echo "eula=true" > "$SERVER/eula.txt"
cat > "$SERVER/server.properties" <<'EOF'
online-mode=false
level-type=flat
spawn-protection=0
max-players=1
EOF
cp "$JAR" "$SERVER/plugins/Chatty.jar"

# Boots the server, waits for full startup, stops it cleanly.
# $1 = path to write the server log to.
run_server() {
    local logfile="$1"
    local pipe="$WORK/stdin.pipe"
    rm -f "$pipe"; mkfifo "$pipe"
    sleep 900 > "$pipe" &          # holds the stdin pipe open
    HOLDER_PID=$!
    disown "$HOLDER_PID" 2>/dev/null || true
    ( cd "$SERVER" && exec "$JAVA_BIN" -Xmx1G -jar "$PAPER_JAR" nogui ) \
        < "$pipe" > "$logfile" 2>&1 &
    SERVER_PID=$!

    local ready=0 i
    for ((i = 0; i < 240; i++)); do
        if grep -q 'Done (' "$logfile" 2>/dev/null; then ready=1; break; fi
        kill -0 "$SERVER_PID" 2>/dev/null || break
        sleep 1
    done

    echo "stop" > "$pipe" 2>/dev/null || true
    for ((i = 0; i < 60; i++)); do
        kill -0 "$SERVER_PID" 2>/dev/null || break
        sleep 1
    done
    kill -9 "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
    kill "$HOLDER_PID" 2>/dev/null || true
    SERVER_PID=""; HOLDER_PID=""

    [ "$ready" -eq 1 ] || { tail -40 "$logfile" >&2; fail "server did not finish startup"; }
}

# Verifies the plugin enabled without errors. $1 = server log.
assert_enabled() {
    local logfile="$1"
    grep -q "Enabling Chatty" "$logfile" || { tail -40 "$logfile" >&2; fail "Chatty was not enabled"; }
    if grep -qE "Error occurred while enabling Chatty|Could not load .plugins.Chatty" "$logfile"; then
        grep -nE "Chatty|Exception|SEVERE" "$logfile" | tail -40 >&2
        fail "Chatty failed to enable"
    fi
}

# --- scenario A: fresh install ---------------------------------------------

step "Scenario A — fresh install"
rm -rf "$SERVER/plugins/Chatty" "$SERVER"/plugins/Chatty_old_*
FRESH_LOG="$WORK/fresh.log"
run_server "$FRESH_LOG"
assert_enabled "$FRESH_LOG"
[ -f "$SERVER/plugins/Chatty/settings.yml" ] || fail "settings.yml was not generated"
[ -f "$SERVER/plugins/Chatty/chats.yml" ]    || fail "chats.yml was not generated"
echo "✓ plugin enables and generates config on a fresh install"

# --- scenario B: legacy v2 migration ---------------------------------------

step "Scenario B — legacy v2 migration"
rm -rf "$SERVER/plugins/Chatty" "$SERVER"/plugins/Chatty_old_*
mkdir -p "$SERVER/plugins/Chatty"
cp "$ROOT/scripts/fixtures/v2-config.yml" "$SERVER/plugins/Chatty/config.yml"
MIGRATE_LOG="$WORK/migrate.log"
run_server "$MIGRATE_LOG"
assert_enabled "$MIGRATE_LOG"

grep -q "Migrating legacy Chatty v2 configuration" "$MIGRATE_LOG" || fail "migration did not start"
grep -q "Legacy configuration migrated"           "$MIGRATE_LOG" || fail "migration did not finish"
! grep -q "Failed to migrate legacy configuration" "$MIGRATE_LOG" || fail "migration threw an exception"
! grep -q "could not be migrated automatically"    "$MIGRATE_LOG" || fail "a config file failed to migrate"

ls -d "$SERVER"/plugins/Chatty_old_* >/dev/null 2>&1 || fail "v2 backup folder was not created"

CHATS="$SERVER/plugins/Chatty/chats.yml"
SETTINGS="$SERVER/plugins/Chatty/settings.yml"
MODERATION="$SERVER/plugins/Chatty/moderation.yml"
PM="$SERVER/plugins/Chatty/pm.yml"

# These also prove okaeri reloaded the migrated YAML without rejecting it:
# the values survive the migrator write -> okaeri load -> okaeri re-save.
grep -q "MIGRATED_MARKER" "$CHATS"     || fail "chat format was not migrated into chats.yml"
grep -q "HIGHEST"         "$SETTINGS"  || fail "listener-priority was not migrated"
grep -q "IP_PATTERN_X"    "$MODERATION" || fail "advertisement ip pattern was not migrated"
grep -q "from-name"       "$PM"        || fail "PM format placeholders were not migrated"
if grep -q "disabled_chat" "$CHATS"; then fail "a disabled v2 chat was migrated"; fi

echo "✓ legacy v2 config migrated and reloaded successfully"
echo
grep -E "\[Chatty\].*(Migrat|migrat|review|-)" "$MIGRATE_LOG" || true

printf '\n\033[32m✓ SMOKE TEST PASSED\033[0m\n'
