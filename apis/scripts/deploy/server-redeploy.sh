#!/usr/bin/env bash
set -euo pipefail

REPO_DIR="/opt/online-exam/apis"
RUNTIME_ROOT=""
MODE=""
SERVICES_CSV=""
BRANCH="main"

ALL_SERVICES=(
  "exam-gateway"
  "exam-account"
  "exam-class"
  "exam-question"
  "exam-paper"
  "exam-system"
  "exam-core"
  "exam-score"
  "exam-issue-core"
  "exam-resource"
  "exam-realtime"
  "exam-issue-notify"
)

usage() {
  cat <<'EOF'
用法:
  bash scripts/deploy/server-redeploy.sh --repo /opt/online-exam/apis --all
  bash scripts/deploy/server-redeploy.sh --repo /opt/online-exam/apis --services exam-account,exam-gateway

参数:
  --repo <path>          服务器仓库目录
  --runtime-root <path>  运行时根目录，默认取仓库父目录
  --all                  重建并重启全部后端服务
  --services <csv>       重建并重启指定服务，逗号分隔
EOF
}

write_section() {
  printf '\n=== %s ===\n' "$1"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "缺少命令: $1" >&2
    exit 1
  fi
}

contains_service() {
  local target="$1"
  local item
  for item in "${ALL_SERVICES[@]}"; do
    if [[ "$item" == "$target" ]]; then
      return 0
    fi
  done
  return 1
}

service_type() {
  case "$1" in
    exam-realtime|exam-issue-notify) echo "node" ;;
    *) echo "java" ;;
  esac
}

service_port() {
  case "$1" in
    exam-gateway) echo "8080" ;;
    exam-account) echo "8081" ;;
    exam-class) echo "8082" ;;
    exam-question) echo "8083" ;;
    exam-paper) echo "8084" ;;
    exam-system) echo "8085" ;;
    exam-core) echo "8086" ;;
    exam-score) echo "8087" ;;
    exam-issue-core) echo "8088" ;;
    exam-resource) echo "8089" ;;
    exam-realtime) echo "8090" ;;
    exam-issue-notify) echo "8091" ;;
    *)
      echo "未知服务端口: $1" >&2
      exit 1
      ;;
  esac
}

service_entry() {
  case "$1" in
    exam-realtime|exam-issue-notify) echo "dist/src/server.js" ;;
    *)
      echo ""
      ;;
  esac
}

service_jar() {
  echo "$1/target/$1-0.0.1-SNAPSHOT.jar"
}

wait_port() {
  local port="$1"
  local timeout_seconds="${2:-90}"
  local start_time
  start_time="$(date +%s)"

  while (( "$(date +%s)" - start_time < timeout_seconds )); do
    if bash -lc "exec 3<>/dev/tcp/127.0.0.1/$port" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  return 1
}

