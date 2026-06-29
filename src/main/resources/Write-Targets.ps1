# Step 2 (Update Shortcuts)
#
# This script takes a list of update instructions and applies them.
#
# Input: shortcuts-to-update.txt (one line per shortcut, with format path|newTarget)
#
# Output: (optional) update-results.txt for logging success/failure
#

$inputFile = "shortcuts-to-update.txt"
$outputFile = "update-results.txt"

$shell = New-Object -ComObject WScript.Shell
$results = @()

Get-Content $inputFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -eq "") { continue }

    $parts = $line -split '\|', 2
    $path = $parts[0]
    $newTarget = $parts[1]

    try {
        $shortcut = $shell.CreateShortcut($path)
        $shortcut.TargetPath = $newTarget
        $shortcut.Save()
        $results += "$path|SUCCESS"
    } catch {
        $results += "$path|FAILED: $($_.Exception.Message)"
    }
}

$results | Out-File -FilePath $outputFile -Encoding UTF8
