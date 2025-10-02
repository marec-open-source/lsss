@echo off

echo Starting LSSS
echo -------------

pushd %~dp0\..
set TOP_INSTALLATION_DIR=%cd%
popd
call "%TOP_INSTALLATION_DIR%\lib\FindJava.bat"

rem MAX_MEMORY_MB is default 2/3 of total physical memory in MB, limited to [3000 MB, 30_000 MB]
if not "%LSSS_MAX_MEMORY_MB%" == "" set MAX_MEMORY_MB=%LSSS_MAX_MEMORY_MB%
rem To manually specify max memory set the environment variable LSSS_MAX_MEMORY_MB,
rem or uncomment and edit the following line:
rem set MAX_MEMORY_MB=3072

"%JAVA%" %JAVA_OPTS% -Xmx%MAX_MEMORY_MB%m -classpath "%TOP_INSTALLATION_DIR%\lib\jar\*" ^
   "-Djava.library.path=%JAVA_LIBRARY_PATH%" "-Djna.library.path=%JAVA_LIBRARY_PATH%" ^
   -XX:-UseGCOverheadLimit -XX:-OmitStackTraceInFastThrow ^
   "-splash:%TOP_INSTALLATION_DIR%\lsss\LSSS-splash.png" ^
   no.imr.lsss.main.LsssMain %*

set err=%errorlevel%
if %err% == 0 goto :eof
echo error code %err%
pause
exit /b %err%
