#!/bin/bash

DIR="${0%/*}"
EXTRA_ARGS=(JVM-ARGS)
export CDS_JVM_OPTS="${EXTRA_ARGS[*]}"
export _JAVA_OPTIONS=""
export JAVA_TOOL_OPTIONS=""

"$DIR/../../runtime/Contents/Home/bin/xpiped" "$@"

read -rsp "Press any key to close" -n 1 key
