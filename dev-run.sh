#!/bin/bash
# Fast development run script for MZmine
# add "clean" after gradlew command for a clean build
echo "Starting MZmine in development mode (fast)..."
export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS --add-exports=javafx.base/com.sun.javafx.event=ALL-UNNAMED --add-exports=javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED"

if [ "$1" = "debug" ]; then
  export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
  echo "Debug mode: waiting for debugger on port 5005..."
fi

./gradlew :mzmine-community:run -x test -x checkLicense -x generateLicenseReport -x copyLicenseInformationToResources -x copyLicenseInformationToBuild --daemon