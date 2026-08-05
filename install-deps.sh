#!/bin/bash

# =============================================================================
# install-deps.sh
#
# Installs the DLibs dependencies required by MoveWithShortcuts from the
# Libs-JARs repository.
#
# This script checks for Libs-JARs in ../Libs-JARs/ by default. If not found,
# it prompts the user to either clone it, provide a different path, or abort.
#
# Usage (from Git Bash):
#
#   ./install-deps.sh
#
# =============================================================================

set -e

# Color definitions
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
NC='\033[0m'

# List of DLibs required by MoveWithShortcuts (direct + transitive)
declare -a DEPS=(
    "dapplication"
    "dutil"
    "dfile"
    "dlog"
    "duserinputoutput"
    "dtestng"
    "dtest"
)

VERSION="2.2.0"
TESTNG_VERSION="2.1.0"
GROUP_ID="djavalibraries"

echo ""
echo "========================================"
echo "  Installing dependencies for MoveWithShortcuts"
echo "========================================"
echo ""

# Determine the location of Libs-JARs
LJ_PATH="../Libs-JARs"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Check if LJ exists at the default location
if [ -d "$SCRIPT_DIR/$LJ_PATH/dlibs" ]; then
    LJ_ABSOLUTE_PATH="$(cd "$SCRIPT_DIR/$LJ_PATH" && pwd)"
    echo -e "${GREEN}Found Libs-JARs at: $LJ_ABSOLUTE_PATH${NC}"
else
    echo -e "${YELLOW}Libs-JARs not found at default location: $SCRIPT_DIR/$LJ_PATH${NC}"
    echo ""
    echo "Options:"
    echo "  1. Press Enter to clone Libs-JARs into $LJ_PATH"
    echo "  2. Enter the path to an existing Libs-JARs folder"
    echo "  3. Enter / to terminate"
    echo ""
    read -p "Your choice: " user_input
    
    if [[ "$user_input" == "/" ]]; then
        echo "Aborted."
        exit 0
    fi
    
    if [[ -z "$user_input" ]]; then
        # Clone the repository
        echo "Cloning Libs-JARs into $LJ_PATH..."
        git clone https://github.com/davide-vecchi/Libs-JARs.git "$SCRIPT_DIR/$LJ_PATH"
        if [ $? -ne 0 ]; then
            echo -e "${RED}Failed to clone Libs-JARs.${NC}"
            exit 1
        fi
        LJ_ABSOLUTE_PATH="$(cd "$SCRIPT_DIR/$LJ_PATH" && pwd)"
        echo -e "${GREEN}Cloned Libs-JARs to: $LJ_ABSOLUTE_PATH${NC}"
    else
        # User provided a path
        if [[ "$user_input" = /* ]]; then
            LJ_PATH="$user_input"
        else
            LJ_PATH="$SCRIPT_DIR/$user_input"
        fi
        
        if [ ! -d "$LJ_PATH/dlibs" ]; then
            echo -e "${RED}Error: $LJ_PATH/dlibs does not exist.${NC}"
            echo "Please ensure the path points to the root of the Libs-JARs repository."
            exit 1
        fi
        LJ_ABSOLUTE_PATH="$(cd "$LJ_PATH" && pwd)"
        echo -e "${GREEN}Using Libs-JARs at: $LJ_ABSOLUTE_PATH${NC}"
    fi
fi

echo ""
echo "Installing dependencies..."

for dep in "${DEPS[@]}"; do
    # Determine version (dtestng is 2.1.0, others are 2.2.0)
    if [[ "$dep" == "dtestng" ]]; then
        ver="$TESTNG_VERSION"
    else
        ver="$VERSION"
    fi
    
    jar_file="$LJ_ABSOLUTE_PATH/dlibs/$GROUP_ID/$dep/$ver/$dep-$ver.jar"
    pom_file="$LJ_ABSOLUTE_PATH/dlibs/$GROUP_ID/$dep/$ver/$dep-$ver.pom"
    
    if [ ! -f "$jar_file" ]; then
        echo -e "${YELLOW}WARNING: JAR not found for $dep:$ver. Skipping.${NC}"
        continue
    fi
    
    if [ ! -f "$pom_file" ]; then
        echo -e "${YELLOW}WARNING: POM not found for $dep:$ver. Skipping.${NC}"
        continue
    fi
    
    echo -e "Installing ${CYAN}$dep${NC} ($ver)..."
    mvn install:install-file -Dfile="$jar_file" -DpomFile="$pom_file"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}  Success${NC}"
    else
        echo -e "${RED}  Failed${NC}"
        exit 1
    fi
    echo ""
done

echo -e "${GREEN}Installation complete.${NC}"
