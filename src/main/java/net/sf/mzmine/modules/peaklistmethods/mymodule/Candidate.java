package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.util.ArrayList;

import org.jmol.util.Logger;
import org.openscience.cdk.interfaces.IIsotope;

import net.sf.mzmine.datamodel.PeakListRow;

public class Candidate {
	private IIsotope isotope;
	private int row;
	private int parentRow;
	private double rating;
	
	public IIsotope getIsotope() {
		return isotope;
	}
	public void setIsotope(IIsotope isotope) {
		this.isotope = isotope;
	}
	public int getRow() {
		return row;
	}
	public void setRow(int row) {
		this.row = row;
	}
	public int getParentRow() {
		return parentRow;
	}
	public void setParentRow(int parentRow) {
		this.parentRow = parentRow;
	}
	
	/**
	 * will check if parentMZ + diffMZ is close to candMZ 
	 * @param parentMZ peak of monoisotopic mass
	 * @param candMZ
	 * @param mzDiff Difference added by isotope mass
	 * @return true if candMZ is a better fit
	 */
	public boolean checkForBetterRating(double parentMZ, double candMZ, double mzDiff)
	{
		double tempRating = candMZ / (parentMZ + mzDiff);
		
		if(tempRating > 1.0) // 0.99 and 1.01 should be comparable
		{
			tempRating -= 1.0; 
			tempRating = 1 - tempRating;
		}
		if(tempRating > 1.0 || tempRating < 0.0)
		{
			Logger.info("ERROR: tempRating > 1 or < 0.\ttempRating: " + tempRating); // TODO: can you do this without creating a new logger?
			return false;
		}		
		if(tempRating > rating)
		{
			rating = tempRating;
			return true;
		}
		return false;
	}
	
	public double calcIntensityDeviation(ArrayList<PeakListRow> pL, int parentindex, int candindex, IIsotope[] isotopes, int isotopenum, double maxDeviation)
	{
		PeakListRow parent = pL.get(parentindex);
		PeakListRow cand = pL.get(candindex);
		
		double x = isotopes[isotopenum].getNaturalAbundance() / isotopes[0].getNaturalAbundance();
		
		return ( (x * parent.getAverageArea()) / cand.getAverageArea() );
	}
	
	public boolean checkForBetterRating(ArrayList<PeakListRow> pL, int parentindex, int candindex, IIsotope[] isotopes, int isotopenum, double maxDeviation)
	{
		double parentMZ = pL.get(parentindex).getAverageMZ();
		double candMZ = pL.get(candindex).getAverageMZ();
		double mzDiff = isotopes[isotopenum].getExactMass() - isotopes[0].getExactMass();
		
		double tempRating = candMZ / (parentMZ + mzDiff);
		double deviation = calcIntensityDeviation(pL, parentindex, candindex, isotopes, isotopenum, maxDeviation);
		
		if(deviation > 1.0)
		{
			deviation -= 1.0;
			deviation = 1 - deviation;
		}
		
		if(tempRating > 1.0) // 0.99 and 1.01 should be comparable
		{
			tempRating -= 1.0; 
			tempRating = 1 - tempRating;
		}
		
		if(deviation > 1.0 || deviation <= 0.0 || tempRating > 1.0 || tempRating < 0.0)
		{
			Logger.debug("ERROR: tempRating or deviation > 1 or < 0.\ttempRating: " + tempRating + "\tdeviation: " + deviation);  // TODO: can you do this without creating a new logger?
			return false;
		}
		
		tempRating *= deviation;
		
		if(tempRating > rating)
		{
			rating = tempRating;
			
			this.setParentRow(parentindex);
			this.setRow(candindex);
			this.setIsotope(isotopes[isotopenum]);
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
		parentRow = 0;
		rating = 0.0;
		//isotope = 0;
	}
}
