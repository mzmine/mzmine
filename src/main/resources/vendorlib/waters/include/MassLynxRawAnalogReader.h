//-----------------------------------------------------------------------------------------------------
// FILE:			MassLynxRawAnalogReader.h
// AUTHOR:			Richard Denny
// DATE:			January 2007
// COPYRIGHT(C):	Waters Corporation
//
// COMMENTS:		This header contains the declaration of the MassLynxRawAnalogReader
//					class.  The class represents part of the core raw data of a MassLynx
//					acquisition and therefore allows data to be read from a raw
//					file.  In this case the raw information is the analog channel data. 
//
//					TODO:	Further documentation
//					
//-----------------------------------------------------------------------------------------------------
#pragma once
//using namespace System;
//using namespace System::Runtime::InteropServices;

#include <string>
#include <vector>

// Classes declared in this header are managed wrappers
// around pre-existing unmanaged c++ code.
// Here we have a forward declaration of the unmanaged
// c++ object representing a raw  data file and here
// called a cluster. Otherwise we don't use this terminology
// for the reasons mentioned above.
namespace micromass_co_uk_mlraw_version_1
{
	class MLRawAccessInterface;
}

//typedef array<float> ^ floatArray;
typedef std::vector<float> floatVector;

/// <summary>
/// We make a nested namespace. The name com::waters
/// is the standard namespace for all software.
/// rawdata contains any classes regarding raw data
/// which may involve raw data in xml format of one form
/// or another. Finally the masslynx name space for classes
/// representing the traditonal MassLynx raw data formatted
/// files.
/// <\summary>
namespace Waters
{
namespace Lib
{
namespace MassLynxRaw
{
	

	// Managed version of AnalogHeader
	struct AnalogChannelHeader
	{
		// true if analogue data
		bool Analog;
		// true if evaporative light scattering dectector data
		bool ELSD;
		// true if readback data
		bool Readback;
		// channel description string
		std::string Description;
		// channel units
		std::string Units;
	};

	///<summary>
	///This class allows reading of raw scan stats from a MassLynx
	///raw file.
	///<\summary>
	class MLYNX_RAW_API MassLynxRawAnalogReader : public MassLynxRawReader
	{
    public:
        ///<summary>Construct with a MassLynxRawReader</summary>
        ///<param name="massLynxRawReader">the raw file reader</param>
        MassLynxRawAnalogReader(MassLynxRawReader & massLynxRawReader );

        ///<summary>Get number of analogue channels</summary>
        ///<return>the number of channels stored</return>
        int getNumberAnalogChannels();

        ///<summary>Get number of data in channel</summary>
        ///<param name="nChannel">the channel number</param>
        int getAnalogChannelLength(
            int nChannel);

        ///<summary>Get the descriptor for a channel</summary>
        ///<param name="nChannel">the channel number</param>
        ///<return>the analog channel header</return>
        AnalogChannelHeader getAnalogChannelHeader( int nChannel);

        ///<summary>Get the data for a channel</summary>
        ///<param name="nChannel">the channel number</param>
        ///<return>the analog channel data</return>
        int MassLynxRawAnalogReader::getAnalogChannelData( int nChannel, floatVector &times, floatVector &intensities );

	private:

		// The "opaque" internal representation of the data file.
		const micromass_co_uk_mlraw_version_1::MLRawAccessInterface* m_pImp;
		
	};

    

}
}
}

