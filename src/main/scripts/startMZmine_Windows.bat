@echo off

:: *****************************************
:: Optional values - Please modify as needed
:: *****************************************

:: Total amount of memory in MB available to MZmine 2.
:: AUTO = automatically determined
:: Default: AUTO
set HEAP_SIZE=AUTO

:: Location where temporary files will be stored.
:: Default: %TEMP%
set TMP_FILE_DIRECTORY=%TEMP%

:: It is usually not necessary to modify the JAVA_COMMAND parameter, but if you like to run
:: a specific Java Virtual Machine, you may set the path to the java.exe command of that JVM
set JAVA_COMMAND=java.exe

:: ********************************************
:: You don't need to modify anything below here
:: ********************************************



:: ***********************************
:: Auto detection of accessible memory
:: ***********************************

:: This is necessary to access the TOTAL_MEMORY and ADDRESS_WIDTH variables inside the IF block
setlocal enabledelayedexpansion

:: Obtain the physical memory size and check if we are running on a 32-bit system.
if exist C:\Windows\System32\wbem\wmic.exe (
  echo Checking physical memory size...
  :: Get physical memory size from OS
  for /f "skip=1" %%p in ('C:\Windows\System32\wbem\wmic.exe os get totalvisiblememorysize') do if not defined TOTAL_MEMORY set /a TOTAL_MEMORY=%%p / 1024
  for /f "skip=1" %%x in ('C:\Windows\System32\wbem\wmic.exe cpu get addresswidth') do if not defined ADDRESS_WIDTH set ADDRESS_WIDTH=%%x
  echo Found !TOTAL_MEMORY! MB memory, !ADDRESS_WIDTH!-bit system
) else (
  echo Skipping memory size check, because wmic.exe could not be found
  set ADDRESS_WIDTH=32
)


:: Find Java version
Set JAVA_BIT_VERSION=32
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do ( @set JAVA_VERSION=%%~g )
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "64-Bit"') do (@set JAVA_BIT_VERSION=64)
echo Java version is %JAVA_VERSION%(%JAVA_BIT_VERSION%-Bit)


:: Prompt user with error if 32-Bit Java is found on 64-Bit Windows
if %ADDRESS_WIDTH%==64 if %JAVA_BIT_VERSION%==32 (
	echo.
	echo *******************************************************************************
	echo *******************************************************************************
	echo.
	echo Warning: You have installed a 32-Bit version of Java on a 64-Bit Windows.
	echo We suggest that you uninstall all Java versions and install the latest 64-Bit
	echo version instead. If you want to continue running MZmine, the software will only
	echo be able to access 1GB of memory.
	echo.
	echo Please press any button to continue.
	echo.
	echo *******************************************************************************
	echo *******************************************************************************
	echo.
	pause > tempFile
	del tempFile
	set HEAP_SIZE=1024
)


:: By default we set the maximum HEAP_SIZE to 1024 MB on 32-bit systems. On 64-bit systems we 
:: either set it to half of the total memory or 2048 MB less than the total memory.
if %HEAP_SIZE%==AUTO (
  if %ADDRESS_WIDTH%==32 (
    set HEAP_SIZE=1024
  ) else (
    if %TOTAL_MEMORY% gtr 4096 (
	  set /a HEAP_SIZE=%TOTAL_MEMORY%-2048
    ) else (
	  set /a HEAP_SIZE=%TOTAL_MEMORY% / 2
    )
  )
)
echo Java maximum heap size set to %HEAP_SIZE% MB
echo.


:: *************************************************
:: Make a unique temp folder for the MZmine instance
:: *************************************************

:: DOS make sure to use 'short name' representation for temp directory
:: and forward slashed (required by features not supporting spaces or '\').
for %%f in ("%TMP_FILE_DIRECTORY%") do (set TMP_FILE_DIRECTORY=%%~sf)
for /F "usebackq tokens=1,2 delims==" %%i in (`wmic os get LocalDateTime /VALUE 2^>NUL`) do if '.%%i.'=='.LocalDateTime.' set ldt=%%j
set ldt=%ldt:~0,4%-%ldt:~4,2%-%ldt:~6,2%_%ldt:~8,2%-%ldt:~10,2%-%ldt:~12,6%
set MZMINE_UNID=MZmine%ldt%

set TMP_FILE_DIRECTORY=%TMP_FILE_DIRECTORY%\\%MZMINE_UNID%
mkdir %TMP_FILE_DIRECTORY%



:: **********************
:: Java specific commands
:: **********************

set JAVA_PARAMETERS=-showversion -classpath lib\* -Djava.ext.dirs= -XX:+UseG1GC -Djava.io.tmpdir=%TMP_FILE_DIRECTORY% -Xms1024m -Xmx%HEAP_SIZE%m
set MAIN_CLASS=net.sf.mzmine.main.MZmineCore

:: Make sure we are in the correct directory
set SCRIPTDIR=%~dp0
cd %SCRIPTDIR%

:: Starts the Java Virtual Machine
%JAVA_COMMAND% %JAVA_PARAMETERS% %MAIN_CLASS% %*

:: If there was an error, give the user a chance to see it
IF ERRORLEVEL 1 pause



:: *****************
:: Clean-up commands
:: *****************

setlocal disableDelayedExpansion

:: Kill/clean-up remaining Rserve instances
:: Load the file path "array"
for /f "tokens=1* delims=:" %%A in ('dir /b %TMP_FILE_DIRECTORY%\rs_pid_*.pid 2^>nul ^|findstr /n "^"') do (
  set "file.%%A=%TMP_FILE_DIRECTORY%\%%B"
  set "file.count=%%A"
)
:: Access the values
setlocal enableDelayedExpansion
for /l %%N in (1 1 %file.count%) do (
  echo Found pidfile: !file.%%N!
  set /P pid=<!file.%%N!
  echo Killing Rserve tree from main instance / pid: !pid!.
  del !file.%%N!
  taskkill /PID !pid! /F 2>nul
)

:: Delete temporary folder
RD /S /Q %TMP_FILE_DIRECTORY%
