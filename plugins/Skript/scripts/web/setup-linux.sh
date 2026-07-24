#!/usr/bin/env sh
# 首次在 Linux 部署时于 web 目录执行：安装 Node 依赖
set -e
cd "$(dirname "$0")"
if ! command -v node >/dev/null 2>&1; then
  echo "请先安装 Node.js 18+（例如 apt install nodejs npm）" >&2
  exit 1
fi
npm install --omit=dev
echo "依赖已安装。将 MCWWS_WebHost.jar 放入 plugins 后启动 Paper 即可。"
