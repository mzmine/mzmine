//-----------------------------------------------------------------------------------------------------
// FILE:			MassLynxRawScanReader.h
// DATE:			Nov 2009
// COPYRIGHT(C):	Waters Corporation
//
// COMMENTS:		This header contains the declaration of the MassLynxRawScanReader
//					class.  The class represents the core raw data of a MassLynx
//					acquisition and therefore allows data to be read from a raw
//					file.
//					
//-----------------------------------------------------------------------------------------------------
#pragma once

#include <vector>
#include "MassLynxRawDefs.h"

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
	///<summary>
	///This class allows reading of raw scan data from a MassLynx
	///raw file.
	///<\summary>
	class MLYNX_RAW_API MassLynxRawScanReader :  public MassLynxRawReader
	{
	public:

		///<summary>
		///Constructs a valid scan reader object.
		///<\summary>
		///<param name='FullPathName'>
		///The fully qualified path name to a disk based MassLynx raw data file.
		///<\param>
		MassLynxRawScanReader( 
			const std::string & strFullPathName );


        MassLynxRawScanReader( 
            MassLynxRawReader & massLynxRawReader );

		///<summary>
		///Return a particular scan from a MassLynx raw data file.
		///<\summary>
		///<param name='nWhichFunction'>
		///The zero based function number from which the scan is to
		///be read.
		///<\param>
		///<param name='nWhichScan'>
		///The zero based scan index of the scan to retrieve.
		///<\param>
        ///<return>an MS spectrum populated with mass, intensity pairs</return>
        void MassLynxRawScanReader::readSpectrum( 
            int nWhichFunction, 
            int nWhichScan,
			std::vector<float> & masses,
			std::vector<float> & intensities);

		void MassLynxRawScanReader::readSpectrum( 
            int nWhichFunction, 
            int nWhichScan,
			std::vector<float> & masses,
			std::vector<float> & intensities,
			std::vector<float> & daughterMasses);

	private:

		// The internal representation of the data file.
		const micromass_co_uk_mlraw_version_1::MLRawAccessInterface * m_pImp;

	};

}   // MassLynxRaw
}   // Lib
}   // Waters

