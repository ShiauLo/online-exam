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
  bash scripts/deploy/status-all-server.sh
  bash scripts/deploy/status-all-server.sh --services exam-gateway,exam-realtime

参数:
  --repo <path>          工程目录，默认 /opt/online-exam/apis/apis
  --runtime-root <path>  运行时根目录，默认取工程父目录
  --env-file <path>      环境变量文件，默认优先使用 <runtime-root>/.env
  --services <csv>       仅查看指定服务，逗号分隔；默认查看全部
EOF
}

print_infra_status() {
  local port name
  write_section "基础依赖状态"
  for port in "${INFRA_PORTS[@]}"; do
    case "$port" in
      3306) name="mysql" ;;
      6379) name="redis" ;;
      8848) name="nacos" ;;
      *) name="port-$port" ;;
    esac

    if wait_port "$port" 1; then
      echo "$name ($port): UP"
    else
      echo "$name ($port): DOWN"
    fi
  done
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

print_infra_status

write_section "微服务状态"
while IFS= read -r service; do
  port="$(service_port "$service")"
  pid="$(read_service_pid "$service")"
  out_log="$(out_log_path "$service")"
  err_log="$(err_log_path "$service")"

  if is_service_running "$service"; then
    echo "$service ($port): UP pid=${pid:-unknown}"
  else
    echo "$service ($port): DOWN pid=${pid:-none}"
  fi
  echo "  out: $out_log"
  echo "  err: $err_log"
done <"$requested_file"
