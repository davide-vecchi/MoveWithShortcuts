# PowerShell helper script to launch the executable JAR.
# This script gets called by a batch file (MoveWithShortcuts.BAT)
# which has the following content to call this script :
#
# @ECHO OFF
# wt pwsh -NoExit -File "C:\MoveWithShortcuts\MoveWithShortcuts.ps1"
#

$OutputEncoding = [Console]::InputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

Set-Location C:\MoveWithShortcuts\

java -jar C:\MoveWithShortcuts\MoveWithShortcuts.jar

# Read-Host "Press Enter to exit"
