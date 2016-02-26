@echo off
echo Invoke this script from the src/main/native folder
echo.

:: build the win32 ThreadCPUTimer library
echo Building ThreadCPUTimer for Win32
echo.
cl /LD /I%JAVA_HOME%\include /I%JAVA_HOME%\include\win32 /I. win32\ThreadCPUTimer.c /FoMETA-INF\lib\ -o META-INF\lib\threadcputimer.dll
echo.
echo Build complete
