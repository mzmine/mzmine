// -*- mode: c++ -*-


/*
	Dynamic Library: ThermoRawFileReader.dll 
	Description: read Thermo/Xcalibur native RAW mass-spec data files,
	and bind java objects, methods and fileds of MZmine application.
	Please note, this dll requires the XRawfile library from 
	ThermoFinnigan to run.

	This file is part of MZmine 2.
  
	MZmine 2 is free software; you can redistribute it and/or modify it under the
	terms of the GNU General Public License as published by the Free Software
	Foundation; either version 2 of the License, or (at your option) any later
	version.
  
	MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
	WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
	A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 
	You should have received a copy of the GNU General Public License along with
	MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
	St, Fifth Floor, Boston, MA 02110-1301 USA

*/


// ThermoRawFileReader.cpp : Defines the exported functions for the DLL application.
//
#include "stdafx.h"
#include <jni.h>
#include <stdio.h>
#include <conio.h>
#include <iostream>
#include <string>
#include <windows.h>

//Verify the right path to your Thermo library
#import "C:\Xcalibur\System\Programs\XrawFile2.dll" named_guids

typedef struct _datapeak
{
	double dMass;
	double dIntensity;
} DataPeak;


/*
 * Class:     net_sf_mzmine_modules_io_rawdataimport_fileformats_XcaliburRawFileReadTask
 * Method:    openFile
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT jint JNICALL Java_net_sf_mzmine_modules_io_rawdataimport_fileformats_XcaliburRawFileReadTask_openFile
(JNIEnv *env, jobject jobj, jstring fileName){

	const char *file;
	jboolean *iscopy = false;
  
	int i = 0;
	long totalNumScans = 0;
	long firstScanNumber = 0;
	long lastScanNumber = 0;
	long curScanNum = 1;
	int msControllerType = 0; // 0 == mass spec device
	jclass cls=env->GetObjectClass(jobj);

	// Initialize java logger object, with finnext and severe levels
	jfieldID fid_logger = env->GetFieldID(cls, "logger","Ljava/util/logging/Logger;");
	jobject log = env->GetObjectField(jobj, fid_logger);
	jclass cls_logger=env->GetObjectClass(log);
	jmethodID mid_finest = env->GetMethodID(cls_logger, 
		"finest", 
		"(Ljava/lang/String;)V");
	if (mid_finest == 0) {
		printf("Can't find method logger.finest\n");
		return (jint) 1;
	}

	jmethodID mid_severe = env->GetMethodID(cls_logger, 
		"severe", 
		"(Ljava/lang/String;)V");
	if (mid_severe == 0) {
		printf("Can't find method logger.finest\n");
		return (jint) 1;
	}

	// Send status messages to MzMine application
	jstring jstr = env->NewStringUTF("Initialize dll");
	env->ExceptionClear();
	env->CallVoidMethod(log, mid_finest, jstr);
	if(env->ExceptionOccurred()) {
		printf("error occured when calling logger.info()\n");
		env->ExceptionDescribe();
		env->ExceptionClear();
		return (jint) 1;
	}

	// Make an instance from XRawfile class defined in XRawFile2.dll.
	XRAWFILE2Lib::IXRawfile3Ptr xrawfile2_ = NULL;

	CoInitialize( NULL );	

	HRESULT hr = xrawfile2_.CreateInstance("XRawfile.XRawfile.1");
	
	// Send status of initialization to MzMine application
	if (!FAILED(hr)) {
		jstr = env->NewStringUTF("Xcalibur 2.0 interface initialized.");
		env->CallVoidMethod(log, mid_finest, jstr);
	}
	else {
		jstr = env->NewStringUTF("Unable to initialize Xcalibur 2.0 interface, try running the command regsvr32 C:\\<path_to_Xcalibur_dll>\\XRawfile2.dll");
		env->CallVoidMethod(log, mid_severe, jstr);
		CoUninitialize();	
		return (jint) 1;
	}
	
	// Open the Thermo raw file
	file = env->GetStringUTFChars(fileName, iscopy);
	_bstr_t name = _bstr_t(file);

	hr = xrawfile2_->Open(name);

	if (hr != ERROR_SUCCESS) {
		jstr = env->NewStringUTF("Unable to open XCalibur RAW file");
		env->CallVoidMethod(log, mid_severe, jstr);
		return (jint) 1;
	}

	// Look for data that belong to the first mass spectra device in the file
	xrawfile2_->SetCurrentController(msControllerType, 1);
	
	// Verifies if can get the first scan
	hr = xrawfile2_->GetFirstSpectrumNumber(&firstScanNumber);
	
	if (hr != ERROR_SUCCESS) {
		jstr = env->NewStringUTF("Unable to get first scan");
		env->CallVoidMethod(log, mid_severe, jstr);
		return (jint) 1;
	}

	// Ask for the last scan number to prepare memory space, for cycle 
	// and final verification
	xrawfile2_->GetLastSpectrumNumber(&lastScanNumber);
	totalNumScans = (lastScanNumber - firstScanNumber) + 1;

	// Affected java fileds in Mzmine application
	jfieldID fid_scanNumber = env->GetFieldID(cls, "scanNumber","I");
	jfieldID fid_msLevel = env->GetFieldID(cls, "msLevel","I");
	jfieldID fid_retentionTime = env->GetFieldID(cls, "retentionTime","D");
	jfieldID fid_mz = env->GetFieldID(cls, "mz","[D");
	jfieldID fid_intensity = env->GetFieldID(cls, "intensity","[D");
	jfieldID fid_precursorMz = env->GetFieldID(cls, "precursorMz","D");
	jfieldID fid_precursorCharge = env->GetFieldID(cls, "precursorCharge","I");
	jfieldID fid_peaksCount = env->GetFieldID(cls, "peaksCount","I");
	jfieldID fid_totalScans = env->GetFieldID(cls, "totalScans","I");

	// Call java method "startScan" and "clean" from module 
	// net.sf.mzmine.modules.io.rawdataimport.fileformats.XcaliburRawFileReadTask
	// in MzMine application
	jmethodID mid_startScan = env->GetMethodID(cls, 
		"startScan", 
		"()V");
	if (mid_startScan == 0) {
		jstr = env->NewStringUTF("Can't find method startScan");
		env->CallVoidMethod(log, mid_severe, jstr);
		return (jint) 1;
	}

	jmethodID mid_clean = env->GetMethodID(cls, "clean","()V");
	if (mid_clean == 0) {
		jstr = env->NewStringUTF("Can't find method clean");
		env->CallVoidMethod(log, mid_severe, jstr);
		return (jint) 1;
	}

	jint total = (jint) totalNumScans;
	env->SetIntField(jobj, fid_totalScans, total);

	// Local variables
	long numDataPoints = -1; // points in both the m/z and intensity arrays
	double retentionTimeInMinutes = -1;
	double retentionTimeInSeconds = -1;
	double minObservedMZ_ = -1;
	double maxObservedMZ_ = -1;
	double totalIonCurrent_ = -1;
	double basePeakMZ_ = -1;
	double basePeakIntensity_ = -1;
	long channel; // unused
	long uniformTime; // unused
	double frequency; // unused
	double precursorMz = 0;
	long precursorCharge = 0;

	// set up the parameters to read the scan
	long dataPoints = 0;
	long scanNum = curScanNum;
	LPCTSTR szFilter = NULL;		// No filter
	long intensityCutoffType = 0;		// No cutoff
	long intensityCutoffValue = 0;	// No cutoff
	long maxNumberOfPeaks = 0;		// 0 : return all data peaks
	double centroidPeakWidth = 0;		// No centroiding
	double* mzArray_;
	double* intensityArray_;
	bool centroidThisScan = false;

	// Parser to verify data's integrity
	FilterLine filterLine;

	// Java variables
	jint vjint;
	jdouble vjdouble;
	jdoubleArray vjdoublearrayMz;
	jdoubleArray vjdoublearrayIntensity;

	std::string thermoFilterLine = "";
	BSTR bstrFilter = NULL;


	// Cycle to read totalnumber of scans, passing values to MzMine application
	for (curScanNum=1; curScanNum <= totalNumScans; curScanNum++) {

		thermoFilterLine = "";
		bstrFilter = NULL;
		xrawfile2_->GetFilterForScanNum(curScanNum, &bstrFilter);

		if (bstrFilter == NULL){
			jstr = env->NewStringUTF("bstrFilter is null");
			env->CallVoidMethod(log, mid_severe, jstr);
			return (jint) 1;
		}

		thermoFilterLine = convertBstrToString(bstrFilter);
		
		// Verifies data's integrity
		if (!filterLine.parse(thermoFilterLine)) {
			jstr = env->NewStringUTF("error parsing filter line. exiting.");
			env->CallVoidMethod(log, mid_severe, jstr);
			return (jint) 1;
		}

		//Clean varaibles
		numDataPoints = -1; // points in both the m/z and intensity arrays
		retentionTimeInMinutes = -1;
		retentionTimeInSeconds = -1;
		minObservedMZ_ = -1;
		maxObservedMZ_ = -1;
		totalIonCurrent_ = -1;
		basePeakMZ_ = -1;
		basePeakIntensity_ = -1;
		channel; // unused
		uniformTime; // unused
		frequency; // unused
		precursorMz = 0;
		precursorCharge = 0;

		xrawfile2_->GetScanHeaderInfoForScanNum(
			curScanNum, 
			&numDataPoints, 
			&retentionTimeInMinutes, 
			&minObservedMZ_,
			&maxObservedMZ_,
			&totalIonCurrent_,
			&basePeakMZ_,
			&basePeakIntensity_,
			&channel, // unused
			&uniformTime, // unused
			&frequency // unused
		);

		retentionTimeInSeconds = retentionTimeInMinutes * 60;

		if (filterLine.msLevel_ > 1){
				
				/*
				* precursorMz
				*/
				VARIANT varValue;
				VariantInit(&varValue);
				xrawfile2_->GetTrailerExtraValueForScanNum(curScanNum, "Monoisotopic M/Z:" , &varValue);
				
				if( varValue.vt == VT_R4 ){ 
					precursorMz = (double) varValue.fltVal;
				}else if( varValue.vt == VT_R8 ) {
					precursorMz = varValue.dblVal;
				}else if ( varValue.vt != VT_ERROR ) {
					char buffer [33];
					_itoa_s ((int)curScanNum, buffer,10);
					char * message = "Scan: ";
					strcat_s(message, 40, buffer);
					strcat_s(message, 40, " MS level: ");
					_itoa_s (filterLine.msLevel_, buffer,10);
					strcat_s(message, 40, buffer);
					strcat_s(message, 40, " unexpected type when looking for precursorMz");
					jstr = env->NewStringUTF(message);
					env->CallVoidMethod(log, mid_severe, jstr);
					precursorMz = 0;
				}
				if (precursorMz == 0) {
					// use the low-precision parent mass in the filter line
					precursorMz = filterLine.cidParentMass_[filterLine.cidParentMass_.size() - 1];
				}
				
				/*
				* precursorCharge
				*/
				VariantClear(&varValue);
				xrawfile2_->GetTrailerExtraValueForScanNum(curScanNum, "Charge State:" , &varValue);
				// First try to use the OCX
				if( varValue.vt == VT_I2 ) 
					precursorCharge = varValue.iVal;
				if (precursorCharge == 0) 
					precursorCharge = -1; // undetermined
				VariantClear(&varValue);
		}

		VARIANT varMassList;
		// initiallize variant to VT_EMPTY
		VariantInit(&varMassList);

		VARIANT varPeakFlags; // unused
		// initiallize variant to VT_EMPTY
		VariantInit(&varPeakFlags);

		// set up the parameters to read the scan
		dataPoints = 0;
		scanNum = curScanNum;
		szFilter = NULL;		// No filter
		intensityCutoffType = 0;		// No cutoff
		intensityCutoffValue = 0;	// No cutoff
		maxNumberOfPeaks = 0;		// 0 : return all data peaks
		centroidPeakWidth = 0;		// No centroiding
		centroidThisScan = false;

		xrawfile2_->GetMassListFromScanNum(
			&scanNum,
			szFilter,			 // filter
			intensityCutoffType, // intensityCutoffType
			intensityCutoffValue, // intensityCutoffValue
			maxNumberOfPeaks,	 // maxNumberOfPeaks
			centroidThisScan,		// centroid result?
			&centroidPeakWidth,	// centroidingPeakWidth
			&varMassList,		// massList
			&varPeakFlags,		// peakFlags
			&dataPoints);		// array size

		// record the number of data point (allocates memory for arrays)
		mzArray_ = new double[dataPoints];
		intensityArray_ = new double[dataPoints];

		// Get a pointer to the SafeArray
		SAFEARRAY FAR* psa = varMassList.parray;
		DataPeak* pDataPeaks = NULL;
		SafeArrayAccessData(psa, (void**)(&pDataPeaks));
		
		// record mass list information in scan object
		for (long j=0; j<dataPoints; j++) {
			mzArray_[j] = pDataPeaks[j].dMass;
			intensityArray_[j] = pDataPeaks[j].dIntensity;
		}

		// cleanup
		SafeArrayUnaccessData(psa); // Release the data handle
		VariantClear(&varMassList); // Delete all memory associated with the variant
		VariantClear(&varPeakFlags); // and reinitialize to VT_EMPTY

		if( varMassList.vt != VT_EMPTY ) {
			SAFEARRAY FAR* psa = varMassList.parray;
			varMassList.parray = NULL;
			SafeArrayDestroy( psa ); // Delete the SafeArray
		}

		if(varPeakFlags.vt != VT_EMPTY ) {
			SAFEARRAY FAR* psa = varPeakFlags.parray;
			varPeakFlags.parray = NULL;
			SafeArrayDestroy( psa ); // Delete the SafeArray
		}

		//Update values in java object for the current scan
		vjint = (jint) curScanNum;
		env->SetIntField(jobj, fid_scanNumber, vjint);
		vjint = (jint) filterLine.msLevel_;
		env->SetIntField(jobj, fid_msLevel, vjint);
		vjint = (jint) dataPoints;
		env->SetIntField(jobj, fid_peaksCount, vjint);
		vjint = (jint) precursorCharge;
		env->SetIntField(jobj, fid_precursorCharge, vjint);

		vjdouble = (jdouble) retentionTimeInSeconds;
		env->SetDoubleField(jobj, fid_retentionTime, vjdouble);
		vjdouble = (jdouble) precursorMz;
		env->SetDoubleField(jobj, fid_precursorMz, vjdouble);

		vjdoublearrayMz = env->NewDoubleArray((jsize) dataPoints);
   		env->SetDoubleArrayRegion(
			vjdoublearrayMz, 0, dataPoints, (const jdouble *) mzArray_);
		env->SetObjectField(jobj, fid_mz, vjdoublearrayMz);
		
		vjdoublearrayIntensity = env->NewDoubleArray((jsize) dataPoints);
   		env->SetDoubleArrayRegion(
			vjdoublearrayIntensity, 0, dataPoints, (const jdouble *) intensityArray_);
		env->SetObjectField(jobj, fid_intensity, vjdoublearrayIntensity);

		// Call java method "startScan"
		env->ExceptionClear();
		env->CallVoidMethod(jobj, mid_startScan);
		if(env->ExceptionOccurred()) {
			jstr = env->NewStringUTF("error occured when calling startScan()");
			env->CallVoidMethod(log, mid_severe, jstr);
			env->ExceptionDescribe();
			env->ExceptionClear();
			return (jint) 1;
		}

		// Call java method "clean"
		env->ExceptionClear();
		env->CallVoidMethod(jobj, mid_clean);
		if(env->ExceptionOccurred()) {
			jstr =env->NewStringUTF("error occured when calling clean()");
			env->CallVoidMethod(log, mid_severe, jstr);
			env->ExceptionDescribe();
			env->ExceptionClear();
			return (jint) 1;
		}

		// Cleanup memory
		env->DeleteLocalRef(vjdoublearrayMz);
		env->DeleteLocalRef(vjdoublearrayIntensity);
		delete[] mzArray_;
		mzArray_=NULL;
		delete[] intensityArray_;
		intensityArray_=NULL;
		
	}

	// Finalize link to XRawfile2.dll library
	hr = xrawfile2_->Close();

	if (hr != ERROR_SUCCESS) {
		jstr = env->NewStringUTF("Catastrofic error trying to close file");
		env->CallVoidMethod(log, mid_severe, jstr);
	}

	CoUninitialize();	
	return (jint) 0;

}


