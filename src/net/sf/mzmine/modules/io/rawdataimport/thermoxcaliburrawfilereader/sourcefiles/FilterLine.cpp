// -*- mode: c++ -*-


/*
    File: FilterLine.cpp
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


#include "stdafx.h"
#include "FilterLine.h"
#include "MSUtilities.h"

#include <sstream>
#include <stack>
#include <iostream>
#include <cctype> // for toupper
#include <algorithm>


using namespace std;


/*

FilterLine dictionary
--From Thermo


Analyzer:

ITMS		Ion Trap
TQMS		Triple Quad
SQMS		Single Quad
TOFMS		TOF
FTMS		ICR
Sector		Sector

Segment Scan Event   (Sectors only)

Polarity
-		Negative
+		Positive


Scan Data
c		centroid
p		profile


Ionization Mode
EI		Electron Impact
CI		Chemical Ionization
FAB		Fast Atom Bombardment
ESI		Electrospray
APCI		Atmospheric Pressure Chemical Ionization
NSI		Nanospray
TSP		Thermospray
FD		Field Desorption
MALDI	Matrix Assisted Laser Desorption Ionization
GD		Glow Discharge

Corona
corona			corona on
!corona		corona off

PhotoIoniziation
pi			photo ionization on
!pi			photo ionization off

Source CID
sid			source cid on			
!sid			source cid off


Detector set
det			detector set
!det			detector not set


TurboScan
t			turbo scan on
!t			turob scan off

Enhanced			(Sectors only)
E			enhanced on
!E			enhanced off

Dependent Type
d			data dependent active
!d			data dependent not-active

Wideband
w			wideband activation on
!w			wideband activation off

Accurate Mass
!AM			accurate mass not active
AM			accurate mass active 
AMI			accurate mass with internal calibration
AME			accurate mass with external calibration

Ultra
u			ultra on
!u			ultra off

Scan Type:
full			full scan
SIM			single ion monitor
SRM			single reaction monitor
CRM
z			zoom scan
Q1MS			q1 mass spec scan
Q3MS			q3 mass spec scan 

Sector Scan			(Sectors only)
BSCAN		b scan
ESCAN		e scan



MSorder

MS2			MSn order
MS3
…
MS15

Activation Type
cid			collision induced dissociation
mpd
ecd			electron capture dissociation
pqd			pulsed q dissociation
etd			electron transfer dissociation
hcd			high energy collision dissociation
sa			supplemental cid
ptr			proton transfer reaction

Free Region			(Sectors only)
ffr1			field free region 1
ffr2			field free region 2

Mass range
[low mass – high mass]

*/

MSAnalyzerType 
FilterLine::parseAnalyzer(const string& word) {
	if (word == "ITMS") {
		return ITMS;
	}
	else if (word == "TQMS") {
		return TQMS;
	}
	else if (word == "SQMS") {
		return SQMS;
	}
	else if (word == "TOFMS") {
		return TOFMS;
	}
	else if (word == "FTMS") {
		return FTMS;
	}
	else if (word == "SECTOR") {
		return SECTOR;
	}
	else {
		return ANALYZER_UNDEF;
	}

}


MSPolarityType 
FilterLine::parsePolarity(const string& word) {
	if (word == "+") {
		return POSITIVE;
	}
	else if (word == "-") {
		return NEGATIVE;
	}
	// does this really exist?
	else if (word == "a") {
		return ANY;
	}
	else {
		return POLARITY_UNDEF;
	}
}


MSScanDataType 
FilterLine::parseScanData(const string& word) {
	if (word == "C") {
		return CENTROID;
	}
	else if (word == "P") {
		return PROFILE;
	}
	else {
		return SCANDATA_UNDEF;
	}
}




MSIonizationType 
FilterLine::parseIonizationMode(const string & word) {
	if (word == "EI" ){
		return EI;
	}
	else if (word == "CI") {
		return CI;
	}
	else if (word == "FAB") {
		return FAB;
	}
	else if (word == "ESI") {
		return ESI;
	}
	else if (word == "APCI") {
		return APCI;
	}
	else if (word == "NSI") {
		return NSI;
	}
	else if (word == "TSP") {
		return TSP;
	}
	else if (word == "FD") {
		return FD;
	}
	else if (word == "MALDI") {
		return MALDI;
	}
	else if (word == "GD") {
		return GD;
	}
	else {
		return IONIZATION_UNDEF;
	}
}



