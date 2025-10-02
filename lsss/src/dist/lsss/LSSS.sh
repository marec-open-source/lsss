#!/bin/bash

echo Starting LSSS
echo -------------

export TOP_INSTALLATION_DIR=$(cd "$(dirname "$0")/.."; pwd)
. "$TOP_INSTALLATION_DIR/lib/FindJava.sh"

# MAX_MEMORY_MB is default 2/3 of total physical memory in MB, limited to [3000 MB, 30_000 MB]
if [[ "$LSSS_MAX_MEMORY_MB" != "" ]]; then MAX_MEMORY_MB=$LSSS_MAX_MEMORY_MB; fi
# To manually specify max memory set the environment variable LSSS_MAX_MEMORY_MB,
# or uncomment and edit the following line:
# MAX_MEMORY_MB=3072

"$JAVA" $JAVA_OPTS "-Xmx${MAX_MEMORY_MB}m" -classpath "$TOP_INSTALLATION_DIR/lib/jar/*" \
   "-Djava.library.path=$JAVA_LIBRARY_PATH" "-Djna.library.path=$JAVA_LIBRARY_PATH" \
   -XX:-UseGCOverheadLimit -XX:-OmitStackTraceInFastThrow \
   "-splash:$TOP_INSTALLATION_DIR/lsss/LSSS-splash.png" \
   no.imr.lsss.main.LsssMain "$@"

err=$?
if [[ $err != 0 ]]; then echo "Error code $err"; read -r -p "Press enter to continue..."; exit $err; fi
