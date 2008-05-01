// -*- mode: c++ -*-


/*
	File: MSTypes.cpp
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

#include "MSTypes.h"
#include "stdafx.h"

using namespace std;

std::string toString(MSManufacturerType manufacturer) {
	string str;
	switch (manufacturer) {
		case THERMO:
			str = "Thermo";
			break;
		case THERMO_SCIENTIFIC:
			str = "Thermo Scientific";
			break;
		case THERMO_FINNIGAN:
			str = "Thermo Finnigan";
			break;
		case WATERS:
			str = "Waters";
			break;
		case ABI_SCIEX:
			str = "ABI / SCIEX";
			break;
		case AGILENT:
			str = "Agilent";
			break;
		case MANUFACTURER_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}

std::string toOBO(MSManufacturerType manufacturer) {
	string obo;
	switch (manufacturer) {
		case THERMO:
			obo = "MS:1000483";
			break;
		case THERMO_SCIENTIFIC:
			obo = "MS:1000494";
			break;
		case THERMO_FINNIGAN:
			obo = "MS:1000125";
			break;
		case WATERS:
			obo = "MS:1000126";
			break;
		case ABI_SCIEX:
			obo = "MS:1000121";
			break;
		case AGILENT:
			obo = "MS:1000490";
			break;
		case MANUFACTURER_UNDEF:
		default:
			obo = "MS:9999999";
			break;
	}
	return obo;
}

std::string toString(MSInstrumentModelType instrumentModel) {
	string str;
	switch (instrumentModel) {
		case LTQ:
			str = "LTQ";
			break;
		case LTQ_XL:
			str = "LTQ XL";
			break;		
		case LTQ_FT:
			str = "LTQ FT";
			break;
		case LTQ_FT_ULTRA:
			str = "LTQ FT Ultra";
			break;
		case LTQ_ORBITRAP:
			str = "LTQ Orbitrap";
			break;
		case LTQ_ORBITRAP_DISCOVERY:
			str = "LTQ Orbitrap Discovery";
			break;
		case LTQ_ORBITRAP_XL:
			str = "LTQ Orbitrap XL";
			break;
		case LXQ:
			str = "LXQ";
			break;
		case TSQ_QUANTUM_ACCESS:
			str = "TSQ Quantum Access";
			break;
		case LCQ_ADVANTAGE:
			str = "LCQ Advantage";
			break;	
		case LCQ_CLASSIC:
			str = "LCQ Classic";
			break;
		case LCQ_DECA:
			str = "LCQ Deca";
			break;
		case LCQ_DECA_XP:
			str = "LCQ Deca XP";
			break;
		case LCQ_DECA_XP_PLUS:
			str = "LCQ Deca XP Plus";
			break;
		case LCQ_FLEET:
			str = "LCQ Fleet";
			break;
		case Q_TOF_MICRO:
			str = "q-tof micro";
			break;
		case Q_TOF_ULTIMA:
			str = "Q-Tof Ultima";
			break;

		case API_100:
			str = "API 100";
			break;
		case API_100_LC:
			str = "API 100";
			break;
		case API_150_MCA:
			str = "API 150 MCA";
			break;
		case API_150_EX:
			str = "API 150 EX";
			break;
		case API_165:
			str = "API 165";
			break;
		case API_300:
			str = "API 300";
			break;
		case API_350:
			str = "API 350";
			break;
		case API_365:
			str = "API 365";
			break;
		case API_2000:
			str = "API 2000";
			break;
		case API_3000:
			str = "API 3000";
			break;
		case API_4000:
			str = "API 4000";
			break;
		case GENERIC_SINGLE_QUAD:
			str = "Generic Single Quad";
			break;
		case QTRAP:
			str = "QTrap";
			break;
		case _4000_Q_TRAP:
			str = "4000 Q Trap";
			break;
		case API_3200:
			str = "API 3200";
			break;
		case _3200_Q_TRAP:
			str = "3200 Q Trap";
			break;
		case API_5000:
			str = "API 5000";
			break;
		case ELAN_6000:
			str = "ELAN 6000";
			break;
		case AGILENT_TOF:
			str = "AGILENT TOF";
			break;
		case QSTAR:
			str = "QStar";
			break;
		case API_QSTAR_PULSAR:
			str = "API QStar Pulsar";
			break;
		case API_QSTAR_PULSAR_I:
			str = "API QStar Pulsar i";
			break;
		case QSTAR_XL_SYSTEM:
			str = "QSTAR XL System";
			break;

		case INSTRUMENTMODEL_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}

std::string toOBO(MSInstrumentModelType instrumentModel) {
	string obo;
	switch (instrumentModel) {
		case LTQ:
			obo = "MS:1000447";
			break;
		case LTQ_XL:
			obo = "MS:9999999"; // OBO: add
			break;
		case LTQ_FT:
			obo = "MS:1000448";
			break;
		case LTQ_FT_ULTRA:
			obo = "MS:1000557";
			break;
		case LTQ_ORBITRAP:
			obo = "MS:1000449";
			break;
		case LTQ_ORBITRAP_DISCOVERY:
			obo = "MS:1000555";
			break;
		case LTQ_ORBITRAP_XL:
			obo = "MS:1000556";
			break;
		case LXQ:
			obo = "MS:1000450";
			break;
		case TSQ_QUANTUM_ACCESS:
			obo = "MS:9999999";
			break;
		case LCQ_ADVANTAGE:
			obo = "MS:1000167";
			break;	
		case LCQ_CLASSIC:
			obo = "MS:1000168";
			break;
		case LCQ_DECA:
			obo = "MS:1000554";
			break;
		case LCQ_DECA_XP:
			obo = "MS:9999999"; // OBO: add
			break;
		case LCQ_DECA_XP_PLUS:
			obo = "MS:1000169";
			break;
		case LCQ_FLEET:
			obo = "MS:1000578";
			break;
		case Q_TOF_MICRO:
			obo = "MS:1000188";
			break;
		case Q_TOF_ULTIMA:
			obo = "MS:1000189";
			break;

		case API_100:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_100_LC:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_150_MCA:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_150_EX:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_165:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_300:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_350:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_365:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_2000:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_3000:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_4000:
			obo = "MS:9999999"; // OBO: add
			break;
		case GENERIC_SINGLE_QUAD:
			obo = "MS:9999999"; // OBO: add
			break;
		case QTRAP:
			obo = "MS:9999999"; // OBO: add
			break;
		case _4000_Q_TRAP:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_3200:
			obo = "MS:9999999"; // OBO: add
			break;
		case _3200_Q_TRAP:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_5000:
			obo = "MS:9999999"; // OBO: add
			break;
		case ELAN_6000:
			obo = "MS:9999999"; // OBO: add
			break;
		case AGILENT_TOF:
			obo = "MS:9999999"; // OBO: add
			break;
		case QSTAR:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_QSTAR_PULSAR:
			obo = "MS:9999999"; // OBO: add
			break;
		case API_QSTAR_PULSAR_I:
			obo = "MS:9999999"; // OBO: add
			break;
		case QSTAR_XL_SYSTEM:
			obo = "MS:9999999"; // OBO: add
			break;

		case INSTRUMENTMODEL_UNDEF:
		default:
			obo = "MS:9999999";
			break;
	}
	return obo;
}


std::string toString(MSAcquisitionSoftwareType acquisitionSoftware) {
	string str;
	switch (acquisitionSoftware) {
		case XCALIBUR:
			str = "Xcalibur";
			break;
		case MASSLYNX:
			str = "MassLynx";
			break;
		case ANALYST:
			str = "Analyst";
			break;
		case ANALYSTQS:
			str = "AnalystQS";
			break;
		case MASSHUNTER:
			str = "MassHunter";
			break;
		case ACQUISITIONSOFTWARE_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}


std::string toOBO(MSAcquisitionSoftwareType acquisitionSoftware) {
	string obo;
	switch (acquisitionSoftware) {
		case XCALIBUR:
			obo = "MS:1000532";
			break;
		case MASSLYNX:
			obo = "MS:1000534";
			break;
		case ANALYST:
			obo = "MS:1000551";
			break;
		case ANALYSTQS:
			obo = "MS:9999999"; // OBO: add
			break;
		case MASSHUNTER:
			obo = "MS:9999999"; // OBO: add
			break;
		case ACQUISITIONSOFTWARE_UNDEF:
		default:
			obo = "MS:9999999";
			break;
	}
	return obo;
}


std::string toString(MSAnalyzerType analyzer) {
	string str;
	switch (analyzer) {
		case ITMS:
			str = "ITMS";
			break;
		case TQMS:
			str = "TQMS";
			break;
		case SQMS:
			str = "SQMS";
			break;
		case TOFMS:
			str = "TOFMS";
			break;
		case FTMS:
			str = "FTMS";
			break;
		case SECTOR:
			str = "SECTOR";
			break;
		case QTOF:
			str = "QTOF";
			break;
		case ANALYZER_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}

std::string toOBOText(MSAnalyzerType analyzer) {
	string str;
	switch (analyzer) {
		case ITMS:
			str = "ion trap";
			break;
		case TQMS:
			str = "TQMS";
			break;
		case SQMS:
			str = "SQMS";
			break;
		case TOFMS:
			str = "time-of-flight";
			break;
		case FTMS:
			str = "fourier transform ion cyclotron resonance mass spectrometer";
			break;
		case SECTOR:
			str = "magnetic sector";
			break;
		case QTOF:
			str = "QTOF";
			break;
		case ANALYZER_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}

std::string toOBO(MSAnalyzerType analyzer) {
	string obo;
	switch (analyzer) {
		case ITMS:
			obo = "MS:1000264";
			break;
		case TQMS:
			obo = "MS:9999999"; // obo fix
			break;
		case SQMS:
			obo = "MS:9999999"; // obo fix
			break;
		case TOFMS:
			obo = "MS:1000084";
			break;
		case FTMS:
			obo = "MS:1000079";
			break;
		case SECTOR:
			obo = "MS:1000080";
			break;
		case QTOF:
			obo = "MS:9999999"; // obo fix
			break;
		case ANALYZER_UNDEF:
		default:
			obo = "MS:9999999";
			break;
	}
	return obo;
}


MSAnalyzerType MSAnalyzerTypeFromString(const std::string &analyzer) {

	if (analyzer == "TIMS") return ITMS;
	if (analyzer == "TQMS") return TQMS;
	if (analyzer == "SQMS") return SQMS;
	if (analyzer == "TOFMS") return TOFMS;
	if (analyzer == "FTMS") return FTMS;
	if (analyzer == "SECTOR") return SECTOR;
	if (analyzer == "QTOF") return QTOF;

	return ANALYZER_UNDEF;
}


std::string toString(MSDetectorType detector) {
	string str;
	switch (detector) {
		case DETECTOR_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}

std::string toOBO(MSDetectorType detector) {
	string obo;
	switch (detector) {
		case DETECTOR_UNDEF:
		default:
			obo = "MS:9999999";
			break;
	}
	return obo;
}

MSDetectorType MSDetectorTypeFromString(const std::string &detector) {
	return DETECTOR_UNDEF;
}

std::string toString(MSIonizationType ionization) {
	string str;
	switch (ionization) {
		case EI:
			str = "EI";
			break;
		case CI:
			str = "CI";
			break;
		case FAB:
			str = "FAB";
			break;
		case ESI:
			str = "ESI";
			break;
		case APCI:
			str = "APCI";
			break;
		case NSI:
			str = "NSI";
			break;
		case TSP:
			str = "TSP";
		case FD:
			str = "FD";
			break;
		case MALDI:
			str = "MALDI";
			break;
		case GD:
			str = "GD";
			break;
		case MS_CHIP:
			str = "MS_CHIP";
			break;
		case IONIZATION_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}

std::string toOBO(MSIonizationType ionization) {
	string obo;
	switch (ionization) {
		case EI:
			obo = "MS:1000389";
			break;
		case CI:
			obo = "MS:1000386";
			break;
		case FAB:
			obo = "MS:1000074";
			break;
		case ESI:
			obo = "MS:1000073";
			break;
		case APCI:
			obo = "MS:1000070";
			break;
		case NSI:
			obo = "MS:1000398";
			break;
		case TSP:
			obo = "MS:9999999"; // obo fix
			break;
		case FD:
			obo = "MS:1000257";
			break;
		case MALDI:
			obo = "MS:1000075";
			break;
		case GD:
			obo = "MS:1000259";
			break;
		case MS_CHIP:
			obo = "MS:9999999"; // obo fix
			break;
		case IONIZATION_UNDEF:
		default:
			obo = "MS:9999999";
			break;
	}
	return obo;
}

std::string toOBOText(MSIonizationType ionization) {
	string str;
	switch (ionization) {
		case EI:
			str = "electron ionization";
			break;
		case CI:
			str = "chemi-ionization";
			break;
		case FAB:
			str = "fast atom bombardment ionization";
			break;
		case ESI:
			str = "electrospray ionization";
			break;
		case APCI:
			str = "atmospheric pressure chemical ionization";
			break;
		case NSI:
			str = "nanoelectrospray";
			break;
		case TSP:
			str = "TSP";
		case FD:
			str = "field desorption";
			break;
		case MALDI:
			str = "matrix-assisted laser desorption ionization";
			break;
		case GD:
			str = "glow discharge ionization";
			break;
		case MS_CHIP:
			str = "MS_CHIP";
			break;
		case IONIZATION_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}


MSIonizationType MSIonizationTypeFromString(const std::string &ionization) {
	if (ionization=="EI") return EI;
	if (ionization=="CI") return CI;
	if (ionization=="FAB") return FAB;
	if (ionization=="ESI") return ESI;
	if (ionization=="APCI") return APCI;
	if (ionization=="NSI") return NSI;
	if (ionization=="TSP") return TSP;
	if (ionization=="FD") return FD;
	if (ionization=="MALDI") return MALDI;
	if (ionization=="GD") return GD;
	if (ionization=="MS_CHIP") return MS_CHIP;

	return IONIZATION_UNDEF;
}

std::string toString(MSScanType scanType) {
	string str;
	switch (scanType) {
		case FULL:
			str = "FULL";
			break;
		case SIM:
			str = "SIM";
			break;
		case SRM:
			str = "SRM";
			break;
		case CRM:
			str = "CRM";
			break;
		case Z:
			str = "Z";
			break;
		case Q1MS:
			str = "Q1MS";
			break;
		case Q3MS:
			str = "Q3MS";
			break;

		case Q1Scan:
			str = "Q1 Scan";
			break;
		case Q1MI:
			str = "Q1 MI";
			break;
		case Q3Scan:
			str = "Q3 Scan";
			break;
		case Q3MI:
			str = "Q3 MI";
			break;
		case MRM:
			str = "MRM";
			break;
		case PrecursorScan:
			str = "Precursor Scan";
			break;
		case ProductIonScan:
			str = "Product IonS can";
			break;
		case NeutralLossScan:
			str = "Neutral Loss Scan";
			break;
		case TOFMS1:
			str = "TOF MS1";
			break;
		case TOFMS2:
			str = "TOF MS2";
			break;
		case TOFPrecursorIonScan:
			str = "TOF Precursor Ion Scan";
			break;
		case EPI: // Enhanced Product Ion
			str = "EPI";
			break;
		case ER: // Enhanced Resolution
			str = "ER";
			break;
		case MS3:
			str = "MS3";
			break;
		case TDF: // Time Delayed Fragmentation
			str = "TDF";
			break;
		case EMS: // Enhanced MS
			str = "EMS";
			break;
		case EMC: // Enhanced Multi-Charge
			str = "EMC";
			break;

		case SCAN_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}

std::string toOBO(MSScanType scanType) {
	string obo;
	switch (scanType) {
		case FULL:
			obo = "MS:1000498";
			break;
		case SIM:
			obo = "MS:1000205";
			break;
		case SRM:
			obo = "MS:1000206";
			break;
		case CRM:
			obo = "MS:1000244";
			break;
		case Z:
			obo = "MS:1000497";
			break;
		case Q1MS:
			obo = "MS:9999999"; // obo fix
			break;
		case Q3MS:
			obo = "MS:9999999"; // obo fix
			break;

		case Q1Scan:
			obo = "MS:9999999"; // obo fix
			break;
		case Q1MI:
			obo = "MS:9999999"; // obo fix
			break;
		case Q3Scan:
			obo = "MS:9999999"; // obo fix
			break;
		case Q3MI:
			obo = "MS:9999999"; // obo fix
			break;
		case MRM:
			obo = "MS:9999999"; // obo fix
			break;
		case PrecursorScan:
			obo = "MS:9999999"; // obo fix
			break;
		case ProductIonScan:
			obo = "MS:9999999"; // obo fix
			break;
		case NeutralLossScan:
			obo = "MS:9999999"; // obo fix
			break;
		case TOFMS1:
			obo = "MS:9999999"; // obo fix
			break;
		case TOFMS2:
			obo = "MS:9999999"; // obo fix
			break;
		case TOFPrecursorIonScan:
			obo = "MS:9999999"; // obo fix
			break;
		case EPI:
			obo = "MS:9999999"; // obo fix
			break;
		case ER:
			obo = "MS:9999999"; // obo fix
			break;
		case MS3:
			obo = "MS:9999999"; // obo fix
			break;
		case TDF:
			obo = "MS:9999999"; // obo fix
			break;
		case EMS:
			obo = "MS:9999999"; // obo fix
			break;
		case EMC:
			obo = "MS:9999999"; // obo fix
			break;

		case SCAN_UNDEF:
		default:
			obo = "MS:9999999";
			break;
	}
	return obo;
}

std::string toOBOText(MSScanType scanType) {
	string str;
	switch (scanType) {
		case FULL:
			str = "full scan";
			break;
		case SIM:
			str = "selected ion monitoring";
			break;
		case SRM:
			str = "selected reaction monitoring";
			break;
		case CRM:
			str = "consecutive reaction monitoring";
			break;
		case Z:
			str = "zoom scan";
			break;
		case Q1MS:
			str = "Q1MS";
			break;
		case Q3MS:
			str = "Q3MS";
			break;

		case Q1Scan:
			str = "Q1 Scan";
			break;
		case Q1MI:
			str = "Q1 MI";
			break;
		case Q3Scan:
			str = "Q3 Scan";
			break;
		case Q3MI:
			str = "Q3 MI";
			break;
		case MRM:
			str = "MRM";
			break;
		case PrecursorScan:
			str = "Precursor Scan";
			break;
		case ProductIonScan:
			str = "Product Ion Scan";
			break;
		case NeutralLossScan:
			str = "Neutral Loss Scan";
			break;
		case TOFMS1:
			str = "TOF MS1";
			break;
		case TOFMS2:
			str = "TOF MS2";
			break;
		case TOFPrecursorIonScan:
			str = "TOF Precursor Ion Scan";
			break;
		case EPI: // Enhanced Product Ion
			str = "EPI";
			break;
		case ER: // Enhanced Resolution
			str = "ER";
			break;
		case MS3:
			str = "MS3";
			break;
		case TDF: // Time Delayed Fragmentation
			str = "TDF";
			break;
		case EMS: // Enhanced MS
			str = "EMS";
			break;
		case EMC: // Enhanced Multi-Charge
			str = "EMC";
			break;

		case SCAN_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}



std::string toString(MSActivationType activation) {
	string str;
	switch (activation) {
		case CID:
			str = "CID";
			break;
		case MPD:
			str = "MPD";
			break;
		case ECD:
			str = "ECD";
			break;
		case PQD:
			str = "PQD";
			break;
		case ETD:
			str = "ETD";
			break;
		case HCD:
			str = "HCD";
			break;
		case SA:
			str = "SA";
			break;
		case PTR:
			str = "PTR";
			break;
		case ACTIVATION_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}

std::string toOBO(MSActivationType activation) {
	string obo;
	switch (activation) {
		case CID:
			obo = "MS:1000133";
			break;
		case MPD:
			obo = "MS:9999999"; // obo fix
			break;
		case ECD:
			obo = "MS:1000250";
			break;
		case PQD:
			obo = "MS:9999999"; // obo fix
			break;
		case ETD:
			obo = "MS:9999999"; // obo fix
			break;
		case HCD:
			obo = "MS:1000422";
			break;
		case SA:
			obo = "MS:9999999"; // obo fix
			break;
		case PTR:
			obo = "MS:9999999"; // obo fix
			break;
		case ACTIVATION_UNDEF:
		default:
			obo = "MS:9999999";
			break;
	}
	return obo;
}

std::string toOBOText(MSActivationType activation) {
	string str;
	switch (activation) {
		case CID:
			str = "collision-induced dissociation";
			break;
		case MPD:
			str = "MPD";
			break;
		case ECD:
			str = "electron capture dissociation";
			break;
		case PQD:
			str = "PQD";
			break;
		case ETD:
			str = "ETD";
			break;
		case HCD:
			str = "high-energy collision-induced dissociation";
			break;
		case SA:
			str = "SA";
			break;
		case PTR:
			str = "PTR";
			break;
		case ACTIVATION_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}


MSActivationType MSActivationTypeFromString(const std::string &activation) {

	if (activation == "CID") return CID;
	if (activation == "MPD") return MPD;
	if (activation == "ECD") return ECD;
	if (activation == "PQD") return PQD;
	if (activation == "ETD") return ETD;
	if (activation == "HCD") return HCD;
	if (activation == "SA") return SA;
	if (activation == "PTR") return PTR;

	return ACTIVATION_UNDEF;
}


std::string toString(ScanCoordinateType scanCoordinateType) {
	string str;
	switch (scanCoordinateType) {
		case ABI_COORDINATE_SAMPLE:
			str = "sample";
			break;
		case ABI_COORDINATE_PERIOD:
			str = "period";
			break;
		case ABI_COORDINATE_EXPERIMENT:
			str = "experiment";
			break;
		case ABI_COORDINATE_CYCLE:
			str = "cycle";
			break;
		case SCAN_COORDINATE_UNDEF:
		default:
			str = "unknown";
			break;
	}
	return str;
}

