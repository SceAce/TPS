#!/usr/bin/env bash
# 脚本说明：Android 局域网构建脚本，负责按传入 IP 生成调试 APK。

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LAN_IP="${1:-10.120.190.96}"

API_BASE_URL="http://${LAN_IP}:8080/"
WS_URL="ws://${LAN_IP}:8080/ws/websocket"
# LAN 包只使用调用时传入的地址，避免真机测试时先访问模拟器或 USB 地址。
API_FALLBACKS="$API_BASE_URL"
WS_FALLBACKS="$WS_URL"

echo "Building Android debug APK for LAN backend"
echo "API: $API_BASE_URL"
echo "WebSocket: $WS_URL"
echo "Fallback APIs: $API_FALLBACKS"
echo "Fallback WebSockets: $WS_FALLBACKS"
echo

cd "$ROOT_DIR"
./gradlew :app:clean :app:assembleDebug \
  -PTPS_API_BASE_URL="$API_BASE_URL" \
  -PTPS_API_FALLBACK_BASE_URLS="$API_FALLBACKS" \
  -PTPS_WS_URL="$WS_URL" \
  -PTPS_WS_FALLBACK_URLS="$WS_FALLBACKS"

echo
echo "APK ready:"
echo "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
