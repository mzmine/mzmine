// -*- mode: c++ -*-


/*
	Dynamic Library: ThermoRawFileReader.dll 
	Description: read Thermo/Xcalibur native RAW mass-spec data files,
	and bind java objects, methods and fileds of MZmine application.
	Please note, this dll requires the XRawfile library from 
	ThermoFinnigan to run.

	Date: May 2008
	Author: Alejandro Villar Briones, OIST Okinawa Japan, 2008, based
	on original work by Joshua Tasman, Pedrioli Patrick, ISB, Proteomics
	(original author), and Brian Pratt, InSilicos

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
#include "net_sf_mzmine_modules_io_rawdataimport_fileformats_XcaliburRawFileReadTask.h"
#include "FilterLine.h"


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
	int msControllerType = 0;
	jclass cls=env->GetObjectClass(jobj);


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

	jstring jstr = env->NewStringUTF("Initialize dll");
	env->ExceptionClear();
	env->CallVoidMethod(log, mid_finest, jstr);
	if(env->ExceptionOccurred()) {
		printf("error occured when calling logger.info()\n");
		env->ExceptionDescribe();
		env->ExceptionClear();
		return (jint) 1;
	}



	XRAWFILE2Lib::IXRawfile3Ptr xrawfile2_ = NULL;

	CoInitialize( NULL );	

	HRESULT hr = xrawfile2_.CreateInstance("XRawfile.XRawfile.1");

	if (!FAILED(hr)) {
		jstr = env->NewStringUTF("Xcalibur 2.0 interface initialized.");
		env->CallVoidMethod(log, mid_finest, jstr);
	}
	else {
		jstr = (jstring) "Unable to initialize Xcalibur 2.0 interface";
 	    env->CallVoidMethod(log, mid_severe, jstr);
		jstr = (jstring) "try running the command regsvr32 C:\\<path_to_Xcalibur_dll>\\XRawfile2.dll";
		env->CallVoidMethod(log, mid_severe, jstr);
		return (jint) 1;
	}
	
	file = env->GetStringUTFChars(fileName, iscopy);
	_bstr_t name = _bstr_t(file);

	hr = xrawfile2_->Open(name);

	if (hr != ERROR_SUCCESS) {
		jstr = env->NewStringUTF("Unable to open XCalibur RAW file");
		env->CallVoidMethod(log, mid_severe, jstr);
		return (jint) 1;
	}

	xrawfile2_->SetCurrentController(msControllerType, 1);

	hr = xrawfile2_->GetFirstSpectrumNumber(&firstScanNumber);
	
	if (hr != ERROR_SUCCESS) {
		jstr = env->NewStringUTF("Unable to get first scan");
		env->CallVoidMethod(log, mid_severe, jstr);
		return (jint) 1;
	}
	xrawfile2_->GetLastSpectrumNumber(&lastScanNumber);
	totalNumScans = (lastScanNumber - firstScanNumber) + 1;


	jfieldID fid_scanNumber = env->GetFieldID(cls, "scanNumber","I");
	jfieldID fid_msLevel = env->GetFieldID(cls, "msLevel","I");
	jfieldID fid_retentionTime = env->GetFieldID(cls, "retentionTime","F");
	jfieldID fid_mz = env->GetFieldID(cls, "mz","[F");
	jfieldID fid_intensity = env->GetFieldID(cls, "intensity","[F");
	jfieldID fid_precursorMz = env->GetFieldID(cls, "precursorMz","F");
	jfieldID fid_precursorCharge = env->GetFieldID(cls, "precursorCharge","I");
	jfieldID fid_peaksCount = env->GetFieldID(cls, "peaksCount","I");
	jfieldID fid_totalScans = env->GetFieldID(cls, "totalScans","I");

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

	for (curScanNum=1; curScanNum <= totalNumScans; curScanNum++) {
	
		std::string thermoFilterLine = "";
		BSTR bstrFilter = NULL;
		xrawfile2_->GetFilterForScanNum(curScanNum, &bstrFilter);

		if (bstrFilter == NULL){
			jstr = env->NewStringUTF("bstrFilter is null");
			env->CallVoidMethod(log, mid_severe, jstr);
			return (jint) 1;
		}

		thermoFilterLine = convertBstrToString(bstrFilter);

		FilterLine filterLine;
		if (!filterLine.parse(thermoFilterLine)) {
			jstr = env->NewStringUTF("error parsing filter line. exiting.");
			env->CallVoidMethod(log, mid_severe, jstr);
			return (jint) 1;
		}

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
		float precursorMz = 0;
		long precursorCharge = 0;

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
					precursorMz = varValue.fltVal;
				}else if( varValue.vt == VT_R8 ) {
					precursorMz = (float) varValue.dblVal;
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
					precursorMz = (float) filterLine.cidParentMass_[filterLine.cidParentMass_.size() - 1];
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
		long dataPoints = 0;
		long scanNum = curScanNum;
		LPCTSTR szFilter = NULL;		// No filter
		long intensityCutoffType = 0;		// No cutoff
		long intensityCutoffValue = 0;	// No cutoff
		long maxNumberOfPeaks = 0;		// 0 : return all data peaks
		double centroidPeakWidth = 0;		// No centroiding
		float* mzArray_;
		float* intensityArray_;
		bool centroidThisScan = false;

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
		mzArray_ = new float[dataPoints];
		intensityArray_ = new float[dataPoints];

		// Get a pointer to the SafeArray
		SAFEARRAY FAR* psa = varMassList.parray;
		DataPeak* pDataPeaks = NULL;
		SafeArrayAccessData(psa, (void**)(&pDataPeaks));
		
		// record mass list information in scan object
		for (long j=0; j<dataPoints; j++) {
			mzArray_[j] = (float) pDataPeaks[j].dMass;
			intensityArray_[j] = (float) pDataPeaks[j].dIntensity;
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
		jint vjint = (jint) curScanNum;
		env->SetIntField(jobj, fid_scanNumber, vjint);
		vjint = (jint) filterLine.msLevel_;
		env->SetIntField(jobj, fid_msLevel, vjint);
		vjint = (jint) dataPoints;
		env->SetIntField(jobj, fid_peaksCount, vjint);
		vjint = (jint) precursorCharge;
		env->SetIntField(jobj, fid_precursorCharge, vjint);

		jfloat vjfloat = (jfloat) retentionTimeInSeconds;
		env->SetFloatField(jobj, fid_retentionTime, vjfloat);
		vjfloat = (jfloat) precursorMz;
		env->SetFloatField(jobj, fid_precursorMz, vjfloat);

		jfloatArray vjfloararrayMs= env->NewFloatArray((jsize) dataPoints);
   		env->SetFloatArrayRegion(
			vjfloararrayMs, 0, dataPoints, (const jfloat *) mzArray_);
		env->SetObjectField(jobj, fid_mz, vjfloararrayMs);

		jfloatArray vjfloararrayIntensity= env->NewFloatArray((jsize) dataPoints);
   		env->SetFloatArrayRegion(
			vjfloararrayIntensity, 0, dataPoints, (const jfloat *) intensityArray_);
		env->SetObjectField(jobj, fid_intensity, vjfloararrayIntensity);

		env->ExceptionClear();
		env->CallVoidMethod(jobj, mid_startScan);
		if(env->ExceptionOccurred()) {
			jstr = env->NewStringUTF("error occured when calling startScan()");
			env->CallVoidMethod(log, mid_severe, jstr);
			env->ExceptionDescribe();
			env->ExceptionClear();
			return (jint) 1;
		}

		env->ExceptionClear();
		env->CallVoidMethod(jobj, mid_clean);
		if(env->ExceptionOccurred()) {
			jstr =env->NewStringUTF("error occured when calling clean()");
			env->CallVoidMethod(log, mid_severe, jstr);
			env->ExceptionDescribe();
			env->ExceptionClear();
			return (jint) 1;
		}
		delete mzArray_;
		delete intensityArray_;
		
	}

	hr = xrawfile2_->Close();

	if (hr != ERROR_SUCCESS) {
		jstr = env->NewStringUTF("Catastrofic error trying to close file");
		env->CallVoidMethod(log, mid_severe, jstr);
	}

	CoUninitialize();	
	return (jint) 0;

}


