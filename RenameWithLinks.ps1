# PowerShell helper script to launch the executable JAR.
# This script gets called by a batch file (RenameWithLinks.BAT)
# which has the following content to call this script :
#
# @ECHO OFF
# wt pwsh -NoExit -File "C:\RenameWithLinks\RenameWithLinks.ps1"
#

$OutputEncoding = [Console]::InputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

Set-Location C:\RenameWithLinks\

java -jar C:\RenameWithLinks\RenameWithLinks.jar

# Read-Host "Press Enter to exit"
