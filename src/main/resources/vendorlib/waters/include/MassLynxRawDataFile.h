//-----------------------------------------------------------------------------------------------------
// FILE:			MassLynxRawDataFile.h
// DATE:			Nov 2009
// COPYRIGHT(C):	Waters Corporation
//
// COMMENTS:		This header contains the declaration of the MassLynxRawDataFileClass
//					class. 
//
//					
//-----------------------------------------------------------------------------------------------------
#pragma once
#pragma warning (disable : 4251)

#include <string>

#include "MassLynxRawDefs.h"

namespace micromass_co_uk_mlraw_version_1
{
	class MLRawDataCluster;
}

namespace Waters
{
namespace Lib
{
namespace MassLynxRaw
{
    class MLYNX_RAW_API MassLynxRawException : public std::exception
    {
    public:
        MassLynxRawException(std::string what) : m_what(what) {}
        virtual const char* what() const throw() { return m_what.c_str(); }

    private:
        std::string m_what;
    };

	///<sumary>
	///Check a MassLynxRaw result code
	///<\summary>
	void CheckMassLynxRawResultCode( 
        int ResultCode );

	///<sumary>
	///Class representing a MassLynx raw data file
	///<\summary>
	class MLYNX_RAW_API MassLynxRawDataFile
	{

	public:
		///<sumary>Read only property returning the internal
		///representation.
		///<\summary>
		///<remarks>
		///The representation is opaque
		///and is available for sharing amongst different
		///objects within this namesapce.  It therefore
		/// is not intended for external use.
		///<\remarks>
		micromass_co_uk_mlraw_version_1::MLRawDataCluster * getDataCluster()
        {
			return m_pImpl;
        }

        virtual ~MassLynxRawDataFile();

		///<summary>
		///Constructor taking a path to a MassLynx data file
		///that already exists on disk, or an empty string.
		///<\summary>
		///<param name='FullPathName'>
		///The fully qualified path to a disk based MassLynx raw
		///data file.
		///<\param>
		///<remark>
		///If the string is empty then the behaviour is as if the
		///default constructor had been called.
		///<\remark>
        MassLynxRawDataFile( const std::string & strFullPathName );


	protected:
		///<summary>
		///Default constructor. An empty object is created
		///with no disk based representation.
		///<\summary>
		MassLynxRawDataFile();

        ///<summary>
        /// Copy constructor
		///<\summary>
		MassLynxRawDataFile( MassLynxRawDataFile & DataFile );

		///<summary>
		///Initialises the DataFile object to a disk based
		///MassLynx data cluster.
		///<\summary>
		///<param name='FullPathName'>
		///The fully qualified path to a disk based MassLynx raw
		///data file.
		///<\param>
		///<remark>
		///If the string is empty then there is no change.
		///<\remark>
		void Initialise( const std::string & strFullPathName );

    protected:
		// The internal representation of the data file.
		micromass_co_uk_mlraw_version_1::MLRawDataCluster * m_pImpl;
};

}   // MassLynxRaw
}   // Lib
}   // Waters

