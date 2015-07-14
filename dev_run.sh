#!/usr/bin/env bash
BIN=`which sbt`
SCRIPT_PATH=$(cd ${0%/*} && echo $PWD/${0##*/})
ROOT=`dirname "${SCRIPT_PATH}"`
CONFIGS="${ROOT}/dev-configs"

action="$1"

if [[ -z $action ]];then
    action="start"
fi

cd "${ROOT}"

echo "action=$action"

case "$action" in
    "start")
        "$BIN" \
            "-Dlogback.configurationFile=${CONFIGS}/logback-dev.xml" \
            "-Dconfig.file=${CONFIGS}/dev.conf" \
            "-DPORT=2550" \
            "-DHOST=127.0.0.1" \
            run
        ;;
    "add-node")
        port=$2
        "$BIN" \
            "-Dlogback.configurationFile=${CONFIGS}/logback-dev.xml" \
            "-Dconfig.file=${CONFIGS}/dev.conf" \
            "-DPORT=${port}" \
            "-DHOST=127.0.0.1" \
            run
        ;;
    *)
        echo "unknown command"
        exit 1
        ;;
esac
