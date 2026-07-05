#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper startup script — properly bootstraps Maven distribution
# ----------------------------------------------------------------------------
set -e

# Resolve the project base directory (where this script lives)
MAVEN_PROJECTBASEDIR=$(cd "$(dirname "$0")" && pwd)
export MAVEN_PROJECTBASEDIR

MAVEN_WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

# Read the Maven distribution URL from wrapper properties
DISTRIBUTION_URL=$(grep "^distributionUrl=" "$MAVEN_WRAPPER_PROPERTIES" | cut -d= -f2-)

# Derive distribution name and install dir
DIST_NAME=$(basename "$DISTRIBUTION_URL" .zip)
WRAPPER_DIR="$HOME/.m2/wrapper/dists/$DIST_NAME"

# Download & unpack if not already present
if [ ! -d "$WRAPPER_DIR" ] || [ -z "$(ls -A "$WRAPPER_DIR" 2>/dev/null)" ]; then
  echo "Downloading Maven: $DISTRIBUTION_URL"
  mkdir -p "$WRAPPER_DIR"
  DIST_ZIP="$WRAPPER_DIR/$DIST_NAME.zip"
  if command -v curl > /dev/null; then
    curl -fL "$DISTRIBUTION_URL" -o "$DIST_ZIP"
  elif command -v wget > /dev/null; then
    wget -q "$DISTRIBUTION_URL" -O "$DIST_ZIP"
  else
    echo "ERROR: Neither curl nor wget found. Install Maven manually." >&2
    exit 1
  fi
  echo "Extracting..."
  unzip -q "$DIST_ZIP" -d "$WRAPPER_DIR"
  rm -f "$DIST_ZIP"
fi

# Find the actual extracted directory (may differ from zip name)
MAVEN_HOME=$(find "$WRAPPER_DIR" -mindepth 1 -maxdepth 1 -type d | head -1)
if [ -z "$MAVEN_HOME" ]; then
  echo "ERROR: Could not find extracted Maven directory in $WRAPPER_DIR" >&2
  exit 1
fi
MVNEXEC="$MAVEN_HOME/bin/mvn"
chmod +x "$MVNEXEC"
echo "Maven ready at $MAVEN_HOME"

exec "$MVNEXEC" \
  -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" \
  "$@"
