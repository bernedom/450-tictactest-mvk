#!/usr/bin/env bash
# Run TicTacTest locally in the browser:
#   - ttyd serves the released jar as a web console
#   - python serves the static frontend plus version.txt
#
# The jar is NOT built here. Put the jar downloaded from the GitHub release
# into $JAR_DIR (default: ./release), e.g.
#   gh release download v1.2.0 --pattern '*.jar' --dir release
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR_DIR="${JAR_DIR:-$ROOT/release}"
TTYD_PORT="${TTYD_PORT:-7681}"
WEB_PORT="${WEB_PORT:-8000}"
MAX_CLIENTS="${MAX_CLIENTS:-4}"   # one JVM per client, keep this small
WEB_ROOT="$ROOT/build/local-web"

# --- locate exactly one jar in the predefined path -------------------------
shopt -s nullglob
jars=("$JAR_DIR"/*.jar)
if (( ${#jars[@]} != 1 )); then
  echo "Expected exactly one jar in $JAR_DIR, found ${#jars[@]}." >&2
  echo "Download it from the release, e.g.: gh release download <tag> --pattern '*.jar' --dir $JAR_DIR" >&2
  exit 1
fi
JAR="${jars[0]}"

# --- determine version: manifest first, then filename -----------------------
version="$(unzip -p "$JAR" META-INF/MANIFEST.MF 2>/dev/null \
  | sed -n 's/^Implementation-Version: *//p' | tr -d '\r')"
if [[ -z "$version" ]]; then
  version="$(basename "$JAR" .jar | grep -Eo '[0-9]+\.[0-9]+\.[0-9]+.*$' || true)"
fi
version="${version:-unknown}"

# --- prepare web root: frontend + generated version.txt ---------------------
rm -rf "$WEB_ROOT"
mkdir -p "$WEB_ROOT"
cp -r "$ROOT/frontend/." "$WEB_ROOT/"
echo "$version" > "$WEB_ROOT/version.txt"

echo "Jar:      $JAR (version $version)"
echo "Console:  http://localhost:$TTYD_PORT/"
echo "Frontend: http://localhost:$WEB_PORT/"
echo "Version:  http://localhost:$WEB_PORT/version.txt"

trap 'kill 0' EXIT INT TERM

# Security: the command is the jar itself, never a shell (bash/sh/login).
# -W  writable, otherwise the browser cannot type
# -i lo  only reachable from this machine
# -m  limit concurrent clients (= concurrent JVMs)
# -a is deliberately NOT set: clients cannot pass arguments via URL
ttyd -W -i lo -p "$TTYD_PORT" -m "$MAX_CLIENTS" \
  -t titleFixed=TicTacTest -t disableLeaveAlert=true -t fontSize=18 \
  java -jar "$JAR" &

python3 -m http.server "$WEB_PORT" --bind 127.0.0.1 --directory "$WEB_ROOT" &

# stop everything as soon as one of the two servers exits
wait -n
