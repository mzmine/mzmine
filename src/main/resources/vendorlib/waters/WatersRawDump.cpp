/*
* Copyright 2006-2014 The MZmine 2 Development Team
*
* This file is part of MZmine 2.
*
* MZmine 2 is free software; you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License along with
* MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
* St, Fifth Floor, Boston, MA 02110-1301 USA
*
* ------------------------------------------------------------------------------
*
* This program binds to the MassLynxRaw.dll provided by the Waters and dumps the
* contents of a given RAW folder as text+binary data. To compile this source, you
* can use Microsoft Visual Studio 2013.
*
* Notes:
*
* 1) The library MassLynxRaw.dll comes from Waters MassLynx raw data library
* (32-bit version)
*
* 2) We use \n (UNIX-style end of line), which is what the MZmine import module
* expects. If we use the endl constant, it would add \r\n (Windows-style end of
* line).

NUMBER OF SCANS: int
SCAN NUMBER: int
SCAN ID: string
POLARITY: char
MS LEVEL: int
RETENTION TIME: double
MZ RANGE: double - double
PRECURSOR: double int
MASS VALUES: int x int BYTES
INTENSITY VALUES: int x int BYTES
END OF SCAN

notes:
RT is in minutes
polarity is + or - or ?
precursor corresponds to m/z and charge (0 if unknown)

*/

#include <vector>
#include <string>
#include <iostream>
#include <fstream>
#include <io.h>
#include <stdio.h>
#include <fcntl.h>
#include <sys/stat.h>

#include "include/MassLynxRawDataFile.h"
#include "include/MassLynxRawReader.h"
#include "include/MassLynxRawScanReader.h"
#include "include/MassLynxRawChromatogramReader.h"
#include "include/MassLynxRawInfo.h"
#include "include/MassLynxRawScanStatsReader.h"

using std::vector;
using std::string;
using std::cout;

using namespace std;
using namespace Waters::Lib::MassLynxRaw;

const int DEBUG = false;

