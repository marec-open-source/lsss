if not "%MAREC_JAVA_HOME%" == "" ( set JAVA=%MAREC_JAVA_HOME%\bin\java.exe
) else if exist "%TOP_INSTALLATION_DIR%\jre" ( set JAVA=%TOP_INSTALLATION_DIR%\jre\bin\java.exe
) else if not "%JAVA_HOME%" == "" ( set JAVA=%JAVA_HOME%\bin\java.exe
) else JAVA=java

for /f "tokens=*" %%i in (
   'call "%JAVA%" -classpath "%TOP_INSTALLATION_DIR%\lib\jar\marec-tools-core.jar" no.imr.tools.main.PrintMaxMemoryMbMain'
) do (
   set MAX_MEMORY_MB=%%i
)

set JAVA_LIBRARY_PATH=%TOP_INSTALLATION_DIR%\lib\native\win64
