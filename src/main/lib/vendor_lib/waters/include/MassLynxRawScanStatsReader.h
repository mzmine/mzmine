//-----------------------------------------------------------------------------------------------------
// FILE:			MassLynxRawScanStatsReader.h
// DATE:			Nov 2009
// COPYRIGHT(C):	Waters Corporation
//
// COMMENTS:		This header contains the declaration of the MassLynxRawScanStatsReader
//					class. 
//
//					
//-----------------------------------------------------------------------------------------------------
#pragma once
#include <vector>
#include <string>

namespace micromass_co_uk_mlraw_version_1
{
	class MLRawAccessInterface;
}

namespace Waters
{
namespace Lib
{
namespace MassLynxRaw
{
	class MassLynxRawScan;

    ///<summary>
    ///This class holds scan statistics
    ///<\summary>
	class MLYNX_RAW_API MSScanStats
    {  
    public:
        int   scanOffset;           // Offset into the data file for this scan.
        int   peaksInScan;          // Numer of peaks in the scan.
        short segmentNumber;        // Segment number of this scan (MTOF function).
        short useContinuumFlag;     // Continuum data flag.
        short continuumDataOverride;// Override continuum data.
        short molecularMasses;      // Scan contains molecular masses.
        short calibratedMasses;     // Scan contains callibrated masses.
        short scanOverload;         // Scan overload.
        bool  isContinuumScan;      // True for continuum data.

        float tic;                  // Total ion current for the scan.
        float rt;                   // Retention time for the scan.
        float basePeakIntensity;    // Base peak intensity for the scan.
        float basePeakMass;         // Base peak mass for the scan.
    }; 

    ///<summary>
    ///Type codes for extended stat's fields
    ///<\summary>
    enum ExtStatsTypeCode
    {
        CHAR=0, SHORT_INT, LONG_INT, SINGLE_FLOAT, DOUBLE_FLOAT, STRING
    };

    ///<summary>
    ///This class holds a description of an extended stats record
    ///<\summary>
    class MLYNX_RAW_API ExtendedStatsType
    {
    public:
        std::string       name;     // field name
        int               code;     // integer field code
        ExtStatsTypeCode  typeCode; // field type code
    };

	///<summary>
	///This class allows reading of raw scan stats from a MassLynx
	///raw file.
	///<\summary>
	class MLYNX_RAW_API MassLynxRawScanStatsReader : public MassLynxRawReader
	{
	public:
        ///<summary>Construct with a MassLynxRawReader</summary>
        ///<param name="massLynxRawReader">the raw file reader</param>
        MassLynxRawScanStatsReader( MassLynxRawReader& massLynxRawReader );

        ///<summary>Destroy a MassLynxRawReader</summary>
        ~MassLynxRawScanStatsReader();

        ///<summary>Read scan stat's, e.g. TIC, BPI, etc.</summary>
        ///<param name="nWhichFunction">function number (0-indexed)</param>
        ///<param name="msScanStats">vector to be populated with scan stat's</param>
        void readScanStats( int nWhichFunction,  std::vector<MSScanStats>& msScanStats) const;

        ///<summary></summary>
        ///<param name="nWhichFunction">function number (0-indexed)</param>
        ///<param name="extStatsTypes"></param>
        void getExtendedStatsTypes( int nWhichFunction, std::vector<ExtendedStatsType>& extStatsTypes) const;

        /// <summary>Get a particular extended stats field for all scans in a function.</summary>
        ///<param name="nWhichFunction">function number (0-indexed)</param>
        ///<param name="type">type to return</param>
        ///<param name="values">vector containing values as type T, whatever the original type.</param>
        template<class T> void getExtendedStatsField(
          int nWhichFunction, const ExtendedStatsType& type, std::vector<T>& values) const;

	private:

		// The internal representation of the data file.
		const micromass_co_uk_mlraw_version_1::MLRawAccessInterface * m_pImp;
        const MassLynxRawInfo * m_pInfo;
	};

    //Explicit instantiation of getExtendedStatsField varieties for dll export
    template void MLYNX_RAW_API MassLynxRawScanStatsReader::getExtendedStatsField<char>  (int, const ExtendedStatsType&, std::vector<char>&)   const;
    template void MLYNX_RAW_API MassLynxRawScanStatsReader::getExtendedStatsField<short> (int, const ExtendedStatsType&, std::vector<short>&)  const;
    template void MLYNX_RAW_API MassLynxRawScanStatsReader::getExtendedStatsField<int>   (int, const ExtendedStatsType&, std::vector<int>&)    const;
    template void MLYNX_RAW_API MassLynxRawScanStatsReader::getExtendedStatsField<float> (int, const ExtendedStatsType&, std::vector<float>&)  const;
    template void MLYNX_RAW_API MassLynxRawScanStatsReader::getExtendedStatsField<double>(int, const ExtendedStatsType&, std::vector<double>&) const;
}
}
}

