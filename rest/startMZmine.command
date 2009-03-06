#!/bin/sh


# The HEAP_SIZE variable line defines the Java heap size in MB. 
# That is the total amount of memory available to MZmine 2.
# Please adjust according to the amount of memory of your computer.
# Maximum value on a 32-bit Mac OS X system is about 2000. 
HEAP_SIZE=1024

# If you have a 64-bit CPU, 64-bit OS and 64-bit JVM installed, you 
# can run MZmine 2 in 64-bit mode and increase the HEAP_SIZE above 
# the limitations of 32-bit platform. In that case, please set the 
# value of USE_64_BIT parameter to "-d64" (without quotes).
USE_64_BIT=

# The TMP_FILE_DIRECTORY parameter defines the location where temporary 
# files (parsed raw data) will be placed. Default is /tmp.
TMP_FILE_DIRECTORY=/tmp

# It is usually not necessary to modify the JAVA_COMMAND parameter, but 
# if you like to run a specific Java Virtual Machine, you may set the 
# path to the java command of that JVM.
JAVA_COMMAND=java

# It is not necessary to modify the following section
LOGGING_CONFIG_FILE=conf/logging.properties
JAVA_PARAMETERS="-Xdock:name='MZmine 2' -Xdock:icon=icons/MZmineIcon.png -Dapple.laf.useScreenMenuBar=true -Djava.util.logging.config.file=$LOGGING_CONFIG_FILE -Xms${HEAP_SIZE}m -Xmx${HEAP_SIZE}m"
CLASS_PATH=MZmine2.jar
MAIN_CLASS=net.sf.mzmine.main.mzmineclient.MZmineClient 

# Make sure we are in the correct directory
SCRIPTDIR=`dirname "$0"`
cd "$SCRIPTDIR"

# This command starts the Java Virtual Machine
echo "$JAVA_PARAMETERS" -classpath $CLASS_PATH $MAIN_CLASS | xargs $JAVA_COMMAND

