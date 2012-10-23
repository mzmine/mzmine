#!/bin/sh

# The HEAP_SIZE variable defines the Java heap size in MB. 
# That is the total amount of memory available to MZmine 2.
# By default we set this to the half of the physical memory 
# size, but feel free to adjust according to your needs. 
HEAP_SIZE=`sysctl hw.memsize | awk '{print int($2 / 1024^2 / 2)}'`

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
JAVA_COMMAND=java

# It is not necessary to modify the following section
JAVA_PARAMETERS="-XX:+UseParallelGC -Xdock:name='MZmine 2' -Xdock:icon=icons/MZmineIcon.png -Djava.io.tmpdir=$TMP_FILE_DIRECTORY -Dapple.laf.useScreenMenuBar=true -Xms${HEAP_SIZE}m -Xmx${HEAP_SIZE}m -Djava.library.path=${JRI_LIB_PATH}"
CLASS_PATH=lib/${project.artifactId}-${project.version}.jar
MAIN_CLASS=net.sf.mzmine.main.MZmineCore 

# Make sure we are in the correct directory
SCRIPTDIR=`dirname "$0"`
cd "$SCRIPTDIR"

# Show java version, in case a problem occurs
echo "-version" | xargs $JAVA_COMMAND

# This command starts the Java Virtual Machine
echo "$JAVA_PARAMETERS" -classpath $CLASS_PATH $MAIN_CLASS "$@" | xargs $JAVA_COMMAND

