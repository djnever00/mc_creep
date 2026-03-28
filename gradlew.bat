@echo off
setlocal

set "APP_HOME=%~dp0"
if not defined JAVA_HOME goto use_java_path
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
goto run

:use_java_path
set "JAVA_EXE=java.exe"

:run
"%JAVA_EXE%" -jar "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" %*
exit /b %ERRORLEVEL%
