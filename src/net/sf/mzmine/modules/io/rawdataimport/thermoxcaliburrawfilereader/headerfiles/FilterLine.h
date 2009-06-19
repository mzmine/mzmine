// -*- mode: c++ -*-


/*
    File: FilterLine.h
    Description: parsing for Thermo/Xcalibur "filter line".
    Date: July 25, 2007

    Copyright (C) 2007 Joshua Tasman, ISB Seattle


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



#ifndef _INCLUDED_FILTERLINE_H_
#define _INCLUDED_FILTERLINE_H_

#include <string>
#include <vector>

#include "MSTypes.h"

class FilterLine {
public:

	enum TriBool {
		BOOL_UNDEF = -1,
		BOOL_FALSE = 0,
		BOOL_TRUE = 1,
	};

	
	MSAnalyzerType parseAnalyzer(const std::string& word);

	MSPolarityType parsePolarity(const std::string& word);

	MSScanDataType parseScanData(const std::string& word);

	MSIonizationType parseIonizationMode(const std::string & word);

	typedef enum {
		ACCURATEMASS_UNDEF = 0,
		NO_AM,		// NOTE: in filter as "!AM": accurate mass not active
		AM,			// accurate mass active 
		AMI,		// accurate mass with internal calibration
		AME,		// accurate mass with external calibration
	} AccurateMassType;
	AccurateMassType parseAccuracteMass(const std::string& word);

	MSScanType parseScanType(const std::string& word);

	MSActivationType parseActivation(const std::string& word);


	MSAnalyzerType analyzer_;
	MSPolarityType polarity_;
	MSScanDataType scanData_;
	MSIonizationType ionizationMode_;
	TriBool coronaOn_;
	TriBool photoIonizationOn_;
	TriBool sourceCIDOn_;
	TriBool detectorSet_;
	TriBool turboScanOn_;
	TriBool dependentActive_; // t: data-dependent active; f: non active
	TriBool widebandOn_; // wideband activation
	AccurateMassType accurateMass_;
	MSScanType scanType_;
	int msLevel_; // n, in MSn: >0
	MSActivationType activationMethod_;

	std::vector<double> cidParentMass_; // one entry per ms level for level >= 2
	std::vector<double> cidEnergy_; // relative units; one entry per ms level for level >= 2

	std::vector<double> scanRangeMin_;
	std::vector<double> scanRangeMax_;

	// for SRM scans only: instead of a scan range, we get q3 transition lists
	std::vector<double> transitionRangeMin_;
	std::vector<double> transitionRangeMax_;

	FilterLine();
	~FilterLine();

	void print();


	bool parse(std::string filterLine);

};



#endif // _INCLUDED_FILTERLINE_H_
