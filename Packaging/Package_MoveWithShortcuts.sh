#!/bin/bash

# =============================================================================
# NOTE : Packages MoveWithShortcuts for distribution.
#
#        It reads the following files :
#          - MoveWithShortcuts.jar  (from this folder; the built executable
#                                    JAR; overridable by the first argument)
#          - MoveWithShortcuts.BAT  (from the parent module folder)
#          - MoveWithShortcuts.ps1  (from the parent module folder)
#          - README.md              (from the parent module folder)
#          - LICENSE                (from the parent module folder)
#
#        and produces a ZIP archive containing a top-level "MoveWithShortcuts"
#        folder with all of them, ready to be extracted into the user's chosen
#        MoveWithShortcuts installation folder.
#
#        Usage : Package_MoveWithShortcuts.sh [jar-path] [version]
#          jar-path : the executable JAR to package (default : this folder's
#                     MoveWithShortcuts.jar)
#          version  : optional; when given, the archive is named
#                     "MoveWithShortcuts-<version>.zip"
# =============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# The application name, the base for the file names below :
APP_NAME="MoveWithShortcuts"

# The file names (used both to locate the inputs and as the staged names) :
JAR_NAME="$APP_NAME.jar"
LAUNCHER_BAT_NAME="$APP_NAME.BAT"
LAUNCHER_PS1_NAME="$APP_NAME.ps1"
README_NAME="README.md"
LICENSE_NAME="LICENSE"

# The executable JAR to package (default : this folder's $JAR_NAME) :
JAR_FILE="$SCRIPT_DIR/$JAR_NAME"
if [ -n "${1:-}" ]; then
    JAR_FILE="$1"
fi

# The folder the packaged JAR comes from (shown in the summary) :
JAR_DIR="$(dirname "$JAR_FILE")/"

# Optional version, used in the archive name :
VERSION="${2:-}"

# The other input files (the launchers, README and LICENSE are in the parent module folder) :
LAUNCHER_BAT="$SCRIPT_DIR/../$LAUNCHER_BAT_NAME"
LAUNCHER_PS1="$SCRIPT_DIR/../$LAUNCHER_PS1_NAME"
README_FILE="$SCRIPT_DIR/../$README_NAME"
LICENSE_FILE="$SCRIPT_DIR/../$LICENSE_NAME"

# Name of the top-level folder inside the archive :
PACKAGE_FOLDER="$APP_NAME"
STAGING_FOLDER="$SCRIPT_DIR/$PACKAGE_FOLDER"

# Output archive :
if [ -z "$VERSION" ]; then
    ARCHIVE_NAME="$APP_NAME.zip"
else
    ARCHIVE_NAME="$APP_NAME-$VERSION.zip"
fi
ARCHIVE_FILE="$SCRIPT_DIR/$ARCHIVE_NAME"

# Ensure the required tool is available :
if ! command -v jar >/dev/null 2>&1; then
    echo ""
    echo "CRITICAL ERROR; \"jar\" was not found on the PATH."
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

# Ensure all the input files exist :
MISSING=0

require() {
    if [ ! -f "$1" ]; then
        echo "[FAIL] missing $2 : $1"
        MISSING=1
    fi
}

require "$JAR_FILE"     "executable JAR"
require "$LAUNCHER_BAT" "Windows launcher"
require "$LAUNCHER_PS1" "PowerShell launcher"
require "$README_FILE"  "README"
require "$LICENSE_FILE" "license"

if [ "$MISSING" -ne 0 ]; then
    echo ""
    echo "CRITICAL ERROR; one or more input files are missing, see above."
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

# Stage the files into the top-level folder :
rm -rf "$STAGING_FOLDER"
mkdir -p "$STAGING_FOLDER"

cp "$JAR_FILE"     "$STAGING_FOLDER/$JAR_NAME"
cp "$LAUNCHER_BAT" "$STAGING_FOLDER/$LAUNCHER_BAT_NAME"
cp "$LAUNCHER_PS1" "$STAGING_FOLDER/$LAUNCHER_PS1_NAME"
cp "$README_FILE"  "$STAGING_FOLDER/$README_NAME"
cp "$LICENSE_FILE" "$STAGING_FOLDER/$LICENSE_NAME"

# Create the archive :
rm -f "$ARCHIVE_FILE"
jar cfM "$ARCHIVE_FILE" -C "$SCRIPT_DIR" "$PACKAGE_FOLDER"
if [ $? -ne 0 ]; then
    echo ""
    echo "CRITICAL ERROR; could not create the archive :"
    echo "$ARCHIVE_FILE"
    echo ""
    read -r -p "Press Enter to exit..."
    exit 1
fi

# Remove the staging folder :
rm -rf "$STAGING_FOLDER"

echo ""
echo "Archive created :"
echo "  $ARCHIVE_FILE"
echo ""
echo "It contains the folder \"$PACKAGE_FOLDER\" with :"
echo "  $JAR_NAME"
echo "    from \"$JAR_DIR\""
echo "  $LAUNCHER_BAT_NAME"
echo "  $LAUNCHER_PS1_NAME"
echo "  $README_NAME"
echo "  $LICENSE_NAME"
echo ""

read -r -p "Press Enter to exit..."