int main(int argc, char* argv[])
{

	// Set locale to C, otherwise we might get decimal commas instead of decimal points at some locales
	setlocale(LC_ALL, "C");

	// Disable output buffering and set output to binary mode, otherwise cout would automatically translate \n to \r\n
	setvbuf(stdout, 0, _IONBF, 0);
	_setmode(_fileno(stdout), _O_BINARY);

	// Prepare vectors 
	vector<ExtendedStatsType> extStatsTypes;
	vector<double> precursorValues;
	vector<MSScanStats> stats;
	vector<float> masses;
	vector<float> intensities;

	try {

		if (argc != 2) {
			cout << "ERROR: This program accepts exactly 1 argument: a RAW file path\n";
			return 1;
		}

		string fileName = argv[1];

		// Test whether the file exists and is a directory
		struct stat st;
		if ((stat(fileName.c_str(), &st) != 0) || ((st.st_mode & S_IFDIR) == 0)) {
			cout << "ERROR: Unable to read RAW file " << fileName << "\n";
			return 1;
		}

		// Get a raw reader
		MassLynxRawReader RR(fileName);
		MassLynxRawInfo RI(RR);
		MassLynxRawScanReader RSR(RR);
		MassLynxRawScanStatsReader RSSR(RR);

		// Get number of functions in the file
		int nFuncs = RI.GetFunctionCount();

		if (DEBUG) {
			for (int func = 0; func < nFuncs; func++) {

				string funcTypeString = RI.GetFunctionTypeString(func);
				FunctionType funcType = RI.GetFunctionType(func);
				cout << "DEBUG: function " << func << " type " << funcType << " = " << funcTypeString << "\n";

				int scans = RI.GetScansInFunction(func);
				cout << "DEBUG: " << scans << " scans\n";

				RSSR.getExtendedStatsTypes(func, extStatsTypes);
				cout << "DEBUG: " << extStatsTypes.size() << " extended stats fields:\n";

				for (int i = 0; i < extStatsTypes.size(); ++i) {
					cout << (i + 1) << " " << extStatsTypes[i].name << " type " << extStatsTypes[i].typeCode << "\n";
				}

			}

		}

		// Calculate total number of scans
		long totalNumScans = 0;
		for (int func = 0; func < nFuncs; func++) {
			totalNumScans += RI.GetScansInFunction(func);
		}
		cout << "NUMBER OF SCANS: " << totalNumScans << "\n";

		int curScanNum = 1;

		for (int func = 0; func < nFuncs; func++) {

			const int nScans = RI.GetScansInFunction(func);

			// If there are no scans, skip this function (otherwise, RSSR.readScanStats() would thrown an exception)
			if (nScans == 0) continue;

			// Get details about scans
			RSSR.getExtendedStatsTypes(func, extStatsTypes);
			
			// Look for precursor fuction
			int havePrecursors = false;
			ExtendedStatsType *setMassType = NULL;
			for (int i = 0; i < extStatsTypes.size(); i++)
			{
				if (extStatsTypes[i].name == "Set Mass") {
					setMassType = &extStatsTypes[i];
					havePrecursors = true;
					break;
				}
			}

			// If there is precursor data, load it
			if (havePrecursors) {
				// For some reason, the getExtrendedStatsField function requires the vector to reserve the memory
				// in advance, otherwise the program will crash due to buffer overflow
				precursorValues.reserve(nScans);
				RSSR.getExtendedStatsField(func, *setMassType, precursorValues);
			}

			// Get type of function
			string funcTypeString = RI.GetFunctionTypeString(func);

			// Get MS level
			FunctionType funcType = RI.GetFunctionType(func);

			int msLevel;
			switch (funcType) {
			case FT_MSMS:
			case FT_TOFD:
			case FT_NL:
			case FT_NG:
			case FT_MS2:
			case FT_QUAD_AUTO_DAU:
			case FT_MRM:
			case FT_ASPEC_MRM:
			case FT_ASPEC_MRMQ:
			case FT_ASPEC_MIKES:
				msLevel = 2;
				break;
			default:
				msLevel = 1;
				break;
			}

			// Get polarity
			char polarity;
			IonMode ionMode = RI.GetIonMode(func);
			switch (ionMode) {
			case IM_EIP: case IM_CIP: case IM_FBP: case IM_TSP:
			case IM_ESP: case IM_AIP: case IM_LDP: case IM_FIP:
				polarity = '+';
				break;
			case IM_EIM: case IM_CIM: case IM_FBM: case IM_TSM:
			case IM_ESM: case IM_AIM: case IM_LDM: case IM_FIM:
				polarity = '-';
				break;
			default:
				polarity = '?';
				break;
			}

			DataType dataType = RI.GetDataType(func);
			float mzRangeLow, mzRangeHigh;
			RI.GetAcquisitionMassRange(func, mzRangeLow, mzRangeHigh);

			// Scan stats contain retention times and numbers of data points
			stats.reserve(nScans);
			RSSR.readScanStats(func, stats);

			for (int scanIndex = 0; scanIndex < nScans; scanIndex++) {

				cout << "START SCAN\n";

				cout << "SCAN NUMBER: " << curScanNum << "\n";
				curScanNum++;

				cout << "SCAN ID: " << funcTypeString << " function=" << func << " scan=" << scanIndex << "\n";

				cout << "POLARITY: " << polarity << "\n";

				cout << "MS LEVEL: " << msLevel << "\n";

				if (havePrecursors)
					cout << "PRECURSOR: " << precursorValues[scanIndex] << " 0\n";

				cout << "RETENTION TIME: " << stats[scanIndex].rt << "\n";

				cout << "MZ RANGE: " << mzRangeLow << " - " << mzRangeHigh << "\n";

				// Read the spectrum data
				masses.reserve(stats[scanIndex].peaksInScan);
				intensities.reserve(stats[scanIndex].peaksInScan);
				RSR.readSpectrum(func, scanIndex, masses, intensities);

				// Note: the size of masses & intensities array should be the same as stats[scanIndex].peaksInScan, 
				// but for some Waters files it is actually different (maybe the peaksInScan value is stored incorrectly?). 
				// It is essential to ignore the peaksInScan value and report the length using masses.size() 

				cout << "MASS VALUES: " << masses.size() << " x " << sizeof(masses[0]) << " BYTES\n";

				if (!DEBUG)
					fwrite(&masses[0], sizeof(masses[0]), masses.size(), stdout);


				cout << "INTENSITY VALUES: " << intensities.size() << " x " << sizeof(intensities[0]) << " BYTES\n";

				if (!DEBUG)
					fwrite(&intensities[0], sizeof(intensities[0]), masses.size(), stdout);

				cout << "END OF SCAN\n";

			}

		}

		return 0;

	}
	catch (MassLynxRawException &e) {
		cout << "ERROR: " << e.what() << "\n";
		return 1;
	}

	return 0;

}
