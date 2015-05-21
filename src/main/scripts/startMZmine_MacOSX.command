#!/bin/sh

# The HEAP_SIZE variable defines the Java heap size in MB. 
# That is the total amount of memory available to MZmine 2.
# By default we set this to the half of the physical memory 
# size, but feel free to adjust according to your needs. 

echo "Checking physical memory size..."
TOTAL_MEMORY=`sysctl hw.memsize | awk '{ print int($2 / 1024^2) }'`
echo "Found $TOTAL_MEMORY MB memory"

if [ "$TOTAL_MEMORY" -gt 4096 ]; then
	HEAP_SIZE=`expr $TOTAL_MEMORY - 2048`
else
	HEAP_SIZE=`expr $TOTAL_MEMORY / 2`
fi
echo Java heap size set to $HEAP_SIZE MB

# The TMP_FILE_DIRECTORY parameter defines the location where temporary 
# files (parsed raw data) will be placed. Default is /tmp.
TMP_FILE_DIRECTORY=/tmp

# Set R environment variables.
export R_HOME=/Library/Frameworks/R.framework/Versions/Current/Resources/

# The directory holding the JRI shared library (libjri.jnilib).
JRI_LIB_PATH=${R_HOME}/library/rJava/jri

# It is usually not necessary to modify the JAVA_COMMAND parameter, but 
# if you like to run a specific Java Virtual Machine, you may set the 
# path to the java command of that JVM.
JAVA_COMMAND=`/usr/libexec/java_home -v 1.6+`/bin/java

# It is not necessary to modify the following section
JAVA_PARAMETERS="-showversion -classpath lib/\* -Djava.ext.dirs= -XX:+UseParallelGC -Xdock:name='MZmine 2' -Xdock:icon=icons/MZmineIcon.png -Djava.io.tmpdir=$TMP_FILE_DIRECTORY -Dapple.laf.useScreenMenuBar=true -Xms${HEAP_SIZE}m -Xmx${HEAP_SIZE}m -Djava.library.path=${JRI_LIB_PATH}"
MAIN_CLASS=net.sf.mzmine.main.MZmineCore 

# Make sure we are in the correct directory
SCRIPTDIR=`dirname "$0"`
cd "$SCRIPTDIR"

# This command starts the Java Virtual Machine
echo "$JAVA_PARAMETERS" $MAIN_CLASS "$@" | xargs $JAVA_COMMAND
