#!/usr/bin/env bash
# 启动「流浪世界」Paper 26.2 服务器（Linux / JDK 25，无 GUI）。
# 由 environment.json 的 terminals 调用，日志同时落在 logs/latest.log。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

JDK_DIR="$HOME/.jdks/temurin-25"
if [ -x "$JDK_DIR/bin/java" ]; then
  export JAVA_HOME="$JDK_DIR"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

PAPER_JAR="$(ls -1 paper-26.2-*.jar 2>/dev/null | sort -V | tail -1 || true)"
if [ -z "$PAPER_JAR" ]; then
  echo "未找到 paper-26.2-*.jar，请先运行 .cursor/install.sh" >&2
  exit 1
fi

echo "启动 $PAPER_JAR （JDK $(java -version 2>&1 | head -1)）"
exec java -Xms1G -Xmx4G -jar "$PAPER_JAR" --nogui
