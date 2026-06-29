# Step 1 (Read Targets) :
#
# This script takes a list of shortcut paths and outputs each shortcut's current target.
#
# Input: shortcuts-to-read.txt (list of .lnk paths, one per line)
#
# Output: targets-output.txt (one line per shortcut, with format path|target)
#

$inputFile = "shortcuts-to-read.txt"
$outputFile = "targets-output.txt"

$shell = New-Object -ComObject WScript.Shell
$results = @()

Get-Content $inputFile | ForEach-Object {
    $path = $_.Trim()
    if ($path -eq "") { continue }

    try {
        $shortcut = $shell.CreateShortcut($path)
        $target = $shortcut.TargetPath
        $results += "$path|$target"
    } catch {
        $results += "$path|[ERROR]"
    }
}

$results | Out-File -FilePath $outputFile -Encoding UTF8