FilterLine::AccurateMassType 
FilterLine::parseAccuracteMass(const string& word) {
	if (word == "!AM") {
		return NO_AM;
	}
	else if (word == "AM") {
		return AM;
	}
	else if (word == "AMI") {
		return AMI;
	}
	else if (word == "AME") {
		return AME;
	}
	else {
		return ACCURATEMASS_UNDEF;
	}
}



MSScanType 
FilterLine::parseScanType(const string& word) {
	if (word == "FULL") {
		return FULL;
	}
	else if (word == "SIM") {
		return SIM;
	}
	else if (word == "SRM") {
		return SRM;
	}
	else if (word == "CRM") {
		return CRM;
	}
	else if (word == "Z") {
		return Z;
	}
	else if (word == "Q1MS") {
		return Q1MS;
	}
	else if (word == "Q3MS") {
		return Q3MS;
	}
	else {
		return SCAN_UNDEF;
	}
}



MSActivationType 
FilterLine::parseActivation(const string& word) {
	if (word == "CID") {
		return CID;
	}
	else if (word == "MPD") {
		return MPD;
	}
	else if (word == "ECD") {
		return ECD;
	}
	else if (word == "PQD") {
		return PQD;
	}
	else if (word == "ETD") {
		return ETD;
	}
	else if (word == "HCD") {
		return HCD;
	}
	else if (word == "SA") {
		return SA;
	}
	else if (word == "PTR") {
		return PTR;
	}
	else {
		return ACTIVATION_UNDEF;
	}
}


FilterLine::FilterLine() : 
analyzer_(ANALYZER_UNDEF),
polarity_(POLARITY_UNDEF),
scanData_(SCANDATA_UNDEF),
ionizationMode_(IONIZATION_UNDEF),
coronaOn_(BOOL_UNDEF),
photoIonizationOn_(BOOL_UNDEF),
sourceCIDOn_(BOOL_UNDEF),
detectorSet_(BOOL_UNDEF),
turboScanOn_(BOOL_UNDEF),
dependentActive_(BOOL_UNDEF),
widebandOn_(BOOL_UNDEF),
accurateMass_(ACCURATEMASS_UNDEF),
scanType_(SCAN_UNDEF),
msLevel_(0),
activationMethod_(ACTIVATION_UNDEF)
{
	cidParentMass_.clear();
	cidEnergy_.clear();
	scanRangeMin_.clear();
	scanRangeMax_.clear();
	transitionRangeMin_.clear();
	transitionRangeMax_.clear();
};


FilterLine::~FilterLine() {
}


void 
FilterLine::print() {
	if (analyzer_) {
		cout << "analyzer " << analyzer_ << endl;
	}

	if (polarity_) {
		cout << "polarity " << polarity_ << endl;
	}

	if (scanData_) {
		cout << "scan data " << scanData_ << endl;
	}

	if (ionizationMode_) {
		cout << "ionization mode " << ionizationMode_ << endl;
	}

	if (coronaOn_ != BOOL_UNDEF) {
		cout << "corona: " << coronaOn_ << endl;
	}

	if (photoIonizationOn_ != BOOL_UNDEF) {
		cout << "photoionization: " << photoIonizationOn_ << endl;
	}

	if (sourceCIDOn_ != BOOL_UNDEF) {
		cout << "source CID: " << sourceCIDOn_ << endl;
	}

	if (detectorSet_ != BOOL_UNDEF) {
		cout << "detector set: " << detectorSet_ << endl;
	}

	if (turboScanOn_ != BOOL_UNDEF) {
		cout << "turboscan: " << turboScanOn_ << endl;
	}

	if (dependentActive_ != BOOL_UNDEF) {
		cout << "data dependent: " << dependentActive_ << endl;
	}

	if (widebandOn_ != BOOL_UNDEF) {
		cout << "wideband: " << widebandOn_ << endl;
	}

	if (accurateMass_) {
		cout << "accurate mass: " << accurateMass_ << endl;
	}

	if (scanType_) {
		cout << "scan type: " << scanType_ << endl;
	}

	if (msLevel_ > 0 ) {
		cout << "MS level: " << msLevel_ << endl;
	}

	if (activationMethod_) {
		// cout << "activation type: " << activationMethod_ << endl;
	}


	cout << endl << endl << endl;

}


