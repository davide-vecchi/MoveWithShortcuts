# PowerShell helper script to launch the executable JAR.
# This script typically gets called by a batch file (MoveWithShortcuts.BAT)
#  :
#
# @ECHO OFF
# wt pwsh -NoExit -File "MoveWithShortcuts.ps1"
#
# Any argument other than -WorkDir is forwarded to the JAR (e.g. -from, -to,
# -search, -verbosity, -help), and -console is added unless it is already
# present, because this is the console launcher.
#

param(
    [string]$WorkDir
)

# Set the output encoding :

$OutputEncoding = [Console]::InputEncoding = [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

# Handle the setting of the work folder :

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
# The arguments not consumed by this script (that is, everything except -WorkDir)
# are collected in $args and forwarded to the program unchanged.

$jarPath = "$PSScriptRoot\MoveWithShortcuts.jar"

# This launcher is the console launcher : add -console unless the caller already
# passed it explicitly.

$consoleArgs = @()

if (-not (($args -contains "-console") -or ($args -contains "--console"))) {

    $consoleArgs += "-console"
}
$consoleArgs += $args

[System.Console]::Out.WriteLine()
[System.Console]::Out.WriteLine("java -jar ""$jarPath"" $($consoleArgs -join ' ')")

java -jar "$jarPath" @consoleArgs

# Read-Host "Press Enter to exit"
