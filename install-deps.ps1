# =============================================================================
# install-deps.ps1
#
# Installs the DLibs dependencies required by MoveWithShortcuts from the
# Libs-JARs repository.
#
# This script checks for Libs-JARs in ../Libs-JARs/ by default. If not found,
# it prompts the user to either clone it, provide a different path, or abort.
#
# Usage:
#   Right-click this file and select "Run with PowerShell", or
#   run from PowerShell: .\install-deps.ps1
#   Or double-click install-deps.bat
# =============================================================================

# Color definitions
function Write-Success { Write-Host $args[0] -ForegroundColor Green }
function Write-Warning { Write-Host $args[0] -ForegroundColor Yellow }
function Write-Error { Write-Host $args[0] -ForegroundColor Red }
function Write-Info { Write-Host $args[0] -ForegroundColor Cyan }
function Write-Normal { Write-Host $args[0] -ForegroundColor White }

# Dependencies required by MoveWithShortcuts (direct + transitive)
$DEPS = @(
    "dapplication",
    "dutil",
    "dfile",
    "dlog",
    "duserinputoutput",
    "dtestng",
    "dtest"
)

$VERSION = "2.2.0"
$TESTNG_VERSION = "2.1.0"
$GROUP_ID = "djavalibraries"

Write-Normal ""
Write-Normal "========================================"
Write-Normal "  Installing dependencies for MoveWithShortcuts"
Write-Normal "========================================"
Write-Normal ""

# Determine the location of Libs-JARs
$LJ_PATH = "..\Libs-JARs"
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path

# Check if LJ exists at the default location
if (Test-Path "$SCRIPT_DIR\$LJ_PATH\dlibs") {
    $LJ_ABSOLUTE_PATH = Resolve-Path "$SCRIPT_DIR\$LJ_PATH"
    Write-Success "Found Libs-JARs at: $LJ_ABSOLUTE_PATH"
} else {
    Write-Warning "Libs-JARs not found at default location: $SCRIPT_DIR\$LJ_PATH"
    Write-Normal ""
    Write-Normal "Options:"
    Write-Normal "  1. Press Enter to clone Libs-JARs into $LJ_PATH"
    Write-Normal "  2. Enter the path to an existing Libs-JARs folder"
    Write-Normal "  3. Enter / to terminate"
    Write-Normal ""
    $user_input = Read-Host "Your choice"
    
    if ($user_input -eq "/") {
        Write-Normal "Aborted."
        exit 0
    }
    
    if ([string]::IsNullOrWhiteSpace($user_input)) {
        # Clone the repository
        Write-Normal "Cloning Libs-JARs into $LJ_PATH..."
        git clone https://github.com/davide-vecchi/Libs-JARs.git "$SCRIPT_DIR\$LJ_PATH"
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to clone Libs-JARs."
            Read-Host "Press Enter to exit"
            exit 1
        }
        $LJ_ABSOLUTE_PATH = Resolve-Path "$SCRIPT_DIR\$LJ_PATH"
        Write-Success "Cloned Libs-JARs to: $LJ_ABSOLUTE_PATH"
    } else {
        # User provided a path
        if ($user_input -match "^[A-Za-z]:") {
            $LJ_PATH = $user_input
        } else {
            $LJ_PATH = "$SCRIPT_DIR\$user_input"
        }
        
        if (-not (Test-Path "$LJ_PATH\dlibs")) {
            Write-Error "Error: $LJ_PATH\dlibs does not exist."
            Write-Normal "Please ensure the path points to the root of the Libs-JARs repository."
            Read-Host "Press Enter to exit"
            exit 1
        }
        $LJ_ABSOLUTE_PATH = Resolve-Path $LJ_PATH
        Write-Success "Using Libs-JARs at: $LJ_ABSOLUTE_PATH"
    }
}

Write-Normal ""
Write-Normal "Installing dependencies..."

$successCount = 0
$failCount = 0

foreach ($dep in $DEPS) {
    # Determine version (dtestng is 2.1.0, others are 2.2.0)
    if ($dep -eq "dtestng") {
        $ver = $TESTNG_VERSION
    } else {
        $ver = $VERSION
    }
    
    $jarFile = "$LJ_ABSOLUTE_PATH\dlibs\$GROUP_ID\$dep\$ver\$dep-$ver.jar"
    $pomFile = "$LJ_ABSOLUTE_PATH\dlibs\$GROUP_ID\$dep\$ver\$dep-$ver.pom"
    
    if (-not (Test-Path $jarFile)) {
        Write-Warning "WARNING: JAR not found for " + $dep + ":" + $ver + ". Skipping."
        $failCount++
        continue
    }
    
    if (-not (Test-Path $pomFile)) {
        Write-Warning "WARNING: JAR not found for " + $dep + ":" + $ver + ". Skipping."
        $failCount++
        continue
    }
    
    Write-Info "Installing $dep ($ver)..."
    & mvn install:install-file -Dfile="$jarFile" -DpomFile="$pomFile"
    
    if ($LASTEXITCODE -eq 0) {
        Write-Success "  Success"
        $successCount++
    } else {
        Write-Error "  Failed"
        $failCount++
        Read-Host "Press Enter to exit"
        exit 1
    }
    Write-Normal ""
}

Write-Normal ""
Write-Success "Installation complete."
Write-Normal "  Successful: $successCount"
Write-Error "  Failed: $failCount"
Read-Host "Press Enter to exit"
