#!/usr/bin/env bash

REPO_DIR="${REPO_DIR:-/opt/online-exam/apis/apis}"
RUNTIME_ROOT="${RUNTIME_ROOT:-}"
ENV_FILE="${ENV_FILE:-}"

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

FOUNDATION_SERVICES=(
  "exam-account"
  "exam-system"
)

BUSINESS_SERVICES=(
  "exam-class"
  "exam-question"
  "exam-paper"
  "exam-core"
  "exam-score"
  "exam-issue-core"
  "exam-resource"
)

EDGE_SERVICES=(
  "exam-realtime"
  "exam-issue-notify"
  "exam-gateway"
)

SHUTDOWN_ORDER=(
  "exam-gateway"
  "exam-realtime"
  "exam-issue-notify"
  "exam-resource"
  "exam-issue-core"
  "exam-score"
  "exam-core"
  "exam-paper"
  "exam-question"
  "exam-class"
  "exam-system"
  "exam-account"
)

INFRA_PORTS=(3306 6379 8848)

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

resolve_runtime_paths() {
  if [[ -z "$RUNTIME_ROOT" ]]; then
    RUNTIME_ROOT="$(cd "$REPO_DIR/.." && pwd)"
  fi

  if [[ -z "$ENV_FILE" && -f "$RUNTIME_ROOT/.env" ]]; then
    ENV_FILE="$RUNTIME_ROOT/.env"
  fi

  LOG_DIR="$RUNTIME_ROOT/runtime-logs"
  PID_DIR="$RUNTIME_ROOT/runtime-pids"
  export REPO_DIR RUNTIME_ROOT ENV_FILE LOG_DIR PID_DIR
}

