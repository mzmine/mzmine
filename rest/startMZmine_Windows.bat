@echo off

rem The HEAP_SIZE variable line defines the Java heap size in MB. 
rem That is the total amount of memory available to MZmine 2.
rem Please adjust according to the amount of memory of your computer.
rem Maximum value on a 32-bit Windows system is about 1300. 
set HEAP_SIZE=1024

rem The TMP_FILE_DIRECTORY parameter defines the location where temporary 
rem files (parsed raw data) will be placed. Default is %TEMP%, which 
rem represents the system temporary directory.
set TMP_FILE_DIRECTORY=%TEMP%

rem It is usually not necessary to modify the JAVA_COMMAND parameter, but 
rem if you like to run a specific Java Virtual Machine, you may set the 
rem path to the java command of that JVM
set JAVA_COMMAND=java

rem It is not necessary to modify the following section
set LOGGING_CONFIG_FILE=conf/logging.properties
set JAVA_PARAMETERS=-XX:+UseParallelGC -Djava.io.tmpdir=%TMP_FILE_DIRECTORY% -Djava.util.logging.config.file=%LOGGING_CONFIG_FILE% -Xms%HEAP_SIZE%m -Xmx%HEAP_SIZE%m
set CLASS_PATH=MZmine2.jar
set MAIN_CLASS=net.sf.mzmine.main.mzmineclient.MZmineClient 

rem This command starts the Java Virtual Machine
%JAVA_COMMAND% %JAVA_PARAMETERS% -classpath %CLASS_PATH% %MAIN_CLASS%  
