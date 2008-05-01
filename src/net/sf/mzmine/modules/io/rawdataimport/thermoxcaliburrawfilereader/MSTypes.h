// -*- mode: c++ -*-


/*
	File: MSTypes.h
	Description: Shared enum types for instrument and scan information
	Date: July 30, 2007

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

#pragma once

#include <string>



typedef enum {
	MANUFACTURER_UNDEF = 0,
	THERMO, // specific incarnation unknown
	THERMO_SCIENTIFIC,
	THERMO_FINNIGAN,
	WATERS,
	ABI_SCIEX,
	AGILENT
} MSManufacturerType;
std::string toString(MSManufacturerType manufacturer);
std::string toOBO(MSManufacturerType manufacturer);



typedef enum {
	INSTRUMENTMODEL_UNDEF = 0,
	// thermo scientific
	LTQ,
	LTQ_XL,
	LTQ_FT,
	LTQ_FT_ULTRA,
	LTQ_ORBITRAP,
	LTQ_ORBITRAP_DISCOVERY,
	LTQ_ORBITRAP_XL,
	LXQ,
	TSQ_QUANTUM_ACCESS,

	// thermo finnigan
	LCQ_ADVANTAGE,
	LCQ_CLASSIC,
	LCQ_DECA,
	LCQ_DECA_XP,
	LCQ_DECA_XP_PLUS,
	LCQ_FLEET,

	// waters
	Q_TOF_MICRO,
	Q_TOF_ULTIMA,

	// ABI_SCIEX
	API_100,
	API_100_LC,
	API_150_MCA,
	API_150_EX,
	API_165,
	API_300,
	API_350,
	API_365,
	API_2000,
	API_3000,
	API_4000,
	GENERIC_SINGLE_QUAD,
	QTRAP,
	_4000_Q_TRAP,
	API_3200,
	_3200_Q_TRAP,
	API_5000,
	ELAN_6000,
	AGILENT_TOF,
	QSTAR,
	API_QSTAR_PULSAR,
	API_QSTAR_PULSAR_I,
	QSTAR_XL_SYSTEM,
} MSInstrumentModelType;
std::string toString(MSInstrumentModelType instrumentModel);
std::string toOBO(MSInstrumentModelType instrumentModel);



typedef enum {
	ACQUISITIONSOFTWARE_UNDEF = 0,
	XCALIBUR,
	MASSLYNX,
	ANALYST,
	ANALYSTQS,
	MASSHUNTER
} MSAcquisitionSoftwareType;
std::string toString(MSAcquisitionSoftwareType acquisitionSoftware);
std::string toOBO(MSAcquisitionSoftwareType acquisitionSoftware);



typedef enum {
	ANALYZER_UNDEF = 0,

	// originally defined from Thermo types
	ITMS,		// Ion Trap
	TQMS,		// Triple Quad
	SQMS,		// Single Quad
	TOFMS,		// TOF
	FTMS,		// ICR
	SECTOR,		// Sector

	// adding types for Waters

	// adding types for Analyst

	// adding types for Agilent MassHunter
	QTOF,		// quadrupole time of flight-- different than thermo's TOFMS?
} MSAnalyzerType;
std::string toString(MSAnalyzerType analyzer);
std::string toOBO(MSAnalyzerType analyzer);
std::string toOBOText(MSAnalyzerType analyzer);
MSAnalyzerType MSAnalyzerTypeFromString(const std::string &analyzer);



typedef enum {
	DETECTOR_UNDEF = 0,
} MSDetectorType;
std::string toString(MSDetectorType detector);
std::string toOBO(MSDetectorType detector);
MSDetectorType MSDetectorTypeFromString(const std::string &detector);

typedef enum {
	POLARITY_UNDEF = 0,
	POSITIVE,
	NEGATIVE,
	ANY // does this really exist? from original ReAdW
} MSPolarityType;



// rename?
typedef enum {
	SCANDATA_UNDEF = 0,
	CENTROID,
	PROFILE,
} MSScanDataType;



typedef enum {
	IONIZATION_UNDEF = 0,

	// originally added for Thermo
	EI,		// Electron Impact
	CI,		// Chemical Ionization
	FAB,	// Fast Atom Bombardment
	ESI,	// Electrospray
	APCI,	// Atmospheric Pressure Chemical Ionization
	NSI,	// Nanospray
	TSP,	// Thermospray
	FD,		// Field Desorption
	MALDI,	// Matrix Assisted Laser Desorption Ionization
	GD,		// Glow Discharge

	// added for Waters

	// added for Analyst

	// added for Agilent MassHunter
	// assuming NSI is equivilent to nanospray ESI
	MS_CHIP,
} MSIonizationType;
std::string toString(MSIonizationType ionization);
std::string toOBO(MSIonizationType ionization);
std::string toOBOText(MSIonizationType ionization);
MSIonizationType MSIonizationTypeFromString(const std::string &ionization);



typedef enum {
	SCAN_UNDEF = 0,

	// originally added for Thermo
	FULL,		// full scan
	SIM,       	// single ion monitor
	SRM,       	// single reaction monitor
	CRM,       	// 
	Z,			// zoom scan
	Q1MS,		// q1 (quadrupole 1) mass spec scan
	Q3MS,		// q3 (quadrupole 3) mass spec scan

	// added for Waters

	// added for Analyst
	Q1Scan,
	Q1MI, 
	Q3Scan, 
	Q3MI, 
	MRM, 
	PrecursorScan,
	ProductIonScan, 
	NeutralLossScan, 
	TOFMS1, 
	TOFMS2, 
	TOFPrecursorIonScan,
	EPI, 
	ER, 
	MS3, 
	TDF, 
	EMS,
	EMC,

	// added for Agilent MassHunter
} MSScanType;
std::string toString(MSScanType scanType);
std::string toOBO(MSScanType scanType);
std::string toOBOText(MSScanType scanType);

typedef enum {
	ACTIVATION_UNDEF = 0,

	// originally added for Thermo
	CID,		// collision induced dissociation
	MPD,		// 
	ECD,		// electron capture dissociation
	PQD,		// pulsed q dissociation
	ETD,		// electron transfer dissociation
	HCD,		// high energy collision dissociation
	SA,			// supplemental cid
	PTR,		// proton transfer reaction

	// added for Waters

	// added for Analyst

	// added for Agilent MassHunter
} MSActivationType;
std::string toString(MSActivationType activation);
std::string toOBO(MSActivationType activation);
std::string toOBOText(MSActivationType activation);
MSActivationType MSActivationTypeFromString(const std::string &activation);


typedef enum {
	SCAN_COORDINATE_UNDEF = 0,

	// added for Thermo

	// added for Waters

	// added for Analyst
	ABI_COORDINATE_SAMPLE,
	ABI_COORDINATE_PERIOD,
	ABI_COORDINATE_EXPERIMENT,
	ABI_COORDINATE_CYCLE

	// added for Agilent MassHunter

} ScanCoordinateType;
std::string toString(ScanCoordinateType scanCoordinateType);

