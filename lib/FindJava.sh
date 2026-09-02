#!/bin/bash

if [[ -n "$MAREC_JAVA_HOME" ]]; then JAVA="$MAREC_JAVA_HOME/bin/java";
elif [[ -d "$TOP_INSTALLATION_DIR/jre" ]]; then JAVA="$TOP_INSTALLATION_DIR/jre/bin/java";
elif [[ -n "$JAVA_HOME" ]]; then JAVA="$JAVA_HOME/bin/java";
else JAVA=java;
fi

MAX_MEMORY_MB="$("$JAVA" -classpath "$TOP_INSTALLATION_DIR/lib/jar/marec-tools-core.jar" no.imr.tools.main.PrintMaxMemoryMbMain)"
if [[ -z "$MAX_MEMORY_MB" ]]; then MAX_MEMORY_MB=3000; fi

case "$(uname)" in
  Darwin) OS_TYPE="macos" ;;
  *)      OS_TYPE="linux" ;;
esac

export JAVA_LIBRARY_PATH="$TOP_INSTALLATION_DIR/lib/native/${OS_TYPE}64"
