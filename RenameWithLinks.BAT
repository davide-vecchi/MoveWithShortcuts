@ECHO OFF

REM Set codepage to UTF-8 (CHCP 65001) :
CHCP 65001 >NUL
wt pwsh -NoExit -File "C:\RenameWithLinks\RenameWithLinks.ps1"