stop_pid_if_exists() {
  local pid_file="$1"
  if [[ ! -f "$pid_file" ]]; then
    return 0
  fi

  local pid
  pid="$(cat "$pid_file" 2>/dev/null || true)"
  if [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1; then
    kill "$pid" >/dev/null 2>&1 || true
    for _ in $(seq 1 15); do
      if ! kill -0 "$pid" >/dev/null 2>&1; then
        rm -f "$pid_file"
        return 0
      fi
      sleep 1
    done
    kill -9 "$pid" >/dev/null 2>&1 || true
  fi

  rm -f "$pid_file"
}

stop_service() {
  local service="$1"
  local pid_file="$PID_DIR/$service.pid"
  local port
  port="$(service_port "$service")"

  stop_pid_if_exists "$pid_file"

  if command -v pgrep >/dev/null 2>&1; then
    local pattern
    if [[ "$(service_type "$service")" == "java" ]]; then
      pattern="$REPO_DIR/$(service_jar "$service")"
    else
      pattern="$REPO_DIR/$service/$(service_entry "$service")"
    fi

    local pids
    pids="$(pgrep -f "$pattern" || true)"
    if [[ -n "$pids" ]]; then
      echo "$pids" | xargs -r kill >/dev/null 2>&1 || true
      sleep 3
      echo "$pids" | xargs -r kill -9 >/dev/null 2>&1 || true
    fi
  fi

  if command -v fuser >/dev/null 2>&1; then
    fuser -k "${port}/tcp" >/dev/null 2>&1 || true
  fi
}

build_service() {
  local service="$1"
  write_section "构建 $service"
  if [[ "$(service_type "$service")" == "java" ]]; then
    (cd "$REPO_DIR" && ./mvnw -pl "$service" -am package -DskipTests)
  else
    (
      cd "$REPO_DIR/$service"
      if [[ ! -d node_modules ]]; then
        npm install
      fi
      npm run build
    )
  fi
}

start_service() {
  local service="$1"
  local service_dir="$REPO_DIR/$service"
  local out_log="$LOG_DIR/$service.out.log"
  local err_log="$LOG_DIR/$service.err.log"
  local pid_file="$PID_DIR/$service.pid"
  local port
  port="$(service_port "$service")"

  mkdir -p "$LOG_DIR" "$PID_DIR"
  : >"$out_log"
  : >"$err_log"

  stop_service "$service"

  write_section "启动 $service"
  if [[ "$(service_type "$service")" == "java" ]]; then
    local jar_path="$REPO_DIR/$(service_jar "$service")"
    if [[ ! -f "$jar_path" ]]; then
      echo "缺少可启动 jar: $jar_path" >&2
      exit 1
    fi

    (
      cd "$service_dir"
      nohup java -jar "$jar_path" --spring.profiles.active=dev,local >>"$out_log" 2>>"$err_log" &
      echo $! >"$pid_file"
    )
  else
    local entry_file="$service_dir/$(service_entry "$service")"
    if [[ ! -f "$entry_file" ]]; then
      echo "缺少 Node 启动入口: $entry_file" >&2
      exit 1
    fi

    (
      cd "$service_dir"
      nohup node "$entry_file" >>"$out_log" 2>>"$err_log" &
      echo $! >"$pid_file"
    )
  fi

  if ! wait_port "$port" 120; then
    echo "$service 启动超时，端口 $port 未就绪。" >&2
    echo "标准输出日志: $out_log" >&2
    tail -n 80 "$out_log" >&2 || true
    echo "错误日志: $err_log" >&2
    tail -n 80 "$err_log" >&2 || true
    exit 1
  fi

  echo "$service 已启动，端口 $port"
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
    --services)
      MODE="services"
      SERVICES_CSV="$2"
      shift 2
      ;;
    --all)
      MODE="all"
      shift
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

if [[ -z "$MODE" ]]; then
  echo "必须传入 --all 或 --services。" >&2
  usage
  exit 1
fi

if [[ ! -d "$REPO_DIR" ]]; then
  echo "仓库目录不存在: $REPO_DIR" >&2
  exit 1
fi

if [[ -z "$RUNTIME_ROOT" ]]; then
  RUNTIME_ROOT="$(cd "$REPO_DIR/.." && pwd)"
fi

LOG_DIR="$RUNTIME_ROOT/runtime-logs"
PID_DIR="$RUNTIME_ROOT/runtime-pids"

require_command git
require_command bash
require_command java
require_command node
require_command npm

ORIGIN_URL="$(git -C "$REPO_DIR" remote get-url origin)"
if [[ "$ORIGIN_URL" == https://* ]]; then
  echo "服务器仓库 origin 当前为 HTTPS，请改为 SSH 地址后再部署: $ORIGIN_URL" >&2
  exit 1
fi

if [[ -n "$(git -C "$REPO_DIR" status --porcelain)" ]]; then
  echo "服务器工作区存在未提交改动，请先清理后再部署。" >&2
  git -C "$REPO_DIR" status --short >&2
  exit 1
fi

write_section "同步服务器仓库"
git -C "$REPO_DIR" fetch origin
git -C "$REPO_DIR" checkout "$BRANCH"
git -C "$REPO_DIR" pull --ff-only origin "$BRANCH"

TARGET_SERVICES=()
if [[ "$MODE" == "all" ]]; then
  TARGET_SERVICES=("${ALL_SERVICES[@]}")
else
  IFS=',' read -r -a RAW_SERVICES <<<"$SERVICES_CSV"
  for item in "${RAW_SERVICES[@]}"; do
    service="$(echo "$item" | xargs)"
    if [[ -z "$service" ]]; then
      continue
    fi
    if ! contains_service "$service"; then
      echo "不支持的服务名: $service" >&2
      exit 1
    fi
    TARGET_SERVICES+=("$service")
  done
fi

if [[ "${#TARGET_SERVICES[@]}" -eq 0 ]]; then
  echo "没有可部署的服务。" >&2
  exit 1
fi

write_section "部署服务"
printf '%s\n' "${TARGET_SERVICES[@]}"

for service in "${TARGET_SERVICES[@]}"; do
  build_service "$service"
  start_service "$service"
done

write_section "部署完成"
echo "日志目录: $LOG_DIR"
echo "PID 目录: $PID_DIR"
