#!/usr/bin/env bash
# Cloud Agent 安装脚本：为「流浪世界」Paper 26.2 服务器仓库准备 Linux 开发环境。
# 目标：安装 JDK 25 工具链、拉取 Paper 服务端、生成世界并落地 libraries/、
#       编译自研插件（当前依赖齐备的即 MCWWS_WebHost），并为网页服务安装 Node 依赖。
# 该脚本必须可反复执行（幂等），且不启动常驻进程。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

JDK_DIR="$HOME/.jdks/temurin-25"
PAPER_VERSION="26.2"
PAPER_API_BASE="https://fill.papermc.io/v3/projects/paper"

log() { printf '\n\033[1;36m[install]\033[0m %s\n' "$*"; }

# --- 1. JDK 25（仓库 build 脚本与 Paper 26.2 均以 JDK 25 为准） ---
if [ ! -x "$JDK_DIR/bin/javac" ]; then
  log "下载并安装 Temurin JDK 25 到 $JDK_DIR"
  mkdir -p "$HOME/.jdks"
  tmp="$(mktemp -d)"
  curl -fsSL --retry 4 --retry-delay 4 -o "$tmp/jdk25.tar.gz" \
    "https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse"
  mkdir -p "$tmp/extract"
  tar xzf "$tmp/jdk25.tar.gz" -C "$tmp/extract"
  rm -rf "$JDK_DIR"
  mv "$tmp/extract"/jdk-* "$JDK_DIR"
  rm -rf "$tmp"
else
  log "JDK 25 已存在，跳过下载"
fi
export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"
java -version

# --- 2. Paper 服务端 jar（*.jar 被 .gitignore 忽略，仓库不含二进制） ---
shopt -s nullglob
existing_paper=( paper-"$PAPER_VERSION"-*.jar )
shopt -u nullglob
if [ ${#existing_paper[@]} -eq 0 ]; then
  log "获取 Paper $PAPER_VERSION 最新构建下载地址"
  meta="$(curl -fsSL --retry 4 --retry-delay 4 "$PAPER_API_BASE/versions/$PAPER_VERSION/builds/latest")"
  jar_name="$(printf '%s' "$meta" | python3 -c 'import sys,json;print(json.load(sys.stdin)["downloads"]["server:default"]["name"])')"
  jar_url="$(printf '%s' "$meta" | python3 -c 'import sys,json;print(json.load(sys.stdin)["downloads"]["server:default"]["url"])')"
  log "下载 $jar_name"
  curl -fsSL --retry 4 --retry-delay 4 -o "$jar_name" "$jar_url"
else
  log "已存在 Paper jar：${existing_paper[*]}，跳过下载"
fi
PAPER_JAR="$(ls -1 paper-"$PAPER_VERSION"-*.jar | sort -V | tail -1)"
log "使用 Paper jar：$PAPER_JAR"

# Paper 要求同意 EULA 才能启动
grep -q '^eula=true' eula.txt 2>/dev/null || echo "eula=true" > eula.txt

# --- 3. 首启引导：生成世界并让 Paper 下载 libraries/（插件编译的 classpath 依赖） ---
if [ ! -d libraries/io/papermc/paper/paper-api ]; then
  log "首次引导 Paper（生成世界 + 落地 libraries/），完成后自动 stop"
  fifo="$(mktemp -u)"; mkfifo "$fifo"
  java -Xms1G -Xmx4G -jar "$PAPER_JAR" --nogui < "$fifo" &
  server_pid=$!
  exec 3>"$fifo"
  for _ in $(seq 1 180); do
    if grep -q 'Done (' logs/latest.log 2>/dev/null; then break; fi
    if ! kill -0 "$server_pid" 2>/dev/null; then break; fi
    sleep 2
  done
  echo stop >&3 || true
  wait "$server_pid" 2>/dev/null || true
  exec 3>&- || true
  rm -f "$fifo"
  log "引导完成"
else
  log "libraries/ 已就绪，跳过引导"
fi

# --- 4. 编译自研 Java 插件 ---
# build.ps1 仅在 Windows 可用；此处用等价的 javac 逻辑在 Linux 上编译。
# classpath 取 libraries/ 与 plugins/ 下全部 jar。
# MCWWS_WebHost 无第三方插件依赖，直接打包进 plugins/ 供服务器加载；
# 其余插件硬依赖第三方付费/手动 jar（Vault、Slimefun、UltimateShop 等，未随仓库分发），
# 这里只做「编译检查」到各自 build/classes，缺依赖则跳过，绝不把无法加载的 jar 丢进 plugins/。
log "编译自研插件"
CP="$(find libraries plugins -maxdepth 6 -name '*.jar' 2>/dev/null | tr '\n' ':')"

# 编译并打包到 plugins/（仅用于依赖齐备、可被服务器加载的插件）
package_plugin() {
  local dir="$1" jar_name="$2" src="$1/src/main/java" res="$1/src/main/resources" out="$1/build/classes"
  [ -d "$src" ] || { echo "  跳过 $dir（无源码）"; return 0; }
  rm -rf "$out"; mkdir -p "$out"
  local sources; sources="$(find "$src" -name '*.java')"
  if javac -encoding UTF-8 -cp "$CP" -d "$out" $sources 2>"$out/javac.log"; then
    [ -d "$res" ] && cp -r "$res/." "$out/"
    ( cd "$out" && jar cf "$ROOT/plugins/$jar_name" . )
    printf '  \033[1;32m✓ 打包 %s\033[0m\n' "$jar_name"
  else
    printf '  \033[1;31m✗ %s 编译失败（见 %s）\033[0m\n' "$dir" "$out/javac.log"
    return 1
  fi
}

# 仅编译检查（输出到 build/classes，不产出 plugins/ 下 jar）
compile_check() {
  local dir="$1" src="$1/src/main/java" out="$1/build/classes"
  [ -d "$src" ] || return 0
  rm -rf "$out"; mkdir -p "$out"
  local sources; sources="$(find "$src" -name '*.java')"
  if javac -encoding UTF-8 -cp "$CP" -d "$out" $sources 2>"$out/javac.log"; then
    printf '  \033[1;32m✓ 编译通过 %s\033[0m\n' "$dir"
  else
    printf '  \033[1;33m↷ 跳过 %s（第三方依赖 jar 缺失，见 %s）\033[0m\n' "$dir" "$out/javac.log"
  fi
}

package_plugin tools/mcwws-web-host MCWWS_WebHost.jar
for d in \
  tools/mcwws-economy-ledger \
  tools/mcwws-worldedit-survival \
  tools/mcwws-residence-quiet \
  tools/mcwws-idea-achievements \
  tools/mcwws-ultimateadvancements \
  tools/mcwws-ultimateshop-fix \
  tools/mcwws-ultimateshop-stash \
  tools/mcwws-axiom-survival \
  MCWWS_SFurnaceFix; do
  compile_check "$d"
done

# --- 5. 网页服务 Node 依赖（node_modules 被 .gitignore 忽略） ---
WEB_DIR="plugins/Skript/scripts/web"
if [ -f "$WEB_DIR/package.json" ] && [ ! -d "$WEB_DIR/node_modules" ]; then
  if command -v npm >/dev/null 2>&1; then
    log "安装网页服务 Node 依赖（npm ci）"
    ( cd "$WEB_DIR" && npm ci --no-audit --no-fund ) || log "npm ci 失败，可稍后手动安装"
  else
    log "未找到 npm，跳过网页依赖安装"
  fi
else
  log "网页依赖已就绪或无 package.json，跳过"
fi

log "安装完成。使用 .cursor/run-server.sh 启动服务器。"
