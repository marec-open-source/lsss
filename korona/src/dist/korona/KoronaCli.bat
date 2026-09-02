@echo off

echo Starting KoronaCli
echo ------------------

for %%i in ("%~dp0..") do set TOP_INSTALLATION_DIR=%%~fi
call "%TOP_INSTALLATION_DIR%\lib\FindJava.bat"

rem MAX_MEMORY_MB is default 2/3 of total physical memory in MB, limited to [3000 MB, 30_000 MB]
if defined KORONA_CLI_MAX_MEMORY_MB set MAX_MEMORY_MB=%KORONA_CLI_MAX_MEMORY_MB%
rem To manually specify max memory set the environment variable KORONA_CLI_MAX_MEMORY_MB,
rem or uncomment and edit the following line:
rem set MAX_MEMORY_MB=3072

"%JAVA%" %JAVA_OPTS% -Xmx%MAX_MEMORY_MB%m -classpath "%TOP_INSTALLATION_DIR%\lib\jar\*" ^
   "-Djava.library.path=%JAVA_LIBRARY_PATH%" "-Djna.library.path=%JAVA_LIBRARY_PATH%" ^
   --enable-native-access=ALL-UNNAMED ^
   -XX:-UseGCOverheadLimit -XX:-OmitStackTraceInFastThrow ^
   no.imr.korona.main.KoronaCliMain %*
