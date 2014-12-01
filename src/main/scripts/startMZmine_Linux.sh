#!/bin/sh

# The HEAP_SIZE variable defines the Java heap size in MB. 
# That is the total amount of memory available to MZmine 2.
# By default we set this to the half of the physical memory 
# size, but feel free to adjust according to your needs. 

echo "Checking physical memory size..."
TOTAL_MEMORY=`free -b | awk '/Mem:/ { print int($2 / 1024^2) }'`
echo "Found $TOTAL_MEMORY MB memory"

HEAP_SIZE=`expr $TOTAL_MEMORY / 2`
echo Java heap size set to $HEAP_SIZE MB

# The TMP_FILE_DIRECTORY parameter defines the location where temporary 
# files (parsed raw data) will be placed. Default is /tmp.
TMP_FILE_DIRECTORY=/tmp

# Set R environment variables.
export R_HOME=/usr/lib64/R
export R_SHARE_DIR=/usr/share/R/share 
export R_INCLUDE_DIR=/usr/share/R/include
export R_DOC_DIR=/usr/share/R/doc
export R_LIBS_USER=${HOME}/R/x86_64-pc-linux-gnu-library/2.10

# Include R shared libraries in LD_LIBRARY_PATH.
export LD_LIBRARY_PATH=${R_HOME}/lib:${R_HOME}/bin

# The directory holding the JRI shared library (libjri.so).
JRI_LIB_PATH=${R_LIBS_USER}/rJava/jri

# It is usually not necessary to modify the JAVA_COMMAND parameter, but 
# if you like to run a specific Java Virtual Machine, you may set the 
# path to the java command of that JVM.
JAVA_COMMAND=java

# It is not necessary to modify the following section
JAVA_PARAMETERS="-showversion -classpath lib/\* -XX:+UseParallelGC -Djava.io.tmpdir=$TMP_FILE_DIRECTORY -Xms${HEAP_SIZE}m -Xmx${HEAP_SIZE}m -Djava.library.path=${JRI_LIB_PATH}"
MAIN_CLASS=net.sf.mzmine.main.MZmineCore

# Make sure we are in the correct directory
SCRIPTDIR=`dirname "$0"`
cd "$SCRIPTDIR"

# This command starts the Java Virtual Machine
echo "$JAVA_PARAMETERS" -classpath $MAIN_CLASS "$@" | xargs $JAVA_COMMAND