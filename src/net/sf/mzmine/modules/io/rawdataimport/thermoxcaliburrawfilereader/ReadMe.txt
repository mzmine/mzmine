========================================================================
    DYNAMIC LINK LIBRARY : ThermoRawFileReader Project Overview
========================================================================

This file contains a summary of what you will find in each of the files that
make up your ThermoRawFileReader library. 

This library reads Thermo/Xcalibur native RAW mass-spec data files, and bind 
java objects, methods and fileds of MZmine application. Please note, this dll 
requires the XRawfile library from ThermoFinnigan.


ThermoRawFileReader.vcproj
    This is the main project file for VC++ projects generated using an Application Wizard.
    It contains information about the version of Visual C++ that generated the file, and
    information about the platforms, configurations, and project features selected with the
    Application Wizard. (Microsoft Visual C++ 2008)

ThermoRawFileReader.cpp
    This is the main DLL source file.This contains the native function openFile, declared
    in XcaliburRawFileReadTask module of MzMine. This core file uses the JNI library to 
    create java objects, which are going to be transfered to MzMine application. The sequence
    of the process is the next.
    
    - Initialize java logger object, with finnest and severe levels
    - Make an instance from XRawfile class defined in XRawFile2.dll*
    - Open the Thermo raw file and get information of the first mass spec device*
    - Retrive first and last scan number to prepare variables*
    - Start loop cycle to read all the scans from defined device
		- Verifies integrity of the data using FilterLine, MSTypes and MSUtilities modules
		 (code files from ReAdW software)**
		- Update data in MzMine application via updating value of java fields and calling some
		  methods in XcaliburRawFileReadTask module.
		- Cleanup memory***
	- Finalize
	
	*	Please check document "XRawfile OCX.doc" from ThermoFinnigan, for a detail description
		of methods and parameters of XRawFile2.dll library.
	**	These files are from source code of ReAdW version 3.5.1
	***	When using java array objects created using JNI, use JNIenv->DeleteLocalRef method to
		release the reserved memory space, otherwise if you use delete or free, you lose the 
		reference and the pointed memory space is unreachable.
		
net_sf_mzmine_modules_io_rawdataimport_fileformats_XcaliburRawFileReadTask.h
	This file contains the definition of the native method "openFile".
	
	Important: If you change the structure of the module in the MzMine, verify that the name of
			   this file correspond with the actual path of the java module. 

StdAfx.h, StdAfx.cpp
    These files are used to build a precompiled header (PCH) file named ThermoRawFileReader.pch
    and a precompiled types file named StdAfx.obj. If you require to include more references
	please do it in stdafx.h
	

/////////////////////////////////////////////////////////////////////////////
Other standard files:

Please check the next link ( http://tools.proteomecenter.org/wiki/index.php?title=Software:ReAdW )
for detail information about the next files (ReAdW version 3.5.1)

	FilterLine.cpp
	FilterLine.h
	MSTypes.cpp
	MSTypes.h
	MSUtilities.cpp
	MSUtilities.h

The use of this files is under the terms of the GNU Lesser General Public License as published 
by the Free Software Foundation.

/////////////////////////////////////////////////////////////////////////////
Other notes:

/////////////////////////////////////////////////////////////////////////////
