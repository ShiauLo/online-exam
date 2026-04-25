#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="/opt/online-exam/apis/apis"
RUNTIME_ROOT=""
ENV_FILE=""
SERVICES_CSV=""

# shellcheck disable=SC1091
source "$SCRIPT_DIR/server-service-common.sh"

usage() {
  cat <<'EOF'
用法:
  bash scripts/deploy/stop-all-server.sh
  bash scripts/deploy/stop-all-server.sh --services exam-gateway,exam-realtime

参数:
  --repo <path>          工程目录，默认 /opt/online-exam/apis/apis
  --runtime-root <path>  运行时根目录，默认取工程父目录
  --env-file <path>      环境变量文件，默认优先使用 <runtime-root>/.env
  --services <csv>       仅停止指定服务，逗号分隔；默认停止全部
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)
      REPO_DIR="$2"
      shift 2
      ;;
    --runtime-root)
      RUNTIME_ROOT="$2"
      shift 2
      ;;
    --env-file)
      ENV_FILE="$2"
      shift 2
      ;;
    --services)
      SERVICES_CSV="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "未知参数: $1" >&2
      usage
      exit 1
      ;;
  esac
done

resolve_runtime_paths
mkdir -p "$LOG_DIR" "$PID_DIR"

requested_file="$(mktemp)"
trap 'rm -f "$requested_file"' EXIT
resolve_requested_services "$SERVICES_CSV" >"$requested_file"

write_section "停止服务"

for service in "${SHUTDOWN_ORDER[@]}"; do
  if ! grep -Fxq "$service" "$requested_file"; then
    continue
  fi

  if is_service_running "$service"; then
    echo "停止 $service"
  else
    echo "$service 未运行，跳过。"
  fi

  stop_service "$service"
done

write_section "停止完成"
echo "PID 目录: $PID_DIR"
