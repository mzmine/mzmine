//-----------------------------------------------------------------------------------------------------
// FILE:			MassLynxRawInfo.h
// DATE:			Nov 2009
// COPYRIGHT(C):	Waters Corporation
//
// COMMENTTS:		This header contains the declaration of the MassLynxRawInfo
//					class.  See MassLynxRaw.h for further comments.
//					TODO:	Further documentation
//					
//-----------------------------------------------------------------------------------------------------
#pragma once

#include <string>

namespace Waters
{
namespace Lib
{
namespace MassLynxRaw
{
    ///<summary>
    ///Enumeration of MassLynx raw function types.
    ///</summary>
	enum FunctionType { 
        FT_MS=0, 
        FT_SIR, 
        FT_DLY, 
        FT_CAT, 
        FT_OFF, 
        FT_PAR, 
        FT_MSMS, 
        FT_NL, 
        FT_NG, 
        FT_MRM, 
        FT_Q1F, 
        FT_MS2, 
        FT_DAD, 
        FT_TOF, 
        FT_PSD, 
        FT_TOFS, 
        FT_TOFD, 
        FT_MTOF, 
        FT_TOFMS, 
        FT_TOFP, 
        FT_ASPEC_VSCAN,
        FT_ASPEC_MAGNET,
        FT_ASPEC_VSIR,
        FT_ASPEC_MAGNET_SIR,
        FT_QUAD_AUTO_DAU,
        FT_ASPEC_BE,
        FT_ASPEC_B2E,
        FT_ASPEC_CNL,
        FT_ASPEC_MIKES,
        FT_ASPEC_MRM,
        FT_ASPEC_NRMS,
        FT_ASPEC_MRMQ,
        FT_GENERIC
    };

    ///<summary>
    ///Enumeration of MassLynx raw ionisation modes.
    ///</summary>
    enum IonMode {
        IM_EIP = 0,
        IM_EIM,
        IM_CIP,
        IM_CIM,
        IM_FBP,
        IM_FBM,
        IM_TSP,
        IM_TSM,
        IM_ESP,
        IM_ESM,
        IM_AIP,
        IM_AIM,
        IM_LDP,
        IM_LDM,
        IM_FIP,
        IM_FIM,
        IM_GENERIC
    };

    ///<summary>
    ///Enumeration of MassLynx raw scan formats.
    ///</summary>
    enum DataType {
        DT_COMPRESSED		=		0,  // Compressed
        DT_STANDARD		    =		1,  // Standard
        DT_SIR_MRM			=		2,  // SIR or MRM
        DT_SCAN_CONTINUUM	=		3,  // Scanning Contimuum
        DT_MCA				=		4,  // MCA
        DT_MCASD			=		5,  // MCA with SD
        DT_MCB				=		6,  // MCB
        DT_MCBSD			=		7,  // MCB with SD
        DT_MOLWEIGHT		=		8,  // Molecular weight data
        DT_HIAC_CALIBRATED	=		9,  // High accuracy calibrated data
        DT_SFPREC			=		10, // Single float precision ( not used )
        DT_EN_UNCAL	    	=		11, // Enhanced uncalibrated data.
        DT_EN_CAL			=		12, // Enhanced calibrated data.
        DT_EN_CAL_ACC		=       13, // Enhanced calibrated accurate mass data
        DT_GENERIC
    };

	///<summary>
	///Class represententing information regarding
	///a MassLynx raw data file.
	///<\summary>
	class MLYNX_RAW_API MassLynxRawInfo : public MassLynxRawReader
	{
		
	public:
		///<summary>
		///Constructs a MassLynxRawInfo object from an already
		///initialised MassLynxRawReader object.
		///<\summary>
		///<param name='rawReader'>
		///A properly initialised MassLynxRawReader object
		///<\param>
		MassLynxRawInfo( MassLynxRawReader & rawReader );

        int GetFunctionCount() const;
        
		///<summary>
		///Return the function type string for a given function.
		///<\summary>
		///<param name='functionNumber'>
		///The function numbver for which the type is required.
		///<\param>
        std::string GetFunctionTypeString( int functionNumber ) const;

		///<summary>
		///Return the function type for a given function.
		///<\summary>
		///<param name='functionNumber'>
		///The function number for which the type is required.
		///<\param>
        FunctionType GetFunctionType( int functionNumber ) const;

        ///<summary>
		///Return the ionisation mode for a given function.
		///<\summary>
		///<param name='functionNumber'>
		///The function number for which the mode is required.
		///<\param>
        IonMode GetIonMode( int functionNumber ) const;
        
		///<summary>
		///Return the ionisation mode string for a given function.
		///<\summary>
		///<param name='functionNumber'>
		///The function numbeer for which the mode is required.
		///<\param>
        std::string GetIonModeString( int functionNumber ) const;

        
        ///<summary>
        ///Return the data type for the given function
        ///</summary>
        DataType GetDataType( int functionNumber ) const;
        
		///<summary>
		///Return the time range over which an acquisiton ran for a given function.
		///<\summary>
		///<param name='functionNumber'>
		///The function numbeer for which the mode is required.
		///<\param>
		///<param name='startTime'>
		///The time at which the acquisition began
		///<\param>
		///<param name='endTime'>
		///The time at which the acquisition finished
		///<\param>
        void GetAcquisitionTimeRange( 
            int functionNumber, 
            float & startTime, 
            float & endTime ) const;

        ///<summary>
		///Return the acquisition mass range.
		///<\summary>
		///<param name='functionNumber'>
		///The function numbeer for which the range is required.
		///<\param>
		///<param name='lowMass'>
		///The reference value in which to return the low mass value
		///<\param>
		///<param name='endTime'>
		///The reference value in which to return the high mass value
		///<\param>
        void GetAcquisitionMassRange( 
            int functionNumber, 
            float & lowMass, 
            float & highMass ) const;

		///<summary>
		///Read only accessor returning the number of scans
		///within a given function.
		///<\summary>
		///<param name='WhichFunction'>
		///The particular function for which to return the
		///number of scans. This is a zero based index.
		///<\param>
		int GetScansInFunction( int WhichFunction ) const;

        ///<summary>
        ///Accessor for continuum data property of function.
        ///</summary>
        ///<param name="nWhichFunction">function number (0-indexed)</param>
		bool GetIsContinuumFunction( int nWhichFunction ) const;

	private:
        FunctionType  FunctionStringToFunctionType( const std::string & type ) const;
	};

}
}
}

