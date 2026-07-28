# PowerShell helper script to launch the executable JAR.
# This script typically gets called by a batch file (MoveWithShortcuts.BAT)
#  :
#
# @ECHO OFF
# wt pwsh -NoExit -File "MoveWithShortcuts.ps1"
#

param(
    [string]$WorkDir
)

# Set the output encoding :

$OutputEncoding = [Console]::InputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

# Handle the setting of the working folder :

if ($WorkDir) {

    # : The -WorkDir argument was passed.

    Set-Location $WorkDir
}
else {

    # : The -WorkDir argument was not passed;
    #   fallback to the folder where this script is :

    Set-Location $PSScriptRoot
}
# Start the executable JAR, which must be in the same folder where this script is :

$jarPath = "$PSScriptRoot\MoveWithShortcuts.jar"

[System.Console]::Out.WriteLine()
[System.Console]::Out.WriteLine("java -jar ""$jarPath""")

java -jar "$jarPath"

# Read-Host "Press Enter to exit"
