@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL

REM Start Windows Terminal around a PowerShell instance.
REM If pwsh.exe (PowerShell 7) is not available on the target system,
REM replace "pwsh" with "powershell". In alternative the program can
REM also be executed with the normal "java -jar RenameWithLinks.jar"
REM but the best Unicode support is provided by Windows Terminal.

wt pwsh -NoExit -File "C:\RenameWithLinks\RenameWithLinks.ps1"
