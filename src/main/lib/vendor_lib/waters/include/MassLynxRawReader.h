//-----------------------------------------------------------------------------------------------------
// FILE:			MassLynxRawReader.h
// DATE:			Nov 2009
// COPYRIGHT(C):	Waters Corporation
//
// COMMENTS:		This header contains the delclaration of the MassLynxRawReader
//					class.  See MassLynxRaw.h for further comments.
//					
//-----------------------------------------------------------------------------------------------------
#pragma once

#include <string>
#include <exception>

#include "MassLynxRawDefs.h"

namespace Waters
{
namespace Lib
{
namespace MassLynxRaw
{
	class MLYNX_RAW_API MassLynxRawReader : public MassLynxRawDataFile
	{

	public:
		///<summary>
		///Single stage construction and initialisation.
		///<\summary>
		///<remark>
		///In single stage construction and initialisation the
		///object is both constructed and then initialised with
		///the disk based file information.
		///<\remark>
		///<param name="strFullPathName">
		///The fully qualified path name to a disk based MassLynx raw data file.
		///<\param>
		MassLynxRawReader( const std::string & strFullPathName );

        ///<summary>
		///Copy construction.
		///<\summary>
		///<remark>
		///This is a shallow copy of the undelying data object
		///<\remark>
		///<param name="massLynxRawReader">
        ///The object from which to make the (shallow) copy.
		///<\param>
        MassLynxRawReader( MassLynxRawReader & massLynxRawReader);

		///<summary>
		///Default constructor for two stage construction and initialization.
		///<\summary>
		///<remark>
		///The constructor creates an empty data object.  Before
		///the object is available for use however it must
		///be initialised with the fully qualified path
		///to a disk based MassLynx raw data file.
		///<\remark>
		MassLynxRawReader();


        ///<summary>
        ///Loads scans from a function.
        ///</summary>
        ///<remark>
        ///Data is calibrated and uncompressed, if necessary.
        ///</remark>
        ///<param name="funcNum">function number (0-indexed)</param>
        ///<param name="startScan">start scan (0-indexed)</param>
        ///<param name="endScan">end scan (0-indexed)</param>
		void BufferScans( 
            int funcNum, 
            int startScan, 
            int endScan);

		///<summary>
		///Initialise this object with the data from
		///a disk based MassLynx raw data file.
		///<\summary>
		///<remark>
		///This routine can be called on an object that
		///is already initialised.  In which case the object
		///will simple be reinitialised.
		///<\remark>
		///<param name='FullPathName'>
		///The fully qualified path name to a disk based MassLynx raw data file.
		///<\param>
		//void InitializeReader( const std::string & strFullPathName );


        ///<summary>
        ///Return some meta data from a MassLynx file.
        ///<\summary>
        ///<remark>
        ///There are a fixed number of tags or label corresponding
        ///to mostly information from a MassLynx header file.
        ///<\remark>
        ///<param name='strTag'>
        ///The tag or label corresponding to the information to be retrieved.
        ///<\param>
        ///<return>Metadata value for the given tag</return>
		void MassLynxRawReader::ReadMetaData( 
			const std::string & strTag, std::string & strMetaData  ) const;

        ///<summary>
        ///Return some meta data from a MassLynx file.
        ///<\summary>
        ///<remark>
        ///There are a fixed number of tags or label corresponding
        ///to mostly information from a MassLynx header file.
        ///<\remark>
        ///<param name='strTag'>
        ///The tag or label corresponding to the information to be retrieved.
        ///<\param>
        ///<param name="funcNum">function number (0-indexed)</param>
        ///<return>Metadata value for the given tag</return>
		void MassLynxRawReader::ReadMetaData( 
			const std::string & strTag, int funcNum, std::string & strMetaData) const;

	};
}
}
}