bool 
FilterLine::parse(string filterLine) {
	/**
	almost all of the fields are optional
	*/
	//boost::to_upper(filterLine);
	transform(filterLine.begin(), filterLine.end(), filterLine.begin(), 
		(int(*)(int)) toupper);
	stringstream s(filterLine);
	string w;


	if (s.eof()) {
		return 1; // ok, empty line
	}
	s >> w;

	analyzer_ = parseAnalyzer(w);
	if (analyzer_) {
		// "analyzer" field was present
		if (s.eof()) {
			return 1;
		}
		s >> w;
	}

	polarity_ = parsePolarity(w);
	if (polarity_) {
		// "polarity" field was present
		if (s.eof()) {
			return 1;
		}
		s >> w;
	}


	scanData_ = parseScanData(w);
	if (scanData_) {
		// "scan data type" field present
		if (s.eof()) {
			return 1;
		}
		s >> w;
	}


	ionizationMode_ = parseIonizationMode(w);
	if (ionizationMode_) {
		// "ionization mode" field present
		if (s.eof()) {
			return 1;
		}
		s >> w;
	}



	bool advance = false;



	// corona
	if (w == "!CORONA") {
		coronaOn_ = BOOL_FALSE;
		advance = true;
	}
	else if (w == "CORONA") {
		coronaOn_ = BOOL_TRUE;
		advance = true;
	}
	if (advance) {
		if (s.eof()) {
			return 1;
		}
		s >> w;
		advance = false;
	}

	// photoIonization
	if (w == "!PI") {
		photoIonizationOn_ = BOOL_FALSE;
		advance = true;
	}
	else if (w == "PI") {
		photoIonizationOn_ = BOOL_TRUE;
		advance = true;
	}
	if (advance) {
		if (s.eof()) {
			return 1;
		}
		s >> w;
		advance = false;
	}

	// source CID
	if (w == "!SID") {
		sourceCIDOn_ = BOOL_FALSE;
		advance = true;
	}
	else if (w == "SID") {
		sourceCIDOn_ = BOOL_TRUE;
		advance = true;
	}
	else if (w.substr(0,4) == "SID=") {
		// note: skipping SID=n value, as this usage isn't documented by Thermo but appears in files.
		sourceCIDOn_ = BOOL_TRUE;
		advance = true;
	}


	if (advance) {
		if (s.eof()) {
			return 1;
		}
		s >> w;
		advance = false;
	}


	// detector
	if (w == "!DET") {
		detectorSet_ = BOOL_FALSE;
		advance = true;
	}
	else if (w == "DET") {
		detectorSet_ = BOOL_TRUE;
		advance = true;
	}
	if (advance) {
		if (s.eof()) {
			return 1;
		}
		s >> w;
		advance = false;
	}


	// turboscan
	if (w == "!T") {
		turboScanOn_ = BOOL_FALSE;
		advance = true;
	}
	else if (w == "T") {
		turboScanOn_ = BOOL_TRUE;
		advance = true;
	}
	if (advance) {
		if (s.eof()) {
			return 1;
		}
		s >> w;
		advance = false;
	}


	// dependent type
	if (w == "!D") {
		dependentActive_ = BOOL_FALSE;
		advance = true;
	}
	else if (w == "D") {
		dependentActive_ = BOOL_TRUE;
		advance = true;
	}
	if (advance) {
		if (s.eof()) {
			return 1;
		}
		s >> w;
		advance = false;
	}

	if (w == "SA") {
		s >> w;
	}

	// wideband
	if (w == "!W") {
		widebandOn_ = BOOL_FALSE;
		advance = true;
	}
	else if (w == "W") {
		widebandOn_ = BOOL_TRUE;
		advance = true;
	}
	if (advance) {
		if (s.eof()) {
			return 1;
		}
		s >> w;
		advance = false;
	}


	accurateMass_ = parseAccuracteMass(w);
	if (accurateMass_) {
		// "accurate mass" field present
		if (s.eof()) {
			return 1;
		}
		s >> w;
	}

	scanType_ = parseScanType(w);
	if (scanType_) {
		// "scan type" field present
		if (s.eof()) {
			return 1;
		}
		s >> w;
	}

	// MS order
	if ( (w.substr(0,2) == "MS") && (w.length() >= 2) ) {
		if (w.length() == 2) {
			msLevel_ = 1; // just "MS"
		} else {
			// MSn: extract int n
			//cout << "len: " << w.length() << endl;
			msLevel_ = toInt(w.substr(2)); // take number after "ms"
		}
		if (s.eof()) {
			return 1;
		}
		s >> w;      
	}


	// activation info
	// if msLevel >=2 there should be mass@energy pairs for each level >= 2
	if (msLevel_ > 1) {
		int expectedPairs = msLevel_ - 1;
		for (int i=0; i< expectedPairs; ++i) {
			char c=w[0];
			int markerPos = w.find('@',0);
			// make sure this work starts with a numeric char, and the word contains "@"
			if ( ! ( (c >= '0') && (c <= '9') && (markerPos != string::npos )) ) {
				// return error
				return false;
			}

			string mass = w.substr(0, markerPos);
			string energy = w.substr(markerPos+1);
			// activation: if "energy" string starts with number, it is CID
			// otherwise, parse the activation type
			// for example: "401.432@cid234.2", "401.432@etd234.2"
			if (energy[0] >= '0' && energy[0] <= '9') {
				// it's CID
				activationMethod_ = CID;
				// "energy" is good to parse
			}
			else {
				string::size_type numericPos = energy.find_first_of("0123456789");
				if (numericPos == string::npos) {
					//error
					return false;
				}
				//cout << "original energy: " << energy << endl;
				string activationStr = energy.substr(0, numericPos);
				energy = energy.substr(numericPos);
				//cout << "numeric energy: " << energy << endl;
				//cout << "will parse: " << activationStr << endl;
				activationMethod_  = parseActivation(activationStr);
				if (activationMethod_ == ACTIVATION_UNDEF) {
					// error
					return false;
				}

			}


			// cout << "got mass " << mass << " at " << energy << " energy (from " << w << ")" << endl;
			cidParentMass_.push_back(toDouble(mass));
			cidEnergy_.push_back(toDouble(energy));			

			// prematurely done?
			if (s.eof()) {
				return false;
			}
			s >> w;      

		}
	}

	// activation type
/*
	activationMethod_ = parseActivation(w);
	if (activationMethod_) {
		// "activation type" field present
		if (s.eof()) {
			return 1;
		}
		s >> w;
	}
*/
	// product masses or mass ranges

	// TODO: record scan ranges
	// TODO: parse single values, for SIM, SRM, CRM
	// some test based on ms level?
	if (w[0] == '[') {
		// sometimes the filter line is "[ 342" instead of "[342"
		if (w.length() == 1) {
			// get the next one

			if (s.eof()) {
				return false; // error
			}
			s >> w;
		}
		else {
			w = w.substr(1); // loose the first "[" so we're on the same page
		}
	}

	// logic for SRM, which prints a transition list here
	if (scanType_ == SRM) {
		bool foundBracket = false;
		while (!foundBracket) {
			int markerPos = w.find('-',0);
			string startMass = w.substr(0, markerPos);
			string endMass = w.substr(markerPos+1);
			// check here for comma?
			int endPos = endMass.find_first_of("],");
			if (endPos != string::npos) {
				// remove tail
				endMass = endMass.substr(0, endPos);
			}
			//cout << "got transition start range " << startMass << " to " << endMass << " (from " << w << ")" << endl;
			transitionRangeMin_.push_back(toDouble(startMass));
			transitionRangeMax_.push_back(toDouble(endMass));

			int bracketPos = w.find(']');
			if (bracketPos != string::npos) {
				foundBracket = true;
			}
			else {
				// get the next word
				if (s.eof()) {
					return false; // error
				}
				s >> w;
			}
		}
	}
	else { // not SRM: these are mass ranges for the scan
		bool foundBracket=false;
		while (!foundBracket) {
			int markerPos = w.find('-',0);
			string startMass = w.substr(0, markerPos);
			string endMass = w.substr(markerPos+1);
			// check here for comma?
			int endPos = endMass.find_first_of("],");
			if (endPos != string::npos) {
				// remove tail
				endMass = endMass.substr(0, endPos);
			}
			// cout << "got startmass " << startMass << " to " << endMass << " (from " << w << ")" << endl;
			scanRangeMin_.push_back(toDouble(startMass));
			scanRangeMax_.push_back(toDouble(endMass));

			int bracketPos = w.find(']');
			if (bracketPos != string::npos) {
				foundBracket = true;
			}
			else {
				// get the next word
				if (s.eof()) {
					return false; // error
				}
				s >> w;
			}
		}
	}


	if (s.eof()) {
		// cout << "done parsing" << endl;
		return true;
	}
	else {
		do {
			cout << "unparsed filter line element: " << w << endl;
		} while (s >> w);
		return false;
	}
	//     while (!s.eof()) {
	//       string w;
	//       s >> w;
	//       cout << "word: " << w << endl;
	//     }
}
