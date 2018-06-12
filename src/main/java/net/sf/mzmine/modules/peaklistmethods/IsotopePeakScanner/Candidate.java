package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.util.ArrayList;

import org.jmol.util.Logger;
import org.openscience.cdk.interfaces.IIsotope;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.PeakListRow;

public class Candidate {

	private int peakNum; // only used for patterns, temporary use only TODO
	private int rowNum, candID; //row represents index in groupedPeaks list, candID is ID in original PeakList
	private double rating;
	private double mz;
	private double height;
	private PeakListRow row;
	

	/**
	 * 
	 * @return row in groupedPeaks
	 */
	public double getMZ()
	{
		return mz;
	}
	public int getRow() {
		return rowNum;
	}
	public void setRowNum(int rowNum) {
		this.rowNum = rowNum;
	}
	public int getCandID() {
		return candID;
	}
	public void setCandID(int candID) {
		this.candID = candID;
	}
	
	public double calcIntensityAccuracy_Pattern(ArrayList<PeakListRow> pL, int parentindex, int candindex, DataPoint pParent, DataPoint pChild)
	{
		PeakListRow parent = pL.get(parentindex);
		PeakListRow cand = pL.get(candindex);
		
		double idealIntensity = pChild.getIntensity() / pParent.getIntensity();
		return ( (idealIntensity * parent.getAverageArea()) / cand.getAverageArea() );
	}
	
	public boolean checkForBetterRating(ArrayList<PeakListRow> pL, int parentindex, int candindex, IsotopePattern pattern, int peakNum, double minRating, boolean checkIntensity)
	{			
		double parentMZ = pL.get(parentindex).getAverageMZ();
		double candMZ = pL.get(candindex).getAverageMZ();
		DataPoint[] points = pattern.getDataPoints();
		double mzDiff = points[peakNum].getMZ() - points[0].getMZ();
		
		double tempRating = candMZ / (parentMZ + mzDiff);
		double intensAcc = 0;
		
		if(tempRating > 1.0) // 0.99 and 1.01 should be comparable
			tempRating = 1 / tempRating;
		
		if(checkIntensity)
		{
			intensAcc = calcIntensityAccuracy_Pattern(pL, parentindex, candindex, points[0], points[peakNum]);
					
			if(intensAcc > 1.0) // 0.99 and 1.01 should be comparable
				intensAcc = 1 / intensAcc;
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

			row = pL.get(candindex);
			mz = row.getAverageMZ();
			height = row.getAverageHeight();
			
			this.setRowNum(candindex);
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
			
			row = pL.get(candindex);
			mz = row.getAverageMZ();
			height = row.getAverageHeight();

			this.setRowNum(candindex);
			this.setCandID(pL.get(candindex).getID());
			return true;
		}
		return false;
	}
	
	public double recalcRatingWithAvgIntensities(double parentMZ, IsotopePattern pattern, int peakNum, double[] avgIntensity) // TODO no pL here!
	{
		double candMZ = this.mz;
		DataPoint[] points = pattern.getDataPoints();
		double mzDiff = points[peakNum].getMZ() - points[0].getMZ();
		
		double tempRating = candMZ / (parentMZ + mzDiff);
		double intensAcc = 0;
		
		if(tempRating > 1.0) // 0.99 and 1.01 should be comparable
			tempRating = 1 / tempRating;
		
		intensAcc = calcIntensityAccuracy_Avg(avgIntensity[0], avgIntensity[peakNum], points[0], points[peakNum]);
					
		if(intensAcc > 1.0) // 0.99 and 1.01 should be comparable
			intensAcc = 1 / intensAcc;
		
		if(intensAcc > 1.0 || intensAcc < 0.0 || tempRating > 1.0 || tempRating < 0.0)
		{
			Logger.debug("ERROR: tempRating or deviation > 1 or < 0.\ttempRating: " + tempRating + "\tintensAcc: " + intensAcc);  // TODO: can you do this without creating a new logger?
			return 0; 
		}
		
		tempRating = intensAcc * tempRating;
		
		//rating = tempRating;
		return tempRating;
	}
	
	private double calcIntensityAccuracy_Avg(double iParent, double iChild, DataPoint pParent, DataPoint pChild)
	{
		double idealIntensity = pChild.getIntensity() / pParent.getIntensity();
		return idealIntensity * iParent / iChild ;
	}
	
	public double getRating()
	{
		return rating;
	}

	Candidate()
	{
		rowNum = 0;
		candID = 0;
		rating = 0.0;
	}
}
