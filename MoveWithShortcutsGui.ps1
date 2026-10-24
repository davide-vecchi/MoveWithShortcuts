# PowerShell helper script to launch the MoveWithShortcuts GUI using javaw,
# so that no console window remains open. Any argument is forwarded to the
# program; the operation options prefill the dialog fields.

$jarPath = "$PSScriptRoot\MoveWithShortcuts.jar"

if (-not (Test-Path -LiteralPath $jarPath)) {

    Write-Host ""
    Write-Host "CRITICAL ERROR; the executable JAR was not found :"
    Write-Host "  $jarPath"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

$arguments = @("-jar", "`"$jarPath`"", "-gui") + $args

try {

    Start-Process -FilePath "javaw" -ArgumentList $arguments
}
catch {

    Write-Host ""
    Write-Host "CRITICAL ERROR; could not start javaw :"
    Write-Host "  $($_.Exception.Message)"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}
