@echo off

rem This is necessary to access the TOTAL_MEMORY and ADDRESS_WIDTH variables inside the IF block
setlocal enabledelayedexpansion

rem Obtain the physical memory size and check if we are running on a 32-bit system.
if exist C:\Windows\System32\wbem\wmic.exe (
  echo Checking physical memory size...
  rem Get physical memory size from OS
  for /f "skip=1" %%p in ('C:\Windows\System32\wbem\wmic.exe os get totalvisiblememorysize') do if not defined TOTAL_MEMORY set /a TOTAL_MEMORY=%%p / 1024
  for /f "skip=1" %%x in ('C:\Windows\System32\wbem\wmic.exe cpu get addresswidth') do if not defined ADDRESS_WIDTH set ADDRESS_WIDTH=%%x
  echo Found !TOTAL_MEMORY! MB memory, !ADDRESS_WIDTH!-bit system
) else (
  echo Skipping memory size check, because wmic.exe could not be found
  set ADDRESS_WIDTH=32
)

rem The HEAP_SIZE variable defines the Java heap size in MB.
rem That is the total amount of memory available to MZmine 2.
rem By default we set this to 1024 MB on 32-bit systems. On 
rem 64-bit systems we either set it to half of the total
rem memory or 2048 MB less than the total memory.
rem Feel free to adjust the HEAP_SIZE according to your needs.
if %ADDRESS_WIDTH%==32 (
  set HEAP_SIZE=1024
) else (
  if %TOTAL_MEMORY% gtr 4096 (
	set /a HEAP_SIZE=%TOTAL_MEMORY%-2048
  ) else (
	set /a HEAP_SIZE=%TOTAL_MEMORY% / 2
  )
)
echo Java heap size set to %HEAP_SIZE% MB

rem The TMP_FILE_DIRECTORY parameter defines the location where temporary 
rem files (parsed raw data) will be placed. Default is %TEMP%, which 
rem represents the system temporary directory.
set TMP_FILE_DIRECTORY=%TEMP%
rem Do not modify:
rem DOS make sure to use 'short name' representation for temp directory
for %%f in ("%TMP_FILE_DIRECTORY%") do (set TMP_FILE_DIRECTORY=%%~sf)
rem rem and forward slashed (required by features not supporting spaces or '\').
rem set TMP_FILE_DIRECTORY=%TMP_FILE_DIRECTORY:\=/%
rem Make the temp working dir unique per MZmine instance.
rem for /f "skip=1" %%x in ('wmic os get localdatetime') do (set MZMINE_UNID="MZmine"%%x)
for /F "usebackq tokens=1,2 delims==" %%i in (`wmic os get LocalDateTime /VALUE 2^>NUL`) do if '.%%i.'=='.LocalDateTime.' set ldt=%%j
set ldt=%ldt:~0,4%-%ldt:~4,2%-%ldt:~6,2%_%ldt:~8,2%-%ldt:~10,2%-%ldt:~12,6%
set MZMINE_UNID=MZmine%ldt%

set TMP_FILE_DIRECTORY=%TMP_FILE_DIRECTORY%\\%MZMINE_UNID%
mkdir %TMP_FILE_DIRECTORY%

rem Set R environment variables.
set R_HOME=C:\Program Files\R\R-2.12.0

rem It is usually not necessary to modify the JAVA_COMMAND parameter, but 
rem if you like to run a specific Java Virtual Machine, you may set the 
rem path to the java command of that JVM
set JAVA_COMMAND=java

rem It is not necessary to modify the following section
set JAVA_PARAMETERS=-showversion -classpath lib\* -Djava.ext.dirs= -XX:+UseParallelGC -Djava.io.tmpdir=%TMP_FILE_DIRECTORY% -Xms%HEAP_SIZE%m -Xmx%HEAP_SIZE%m
set MAIN_CLASS=net.sf.mzmine.main.MZmineCore

rem This command starts the Java Virtual Machine
%JAVA_COMMAND% %JAVA_PARAMETERS% %MAIN_CLASS% %*

rem If there was an error, give the user chance to see it
IF ERRORLEVEL 1 pause


rem Do not modify:
rem Kill/cleanup remaining Rserve instances
setlocal disableDelayedExpansion
:: Load the file path "array"
for /f "tokens=1* delims=:" %%A in ('dir /b %TMP_FILE_DIRECTORY%\rs_pid_*.pid^|findstr /n "^"') do (
  set "file.%%A=%TMP_FILE_DIRECTORY%\%%B"
  set "file.count=%%A"
)
:: Access the values
setlocal enableDelayedExpansion
for /l %%N in (1 1 %file.count%) do (
  echo !file.%%N!
  set /P pid=<!file.%%N!
  echo !pid!
  del !file.%%N!
  taskkill /PID !pid! /F 2>nul
)