load_env_file() {
  if [[ -n "$ENV_FILE" && -f "$ENV_FILE" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$ENV_FILE"
    set +a
  fi
}

apply_runtime_defaults() {
  export EXAM_NACOS_ADDR="${EXAM_NACOS_ADDR:-127.0.0.1:8848}"
  export EXAM_NACOS_USERNAME="${EXAM_NACOS_USERNAME:-nacos}"
  export EXAM_NACOS_PASSWORD="${EXAM_NACOS_PASSWORD:-nacos}"

  export EXAM_DB_HOST="${EXAM_DB_HOST:-127.0.0.1}"
  export EXAM_DB_PORT="${EXAM_DB_PORT:-3306}"
  export EXAM_DB_NAME="${EXAM_DB_NAME:-online_exam_db}"
  export EXAM_DB_USERNAME="${EXAM_DB_USERNAME:-root}"
  if [[ -z "${EXAM_DB_PASSWORD:-}" && -n "${MYSQL_ROOT_PASSWORD:-}" ]]; then
    export EXAM_DB_PASSWORD="$MYSQL_ROOT_PASSWORD"
  fi

  export EXAM_REDIS_HOST="${EXAM_REDIS_HOST:-127.0.0.1}"
  export EXAM_REDIS_PORT="${EXAM_REDIS_PORT:-6379}"
  export EXAM_REDIS_DATABASE="${EXAM_REDIS_DATABASE:-0}"
  if [[ -z "${EXAM_REDIS_PASSWORD:-}" && -n "${REDIS_PASSWORD:-}" ]]; then
    export EXAM_REDIS_PASSWORD="$REDIS_PASSWORD"
  fi

  export EXAM_JAVA_START_OPTS="${EXAM_JAVA_START_OPTS:--Xms128m -Xmx384m -XX:MaxMetaspaceSize=192m}"
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

artifact_path() {
  local service="$1"
  if [[ "$(service_type "$service")" == "java" ]]; then
    echo "$REPO_DIR/$(service_jar "$service")"
  else
    echo "$REPO_DIR/$service/$(service_entry "$service")"
  fi
}

pid_file_path() {
  echo "$PID_DIR/$1.pid"
}

out_log_path() {
  echo "$LOG_DIR/$1.out.log"
}

err_log_path() {
  echo "$LOG_DIR/$1.err.log"
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

is_pid_running() {
  local pid="$1"
  [[ -n "$pid" ]] && kill -0 "$pid" >/dev/null 2>&1
}

read_service_pid() {
  local pid_file
  pid_file="$(pid_file_path "$1")"
  if [[ -f "$pid_file" ]]; then
    cat "$pid_file" 2>/dev/null || true
  fi
}

find_service_pids() {
  local service="$1"
  local pattern

  if [[ "$(service_type "$service")" == "java" ]]; then
    pattern="$(artifact_path "$service")"
  else
    pattern="$(artifact_path "$service")"
  fi

  if command -v pgrep >/dev/null 2>&1; then
    pgrep -f "$pattern" || true
  fi
}

stop_pid_if_exists() {
  local pid_file="$1"
  if [[ ! -f "$pid_file" ]]; then
    return 0
  fi

  local pid
  pid="$(cat "$pid_file" 2>/dev/null || true)"
  if is_pid_running "$pid"; then
    kill "$pid" >/dev/null 2>&1 || true
    for _ in $(seq 1 15); do
      if ! is_pid_running "$pid"; then
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
  local pid_file port pids
  pid_file="$(pid_file_path "$service")"
  port="$(service_port "$service")"

  stop_pid_if_exists "$pid_file"

  pids="$(find_service_pids "$service")"
  if [[ -n "$pids" ]]; then
    echo "$pids" | xargs -r kill >/dev/null 2>&1 || true
    sleep 2
    echo "$pids" | xargs -r kill -9 >/dev/null 2>&1 || true
  fi

  if command -v fuser >/dev/null 2>&1; then
    fuser -k "${port}/tcp" >/dev/null 2>&1 || true
  fi
}

is_service_running() {
  local service="$1"
  local pid port
  pid="$(read_service_pid "$service")"
  port="$(service_port "$service")"

  if is_pid_running "$pid" && wait_port "$port" 1; then
    return 0
  fi

  if wait_port "$port" 1; then
    return 0
  fi

  return 1
}

ensure_service_artifact() {
  local service="$1"
  local path
  path="$(artifact_path "$service")"
  if [[ ! -f "$path" ]]; then
    echo "$service 缺少构建产物: $path" >&2
    echo "请先执行 server-redeploy.sh 构建该服务。" >&2
    return 1
  fi
}

wait_service_stable() {
  local service="$1"
  local timeout_seconds="${2:-120}"
  local stable_seconds="${3:-8}"
  local pid port elapsed stable_count
  port="$(service_port "$service")"
  elapsed=0
  stable_count=0

  while (( elapsed < timeout_seconds )); do
    pid="$(read_service_pid "$service")"
    if is_pid_running "$pid" && wait_port "$port" 1; then
      stable_count=$((stable_count + 1))
      if (( stable_count >= stable_seconds )); then
        return 0
      fi
    else
      stable_count=0
    fi

    sleep 1
    elapsed=$((elapsed + 1))
  done

  return 1
}

start_service() {
  local service="$1"
  local service_dir out_log err_log pid_file port jar_path entry_file

  if is_service_running "$service"; then
    echo "$service 已在运行，跳过启动。"
    return 0
  fi

  ensure_service_artifact "$service"

  service_dir="$REPO_DIR/$service"
  out_log="$(out_log_path "$service")"
  err_log="$(err_log_path "$service")"
  pid_file="$(pid_file_path "$service")"
  port="$(service_port "$service")"

  mkdir -p "$LOG_DIR" "$PID_DIR"
  : >"$out_log"
  : >"$err_log"

  stop_service "$service"

  if [[ "$(service_type "$service")" == "java" ]]; then
    jar_path="$(artifact_path "$service")"
    (
      cd "$service_dir"
      nohup java $EXAM_JAVA_START_OPTS -jar "$jar_path" \
        --spring.profiles.active=dev,local \
        --EXAM_NACOS_ADDR="$EXAM_NACOS_ADDR" \
        --EXAM_NACOS_USERNAME="$EXAM_NACOS_USERNAME" \
        --EXAM_NACOS_PASSWORD="$EXAM_NACOS_PASSWORD" \
        --EXAM_DB_HOST="$EXAM_DB_HOST" \
        --EXAM_DB_PORT="$EXAM_DB_PORT" \
        --EXAM_DB_NAME="$EXAM_DB_NAME" \
        --EXAM_DB_USERNAME="$EXAM_DB_USERNAME" \
        --EXAM_DB_PASSWORD="$EXAM_DB_PASSWORD" \
        --EXAM_REDIS_HOST="$EXAM_REDIS_HOST" \
        --EXAM_REDIS_PORT="$EXAM_REDIS_PORT" \
        --EXAM_REDIS_DATABASE="$EXAM_REDIS_DATABASE" \
        --EXAM_REDIS_PASSWORD="$EXAM_REDIS_PASSWORD" \
        >>"$out_log" 2>>"$err_log" &
      echo $! >"$pid_file"
    )
  else
    entry_file="$(artifact_path "$service")"
    (
      cd "$service_dir"
      export EXAM_NACOS_ADDR EXAM_NACOS_USERNAME EXAM_NACOS_PASSWORD
      export EXAM_DB_HOST EXAM_DB_PORT EXAM_DB_NAME EXAM_DB_USERNAME EXAM_DB_PASSWORD
      export EXAM_REDIS_HOST EXAM_REDIS_PORT EXAM_REDIS_DATABASE EXAM_REDIS_PASSWORD
      nohup node "$entry_file" >>"$out_log" 2>>"$err_log" &
      echo $! >"$pid_file"
    )
  fi

  if ! wait_service_stable "$service" 150 8; then
    echo "$service 启动失败或未稳定。" >&2
    echo "标准输出日志: $out_log" >&2
    tail -n 80 "$out_log" >&2 || true
    echo "错误日志: $err_log" >&2
    tail -n 80 "$err_log" >&2 || true
    return 1
  fi

  echo "$service 已启动，端口 $port"
}

wait_infra_ready() {
  local port
  for port in "${INFRA_PORTS[@]}"; do
    if ! wait_port "$port" "${1:-60}"; then
      echo "基础依赖端口未就绪: $port" >&2
      return 1
    fi
  done
}

resolve_requested_services() {
  local csv="$1"
  local selected=()
  local raw service

  if [[ -z "$csv" ]]; then
    printf '%s\n' "${ALL_SERVICES[@]}"
    return 0
  fi

  IFS=',' read -r -a raw <<<"$csv"
  for service in "${raw[@]}"; do
    service="$(echo "$service" | xargs)"
    if [[ -z "$service" ]]; then
      continue
    fi
    if ! contains_service "$service"; then
      echo "不支持的服务名: $service" >&2
      return 1
    fi
    selected+=("$service")
  done

  printf '%s\n' "${selected[@]}"
}
