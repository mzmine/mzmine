package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.util.ArrayList;

import org.jmol.util.Logger;
import org.openscience.cdk.interfaces.IIsotope;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakListRow;

public class Candidate {
	private IIsotope isotope; //only used for single atom patterns
	private int peakNum; // only used for patterns, temporary use only TODO
	private int row, candID; //row represents index in groupedPeaks list, candID is ID in original PeakList
	private int parentID;
	private double rating;
	
	
	public IIsotope getIsotope() {
		return isotope;
	}
	public void setIsotope(IIsotope isotope) {
		this.isotope = isotope;
	}
	/**
	 * 
	 * @return row in groupedPeaks
	 */
	public int getRow() {
		return row;
	}
	public void setRow(int row) {
		this.row = row;
	}
	public int getParentID() {
		return parentID;
	}
	public void setParentID(int parentID) {
		this.parentID = parentID;
	}
	public int getCandID() {
		return candID;
	}
	public void setCandID(int candID) {
		this.candID = candID;
	}
	
	
	public double calcIntensityAccuracy(ArrayList<PeakListRow> pL, int parentindex, int candindex, IIsotope[] isotopes, int isotopenum)
	{
		PeakListRow parent = pL.get(parentindex);
		PeakListRow cand = pL.get(candindex);
		
		double idealIntensity = isotopes[isotopenum].getNaturalAbundance() / isotopes[0].getNaturalAbundance();
		//TODO sth seems to be wrong here
		return ( (idealIntensity * parent.getAverageArea()) / cand.getAverageArea() );
	}
	/**
	 * will check if parentMZ + diffMZ is close to candMZ 
	 * @return true if candMZ is a better fit
	 */
	public boolean checkForBetterRating(ArrayList<PeakListRow> pL, int parentindex, int candindex, IIsotope[] isotopes, int isotopenum, double maxDeviation, double minRating, boolean checkIntensity)
	{			
		double parentMZ = pL.get(parentindex).getAverageMZ();
		double candMZ = pL.get(candindex).getAverageMZ();
		double mzDiff = isotopes[isotopenum].getExactMass() - isotopes[0].getExactMass();
		
		double tempRating = candMZ / (parentMZ + mzDiff);
		double intensAcc = 0;
		
		if(tempRating > 1.0) // 0.99 and 1.01 should be comparable
		{
			tempRating -= 1.0; 
			tempRating = 1 - tempRating;
		}
		
		if(checkIntensity)
		{
			intensAcc = calcIntensityAccuracy(pL, parentindex, candindex, isotopes, isotopenum);
					
			if(intensAcc > 1.0) // 0.99 and 1.01 should be comparable
			{
				intensAcc -= 1.0;
				intensAcc = 1 - intensAcc;
			}
			
			if(intensAcc < (1 - maxDeviation))
				return false;
		}
		
		if(intensAcc > 1.0 || intensAcc < 0.0 || tempRating > 1.0 || tempRating < 0.0)
		{
			Logger.debug("ERROR: tempRating or deviation > 1 or < 0.\ttempRating: " + tempRating + "\tintensAcc: " + intensAcc);  // TODO: can you do this without creating a new logger?
			return false;
		}
		
		if(checkIntensity)
			tempRating = intensAcc * tempRating;
		
		if(tempRating > rating && tempRating >= minRating)
		{
			rating = tempRating;
			
			this.setParentID(parentindex);
			this.setRow(candindex);
			this.setCandID(pL.get(candindex).getID());
			this.setIsotope(isotopes[isotopenum]);
			return true;
		}
		return false;
	}
	
	public double calcIntensityAccuracy_Pattern(ArrayList<PeakListRow> pL, int parentindex, int candindex, DataPoint pParent, DataPoint pChild)
	{
		PeakListRow parent = pL.get(parentindex);
		PeakListRow cand = pL.get(candindex);
		
		double idealIntensity = pChild.getIntensity() / pParent.getIntensity();
		return ( (idealIntensity * parent.getAverageArea()) / cand.getAverageArea() );
	}
	
	public boolean checkForBetterRating(ArrayList<PeakListRow> pL, int parentindex, int candindex, IsotopePattern pattern, int peakNum, double maxDeviation, double minRating, boolean checkIntensity)
	{			
		double parentMZ = pL.get(parentindex).getAverageMZ();
		double candMZ = pL.get(candindex).getAverageMZ();
		DataPoint[] points = pattern.getDataPoints();
		double mzDiff = points[peakNum].getMZ() - points[0].getMZ();
		
		double tempRating = candMZ / (parentMZ + mzDiff);
		double intensAcc = 0;
		
		if(tempRating > 1.0) // 0.99 and 1.01 should be comparable
		{
			tempRating -= 1.0; 
			tempRating = 1 - tempRating;
		}
		
		if(checkIntensity)
		{
			intensAcc = calcIntensityAccuracy_Pattern(pL, parentindex, candindex, points[0], points[peakNum]);
					
			if(intensAcc > 1.0) // 0.99 and 1.01 should be comparable
			{
				intensAcc -= 1.0;
				intensAcc = 1 - intensAcc;
			}
			
			//if(intensAcc < (1 - maxDeviation))
				//return false;
		}
		
		if(intensAcc > 1.0 || intensAcc < 0.0 || tempRating > 1.0 || tempRating < 0.0)
		{
			
			Logger.debug("ERROR: tempRating or deviation > 1 or < 0.\ttempRating: " + tempRating + "\tintensAcc: " + intensAcc);  // TODO: can you do this without creating a new logger?
			return false; 
		}
		
		if(checkIntensity)
			tempRating = intensAcc * tempRating;
		
		if(tempRating > rating && tempRating >= minRating)
		{
			rating = tempRating;
			
			this.setParentID(parentindex);
			this.setRow(candindex);
			this.setCandID(pL.get(candindex).getID());
			//this.setIsotope(isotopes[isotopenum]);
			return true;
		}
		return false;
	}
	
	public boolean checkForBetterRating(ArrayList<PeakListRow> pL, int parentindex, int candindex, double mzDiff, double minRating)
	{			
		double parentMZ = pL.get(parentindex).getAverageMZ();
		double candMZ = pL.get(candindex).getAverageMZ();
		
		double tempRating = candMZ / (parentMZ + mzDiff);
		double intensAcc = 0;
		
		if(tempRating > 1.0) // 0.99 and 1.01 should be comparable
		{
			tempRating -= 1.0; 
			tempRating = 1 - tempRating;
		}
		
		if(intensAcc > 1.0 || intensAcc < 0.0 || tempRating > 1.0 || tempRating < 0.0)
		{
			Logger.debug("ERROR: tempRating > 1 or < 0.\ttempRating: " + tempRating);  // TODO: can you do this without creating a new logger?
			return false;
		}
		
		if(tempRating > rating && tempRating >= minRating)
		{
			rating = tempRating;
			
			this.setParentID(parentindex);
			this.setRow(candindex);
			this.setCandID(pL.get(candindex).getID());
			return true;
		}
		return false;
	}
	
	public double getRating()
	{
		return rating;
	}

	Candidate()
	{
		row = 0;
		parentID = 0;
		rating = 0.0;
		//isotope = 0;
	}
}
