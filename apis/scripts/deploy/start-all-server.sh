#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="/opt/online-exam/apis/apis"
RUNTIME_ROOT=""
ENV_FILE=""
SERVICES_CSV=""
INFRA_TIMEOUT=60

# shellcheck disable=SC1091
source "$SCRIPT_DIR/server-service-common.sh"

usage() {
  cat <<'EOF'
用法:
  bash scripts/deploy/start-all-server.sh
  bash scripts/deploy/start-all-server.sh --services exam-account,exam-core,exam-gateway

参数:
  --repo <path>          工程目录，默认 /opt/online-exam/apis/apis
  --runtime-root <path>  运行时根目录，默认取工程父目录
  --env-file <path>      环境变量文件，默认优先使用 <runtime-root>/.env
  --services <csv>       仅启动指定服务，逗号分隔；默认启动全部
  --infra-timeout <sec>  基础依赖等待秒数，默认 60
EOF
}

start_group() {
  local group_name="$1"
  shift

  local pids=()
  local service
  local failed=0

  if [[ "$#" -eq 0 ]]; then
    return 0
  fi

  write_section "启动分组: $group_name"
  printf '%s\n' "$@"

  for service in "$@"; do
    (
      start_service "$service"
    ) &
    pids+=("$!")
  done

  for pid in "${pids[@]}"; do
    if ! wait "$pid"; then
      failed=1
    fi
  done

  if (( failed != 0 )); then
    echo "分组启动失败: $group_name" >&2
    return 1
  fi
}

filter_group() {
  local requested_file="$1"
  shift

  local service
  for service in "$@"; do
    if grep -Fxq "$service" "$requested_file"; then
      printf '%s\n' "$service"
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
    --infra-timeout)
      INFRA_TIMEOUT="$2"
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
load_env_file
apply_runtime_defaults

require_command bash
require_command java
require_command node
mkdir -p "$LOG_DIR" "$PID_DIR"

if [[ ! -d "$REPO_DIR" ]]; then
  echo "工程目录不存在: $REPO_DIR" >&2
  exit 1
fi

write_section "基础依赖检查"
wait_infra_ready "$INFRA_TIMEOUT"
echo "3306/6379/8848 已就绪。"

requested_file="$(mktemp)"
trap 'rm -f "$requested_file"' EXIT
resolve_requested_services "$SERVICES_CSV" >"$requested_file"

foundation_services=()
business_services=()
edge_services=()

while IFS= read -r service; do
  foundation_services+=("$service")
done < <(filter_group "$requested_file" "${FOUNDATION_SERVICES[@]}")

while IFS= read -r service; do
  business_services+=("$service")
done < <(filter_group "$requested_file" "${BUSINESS_SERVICES[@]}")

while IFS= read -r service; do
  edge_services+=("$service")
done < <(filter_group "$requested_file" "${EDGE_SERVICES[@]}")

start_group "基础服务" "${foundation_services[@]}"
start_group "业务服务" "${business_services[@]}"
start_group "边缘服务" "${edge_services[@]}"

write_section "启动完成"
echo "日志目录: $LOG_DIR"
echo "PID 目录: $PID_DIR"
