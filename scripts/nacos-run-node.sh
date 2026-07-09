#!/usr/bin/env bash
set -euo pipefail

BASE_DIR="${1:?Usage: nacos-run-node.sh <nacos-base-dir>}"
MEMORY="${CUSTOM_NACOS_MEMORY:--Xms512m -Xmx512m -Xmn256m}"

if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA_BIN="${JAVA_HOME}/bin/java"
elif [[ -x "/Users/liminjie/Documents/software/jdk/zulu21.48.17-ca-jdk21.0.10-macosx_aarch64/zulu-21.jdk/Contents/Home/bin/java" ]]; then
  JAVA_BIN="/Users/liminjie/Documents/software/jdk/zulu21.48.17-ca-jdk21.0.10-macosx_aarch64/zulu-21.jdk/Contents/Home/bin/java"
else
  JAVA_BIN="$(command -v java)"
fi

mkdir -p "${BASE_DIR}/logs"
{
  echo
  echo "===== $(date '+%Y-%m-%d %H:%M:%S') starting nacos node: ${BASE_DIR} ====="
  echo "JAVA_BIN=${JAVA_BIN}"
  echo "CUSTOM_NACOS_MEMORY=${MEMORY}"
} >> "${BASE_DIR}/logs/startup.log"

read -r -a MEMORY_OPTS <<< "${MEMORY}"

exec >> "${BASE_DIR}/logs/startup.log" 2>&1
exec "${JAVA_BIN}" \
  -server \
  "${MEMORY_OPTS[@]}" \
  -XX:-OmitStackTraceInFastThrow \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath="${BASE_DIR}/logs/java_heapdump.hprof" \
  -XX:-UseLargePages \
  -Dnacos.server.ip=127.0.0.1 \
  -Dnacos.member.list= \
  -Xlog:gc*:file="${BASE_DIR}/logs/nacos_gc.log":time,tags:filecount=10,filesize=100m \
  --add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
  --add-opens=java.base/java.util=ALL-UNNAMED \
  -Dnacos.deployment.type=merged \
  -Dloader.path="${BASE_DIR}/plugins,${BASE_DIR}/plugins/health,${BASE_DIR}/plugins/cmdb,${BASE_DIR}/plugins/selector" \
  -Dnacos.home="${BASE_DIR}" \
  -jar "${BASE_DIR}/target/nacos-server.jar" \
  --spring.config.additional-location="file:${BASE_DIR}/conf/" \
  --logging.config="${BASE_DIR}/conf/nacos-logback.xml" \
  --server.max-http-request-header-size=524288 \
  nacos.nacos
