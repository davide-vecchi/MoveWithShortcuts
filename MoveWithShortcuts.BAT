@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

REM Start Windows Terminal around a PowerShell instance.
REM This provides the best Unicode support.
REM
REM If pwsh.exe (PowerShell 7) is not available on the target system,
REM replace "pwsh" with "powershell".
REM
REM If Windows Terminal is not installed, MoveWithShortcuts can be executed
REM directly in PowerShell. To do that, just use the command below that does
REM not start with "wt ". This provides less good Unicode support than
REM Windows Terminal does.
REM
REM In alternative, MoveWithShortcuts can also be executed with the normal
REM "java -jar MoveWithShortcuts.jar"
REM but that provides the least good Unicode support.

wt powershell -NoExit -File "%~dp0MoveWithShortcuts.ps1" -WorkDir "%CD%"

REM powershell -ExecutionPolicy Bypass -NoExit -File "%~dp0MoveWithShortcuts.ps1" -WorkDir "%CD%"

REM java -jar MoveWithShortcuts.jar

PAUSE
