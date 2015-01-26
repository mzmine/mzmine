//-----------------------------------------------------------------------------------------------------
// FILE:			MassLynxRawChromatogramReader.h
// COPYRIGHT(C):	Waters Corporation
//
// COMMENTS:		This header contains the declaration of the MassLynxRawChromatogramReader
//					class.  The class allows the TIC and BPI chromatograms to be read as well
//                  as general mass chromatograms.
//
//					TODO:	Further documentation
//					
//-----------------------------------------------------------------------------------------------------
#pragma once

#include <string>
#include <vector>

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
    class MLYNX_RAW_API MassLynxRawChromatogramReader :  public MassLynxRawReader
	{
	
	public:
		///<summary>
		///Constructs a valid chromatogram reader object.
		///<\summary>
		///<param name='FullPathName'>
		///The fully qualified path name to a disk based MassLynx raw data file.
		///<\param>
		MassLynxRawChromatogramReader( const std::string & strFullPathName );

        ///<summary>
		///Constructs a valid chromatogram reader object.
		///<\summary>
		///<param name='massLynxRawReader'>
		///The MassLynxRawReader on which the chromatogram reader is based.
		///<\param>
		MassLynxRawChromatogramReader( MassLynxRawReader & massLynxRawReader );

        ///<summary>
		///Returns the complete TIC chromatogram of a function from the data file.
		///<\summary>
		///<param name='functionNumber'>
		///The function number from which the TIC chromatogram is required.
		///<\param>
		///<param name='Times'>
		///Vector of floating point values corresponding to the time values
        ///of the chromatogram.
		///data file.
		///<\param>
		///<param name='Intensities'>
		///Vector of floating point values corresponding to the intensity values
        ///of the chromatogram.
		///<\param>
        ///<returns>
        ///An integer value giving the size of the chromatogram.
        ///<\returns>
		int ReadTICChromatogram( 
            int functionNumber, 
            std::vector<float> & times, 
            std::vector<float> & intensities);

        ///<summary>
		///Returns the complete BPI chromatogram of a function from the data file.
		///<\summary>
		///<param name='functionNumber'>
		///The function number from which the TIC chromatogram is required.
		///<\param>
		///<param name='Times'>
		///Vector of floating point values corresponding to the time values
        ///of the chromatogram.
		///data file.
		///<\param>
		///<param name='Intensities'>
		///Vector of floating point values corresponding to the intensity values
        ///of the chromatogram.
		///<\param>
		///<remark>
		///An exception will be thrown if the data file is uninitialised. That
        ///is it corresponds to a valid MassLynx raw disk file.
		///<\remark>
        ///<returns>
        ///An integer value giving the size of the chromatogram.
        ///<\returns>
		int ReadBPIChromatogram( 
            int functionNumber, 
            std::vector<float> & times, 
            std::vector<float> & intensities);
      
        ///<summary>
		///Returns a mass chromatogram using a given mass and window or tolerance
        ///around the mass.
		///<\summary>
		///<param name='functionNumber'>
		///The function number from which the mass chromatogram is required.
		///<\param>
		///<param name='Mass'>
        ///The (target) mass used to obtain the mass chromatogram.
		///<\param>
		///<param name='Times'>
		///Vector of floating point values corresponding to the time values
        ///of the returned chromatogram.
		///<\param>
		///<param name='Intensities'>
		///Vector of floating point values corresponding to the intensity values
        ///of the returned chromatogram.
		///<\param>
		///<param name='massWindow'>
		///The mass range over which intensities are summed. 
		///<\param>
        ///<param name='bDaughters'>
        ///Extract chromatogram for daughters if true
        ///<\param>
		///<remark>
        ///The mass chromatogram is obtained by taking each scan and summing intensities
        ///over the massWindow range centered on the target mass.
		///An exception will be thrown if the data file is uninitialised. That
        ///is it corresponds to a valid MassLynx raw disk file.
		///<\remark>
        ///<returns>
        ///An integer value giving the size of the chromatogram.
        ///<\returns>
	    int ReadMassChromatogram( 
            int functionNumber, 
            float Mass, 
            std::vector<float> & times, 
            std::vector<float> & intensities,
            float massWindow,
            bool bDaughters);

        ///<summary>
		///Returns multiple mass chromatograms using given masses and a window or tolerance
        ///around the mass.
		///<\summary>
		///<param name='functionNumber'>
		///The function number from which the mass chromatogram is required.
		///<\param>
		///<param name='Masses'>
        ///The (target) masses used to obtain the mass chromatograms.
		///<\param>
		///<param name='Times'>
		///Vector of floating point values corresponding to the time values
        ///of the returned chromatograms.
		///<\param>
		///<param name='Intensities'>
		///2D Vector of floating point values corresponding to the intensity values
        ///of the returned chromatograms.
		///<\param>
		///<param name='massWindow'>
		///The mass range over which intensities are summed. 
		///<\param>
		///<remark>
        ///The mass chromatograms are obtained by taking each scan and summing intensities
        ///over the massWindow range centered on the target mass.
		///An exception will be thrown if the data file is uninitialised. That
        ///is it corresponds to a valid MassLynx raw disk file.
		///<\remark>
        ///<returns>
        ///An integer value giving the size of the chromatogram.
        ///<\returns>
        int ReadMultipleMassChromatograms( 
            int functionNumber,
            const std::vector<float> & Masses,
            std::vector<float> & Times,
            std::vector<std::vector<float>> & Intensities,
            float massWindow,
            bool bDaughters = true);

        ///<summary>
		///Returns multiple mass chromatograms for quadrupole data
		///<\summary>
		///<param name='functionNumber'>
		///The function number from which the mass chromatograms are required.
		///<\param>
		///<param name='Times'>
		///Vector of floating point values corresponding to the time values
        ///of the returned chromatograms.
		///<\param>
		///<param name='Intensities'>
		///2D Vector of floating point values corresponding to the intensity values
        ///of the returned chromatograms.
		///<\param>
		///<remark>
        ///All chromatograms for the specified function are returned.  The parent (and daughter)
        ///masses defining each chromatogram can be determined by calling the appropriate overload of
        ///readSpectrum for any scan in the specified function.
		///<\remark>
        ///<returns>
        ///An integer value giving the size of the chromatogram.
        ///<\returns>
        int ReadMRMChromatograms( 
            int functionNumber,
            std::vector<float> & Times,
            std::vector<std::vector<float>> & Intensities);

        ///<summary>
		///Returns a mass chromatogram for quadrupole data
		///<\summary>
		///<param name='functionNumber'>
		///The function number from which the mass chromatograms are required.
		///<\param>
        ///<param name='index'>
		///The index of the chromatogram to return
		///<\param>
		///<param name='Times'>
		///Vector of floating point values corresponding to the time values
        ///of the returned chromatograms.
		///<\param>
		///<param name='Intensities'>
		///2D Vector of floating point values corresponding to the intensity values
        ///of the returned chromatograms.
		///<\param>
		///<remark>
        ///All chromatograms for the specified function are returned.  The parent (and daughter)
        ///masses corresponding to each index can be determined by calling the appropriate overload of
        ///readSpectrum for any scan in the specified function.
		///<\remark>
        ///<returns>
        ///An integer value giving the size of the chromatogram.
        ///<\returns>
        int ReadMRMChromatogram( 
            int functionNumber,
            int index,
            std::vector<float> & Times,
            std::vector<float> & Intensities);

	private:
		// The internal representation of the access interface object.
		const micromass_co_uk_mlraw_version_1::MLRawAccessInterface * m_pImp;
		
	};
}
}
}

