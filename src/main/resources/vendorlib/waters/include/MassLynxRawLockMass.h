#pragma once
#include "MassLynxRawDefs.h"


class MLRawLockMassImp;

namespace Waters
{
namespace Lib
{
namespace MassLynxRaw
{

class  MLYNX_RAW_API MassLynxRawLockMass : public MassLynxRawReader
{
public:
	///<summary>
	///Constructs a valid Lockmass  object.
	///<\summary>
	///<param name='massLynxRawReader'>
	/// Valid massLynx reader to a disk based MassLynx raw data file.
	///<\param>
	MassLynxRawLockMass( MassLynxRawReader & massLynxRawReader);
	MassLynxRawLockMass( const std::string & strFullPathName);
	
	///<summary>
	/// Updates the applied lock mass correction
	///<\summary>
	///<param name='fLockmass'>
	/// Mass of the lockmass.
	///<\param>
	///<param name='fTolerance'> // window
	/// Window applied
	///<\param>
	void MassLynxRawLockMass::UpdateLockMassCorrection(const float& fLockMass, const float& fTolerance);

	///<summary>
	/// Removes the applied lock mass correction
	///<\summary>
	void MassLynxRawLockMass::RemoveLockMassCorrection();													

	///<summary>
	/// Checks if we lock mass correction is currently applied
	///<\summary>
	///<param name='bApplied'>
	/// true if lock mass correction is applied
	///<\param>
	void MassLynxRawLockMass::GetLockMassCorrectionApplied(bool& bApplied) const;					 

	///<summary>
	/// Checks if we can apply lock mass correction to the raw data
	///<\summary>
	///<param name='bCanApply'>
	/// true if lock mass correction can be applied
	///<\param>
	void MassLynxRawLockMass::CanApplyLockMassCorrection(bool& bCanApply) const;				

	///<summary>
	/// Gets the currently applied lock mass values
	///<\summary>
	///<param name='mass'>
	/// mass that was used to amke the lock mass corection
	///<\param>
	///<param name='tolerance'>
	/// tolerance that was used to make the lock mass corection +/-
	///<\param>
	void MassLynxRawLockMass::GetLockMassValues(float& mass, float& tolerance) const;					 

	///<summary>
	/// Gets the correction value based on a retention time
	///<\summary>
	///<param name='retentionTime'>
	/// retention time
	///<\param>
	///<param name='gain'>
	/// returned correcton factor 
	///<\param>
	void MassLynxRawLockMass::GetLockMassCorrection(const float& retentionTime, float& gain) const;	

	~MassLynxRawLockMass();

private:
	// internal implemenation object
	MLRawLockMassImp * m_pImp;
};

}   // MassLynxRaw
}   // Lib
}   // Waters
