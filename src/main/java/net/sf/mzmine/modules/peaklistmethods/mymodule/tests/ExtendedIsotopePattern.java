package net.sf.mzmine.modules.peaklistmethods.mymodule.tests;

import java.io.IOException;
import java.util.ArrayList;

/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

import javax.annotation.Nonnull;

import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.interfaces.IElement;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.datamodel.PolarityType;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

/**
 * Simple implementation of IsotopePattern interface
 */
public class ExtendedIsotopePattern implements IsotopePattern {

	private static final double ELECTRON_MASS = 5.4857990943E-4;
    private ArrayList<DataPoint> dataPoints;
    private ArrayList<String> dpDescr;
    private DataPoint highestPeak;
    private IsotopePatternStatus status;
    private String description;
    private Range<Double> mzRange;
    private ArrayList<String> descr;
    IMolecularFormula formula;
    IChemObjectBuilder builder;
    Isotopes ifac;
    
    public ExtendedIsotopePattern() {
		builder = SilentChemObjectBuilder.getInstance();
		dataPoints = new ArrayList<DataPoint>();
		dataPoints.add(new SimpleDataPoint(0, 1));
		try {
			ifac = Isotopes.getInstance();
		}catch (IOException e) {
			e.printStackTrace();
	    }
    }
    /**
     * Will create isotope pattern, can be accessed via getDataPoints and getSimple/PreciseDescr.
     * @param sumFormula
     * @param minAbundance the minimum abundance of a peak in finished pattern
     */
   public void setUpFromFormula(String sumFormula, double minAbundance)
    {
    	IMolecularFormula form = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(sumFormula, builder);
    	description = sumFormula;
    	formula = form;
    	status = IsotopePatternStatus.PREDICTED;
    	addMolecule(form);
    	removePeaksBelowAbundance(minAbundance);
    }
    
    private void addMolecule(IMolecularFormula molecule)
    {    												//.isotopes() will give the major isotope of each element contained in the formula
    	for(IIsotope element : molecule.isotopes())		//so we loop through every element
    		addElement(element, molecule.getIsotopeCount(element));	//and the number of each element
    }
    
    private void addElement(IIsotope element, int count)
    {
    	IIsotope[] isotopes = ifac.getIsotopes(element.getSymbol());
    	for(int i = 0; i < count; i++)	//add each element "count" times to the pattern
    		addIsotopes(isotopes);
    }
    
    private void addIsotopes(IIsotope[] isotopes)
    {
    	ArrayList<DataPoint> newDp = new ArrayList<DataPoint>();
    	ArrayList<String> newDpDescr = new ArrayList<String>();
    	for(int i = 0; i < dataPoints.size(); i++)	//add every isotope to every data point
    	{
    		for(IIsotope iso : isotopes)
    		{
    			if(iso.getNaturalAbundance() < 0.0001)
    				continue;
    			
	    		newDp.add(new SimpleDataPoint(dataPoints.get(i).getMZ() + iso.getExactMass(),
	    				dataPoints.get(i).getIntensity() * iso.getNaturalAbundance()/100 ));
	    		//System.out.println(iso.getMassNumber() + iso.getSymbol());
	    		
	    		if(dpDescr != null)
	    			newDpDescr.add(dpDescr.get(i) + "/" + iso.getMassNumber() + iso.getSymbol() );
	    		else
	    			newDpDescr.add("/" + iso.getMassNumber() + iso.getSymbol());
    		}
    	}
    	
    	dataPoints = newDp;
    	dpDescr = newDpDescr;
    	
    	mergeDuplicates();
    	normalizePatternToHighestPeak();
    }
    
    /**
     * Normalizes max-intensity-peak to 1
     */
    public void normalizePatternToHighestPeak()
    {
    	ArrayList<DataPoint> newDp = new ArrayList<DataPoint>();
    	updateHighestPeak();
    	double maxIntensity = highestPeak.getIntensity();
    	
    	for(int i = 0; i < dataPoints.size(); i++)
    		newDp.add(new SimpleDataPoint(dataPoints.get(i).getMZ(),
    				dataPoints.get(i).getIntensity() / maxIntensity));
    	dataPoints = newDp;
    	
    	updateHighestPeak();
    }
    /**
     * Normalizes the whole pattern to peak peakNum.
     * @param peakNum Index of the peak.
     */
    public void normalizePatternToPeak(int peakNum)
    {
    	ArrayList<DataPoint> newDp = new ArrayList<DataPoint>();
    	double normIntensity = dataPoints.get(peakNum).getIntensity();
    	
    	for(int i = 0; i < dataPoints.size(); i++)
    		newDp.add(new SimpleDataPoint(dataPoints.get(i).getMZ(),
    				dataPoints.get(i).getIntensity() / normIntensity));
    	dataPoints = newDp;
    	
    	updateHighestPeak();
    }
    
    /**
     * Merges duplicate peaks. (35Cl + 37Cl will be merged with 37Cl + 35Cl)
     */
    private void mergeDuplicates()
    {
    	ArrayList<DataPoint> newDp = new ArrayList<DataPoint>();
    	ArrayList<String> newDpDescr = new ArrayList<String>();
    	
    	for(int i = 0; i < dataPoints.size(); i++)
    	{
    		for(int j = 0; j < dataPoints.size(); j++)
    		{
    			if(dataPoints.get(j) == null || dataPoints.get(i) == null || i == j)
    				continue;
    			if(Math.abs(dataPoints.get(i).getMZ() - dataPoints.get(j).getMZ()) < 0.00000001)
    			{
    				double newIntensity = dataPoints.get(i).getIntensity() + dataPoints.get(j).getIntensity();
    				dataPoints.set(i, new SimpleDataPoint(
    					( dataPoints.get(i).getMZ() * dataPoints.get(i).getIntensity()
    					+ dataPoints.get(j).getMZ() * dataPoints.get(j).getIntensity() )
    					/ newIntensity, newIntensity));
    				
    				dataPoints.set(j, null);
    				dpDescr.set(j, null);	//we dont want to merge here since its the !same! peak
    			}
    		}
    	}
    	
    	for(int i = 0; i < dataPoints.size(); i++)
    		if(dataPoints.get(i) != null)
    		{
    			newDp.add(dataPoints.get(i));
    			newDpDescr.add(dpDescr.get(i));
    		}
    	
    	dataPoints = newDp;
    	dpDescr = newDpDescr;
    	
    	normalizePatternToHighestPeak();
//    	updateHighestPeak();
    }
    /**
     * Merges peaks withing given mzTolerance.
     * @param mzTolerance Absolute tolerance range.
     */
    public void mergePeaks(double mzTolerance)	// totally based on mergeIsotopes in IsotopePatternCalculator
    {
    	ArrayList<DataPoint> newDp = new ArrayList<DataPoint>();
    	
    	for(int i = 0; i < dataPoints.size()-1; i++)
    	{
    		if(Math.abs(dataPoints.get(i).getMZ() - dataPoints.get(i+1).getMZ()) < mzTolerance)
    		{
    			double newIntensity = dataPoints.get(i).getIntensity() + dataPoints.get(i+1).getIntensity();
    			//newDp.add(new SimpleDataPoint(dataPoints.get(index)))
    			dataPoints.set(i+1, new SimpleDataPoint( 
    					Math.abs(( dataPoints.get(i).getMZ() * dataPoints.get(i).getIntensity()
    					+ dataPoints.get(i+1).getMZ() * dataPoints.get(i+1).getIntensity() )
    					/ newIntensity), newIntensity)); // set it to i+1 first, we might have to merge more than one Peak
    			dataPoints.set(i, null);
    			
    			mergeDescription(i, i+1);
    		}
    	}
    	
    	for(int i = 0; i < dataPoints.size(); i++)
    		if(dataPoints.get(i) != null)
    			newDp.add(dataPoints.get(i));
    	
    	dataPoints = newDp;
    	
    	normalizePatternToHighestPeak();
//    	updateHighestPeak();
    }
    
    /**
     * Merges peak Description. Result will be /37Cl/12C/12C/ + 35Cl/13C/13C/ 
     * @param peak1
     * @param peak2
     */
    private void mergeDescription(int peak1, int peak2)
    {
    	ArrayList<String> newDpDescr = new ArrayList<String>();
    	for(int i = 0; i < dpDescr.size(); i++)
    	{
    		if(i==peak1)
    			newDpDescr.add(dpDescr.get(peak1) + "/ + /" + dpDescr.get(peak2));
    		else if(i == peak2)
    			continue;
    		else
    			newDpDescr.add(dpDescr.get(i));
    	}
    	dpDescr = newDpDescr;
    }
    
    /**
     * Removes peaks below given intensity.  
     * @param minAbundance Threshold min=0.0, max=1.0
     */
    private void removePeaksBelowAbundance(double minAbundance)
    {
    	ArrayList<DataPoint> newDp = new ArrayList<DataPoint>();
    	ArrayList<String> newDpDescr = new ArrayList<String>();
    	
    	normalizePatternToHighestPeak();
    	
    	for(int i = 0; i < dataPoints.size(); i++)
    	{
    		if(dataPoints.get(i).getIntensity() < minAbundance)
    		{
    			dataPoints.set(i, null);
    			dpDescr.set(i, null);
    		}
    	}
    	
    	for(int i = 0; i < dataPoints.size(); i++)
    		if(dataPoints.get(i) != null)
    		{
    			newDp.add(dataPoints.get(i));
    			newDpDescr.add(dpDescr.get(i));
    		}
    	
    	dataPoints = newDp;
    	dpDescr = newDpDescr;
    	
    	normalizePatternToHighestPeak();
    }
    /**
     * Adds polarity and charge 
     * WANRING: ONLY USE THIS METHOD WHEN YOU'RE DONE AND DONT WANT TO ADD ANY MORE ELEMENTS TO THE PATTERN
     * @param charge
     * @param polarityType
     */
    public void applyCharge(int charge, PolarityType polarityType)	// totally based on IsotopePatternCalculator
    {
    	ArrayList<DataPoint> newDp = new ArrayList<DataPoint>();
    	
    	for(int i = 0; i < dataPoints.size(); i++)
    	{
    		double newMass = dataPoints.get(i).getMZ() + (polarityType.getSign() * -1 * charge * ELECTRON_MASS);
    		
    		if(charge != 0)
    			newMass /= charge;
    		
    		newDp.add(new SimpleDataPoint(newMass, dataPoints.get(i).getIntensity()));
    	}
    	dataPoints = newDp;
    }
    
    @Override
    public @Nonnull DataPoint[] getDataPoints() {
    	DataPoint[] dp = new DataPoint[dataPoints.size()];
    	for(int i = 0; i < dataPoints.size(); i++)
    		dp[i] = new SimpleDataPoint(dataPoints.get(i));
    	return dp;
    }
    /**
     * 
     * @param peakNum Index of the Peak.
     * @return Peak description like /37Cl/12C/12C/. If merged then /37Cl/12C/12C/ + 35Cl/13C/13C/ 
     */
    public String getExplicitPeakDescription(int peakNum)
    {
    	if(dpDescr.size() < peakNum)
    		return null;
    	return dpDescr.get(peakNum);
    }
    /**
     * Note: If Peaks werge merged, only the first description will be analyzed. Use getExplicitPeakDescription then.
     * @param peakNum
     * @return Peak description such as ^37Cl1_^12C2 or ^35Cl1_^13C2
     */
    public String getSimplePeakDescription(int peakNum)
    {
    	String descr = getExplicitPeakDescription(peakNum);
    	String[] cut = descr.split("/");
    	
    	String simpleDescr = "";
    	
    	for(IIsotope element : formula.isotopes())
    	{
    		String symbol = element.getSymbol();
    		IIsotope[] isotopes = ifac.getIsotopes(element.getSymbol());
    		int[] isotopeCount = new int[isotopes.length];
    		
    		for(int i = 0; i < isotopes.length; i++)
    		{
    			isotopeCount[i] = 0;
    			if(isotopes[i].getNaturalAbundance() < 0.0001)
    				continue;
    			
    			for(int j = 0; j < cut.length; j++)
    			{
    				if(cut[j].equals(isotopes[i].getMassNumber() + symbol))
    					isotopeCount[i]++;
    				if(cut[j].equals(" + "))	//maybe description was merged, so dont want to count it double
    					break;
    			}
    			if(isotopeCount[i] != 0 && !(symbol.equals("C") && isotopes[i].getMassNumber() == 12))
    				simpleDescr += "^" + isotopes[i].getMassNumber() + symbol + isotopeCount[i] + "_";
    		}
    	}
    	return simpleDescr;
    }
    
    public void sortByAscendingMZ()
    {
    	ArrayList<DataPoint> newDp = new ArrayList<DataPoint>();
    	ArrayList<String> newDpDescr = new ArrayList<String>();
    	
    	for(int i = 0; i < dataPoints.size(); i++)
    	{
    		for(int j = 0; j < newDp.size(); j++)
    		{
    			if(newDp.size() < i)
    			{
    				newDp.add(dataPoints.get(i));
    				newDpDescr.add(dpDescr.get(i));
    			}
    			else if(dataPoints.get(i).getMZ() > newDp.get(j).getMZ())
    			{
    				newDp.add(j, dataPoints.get(i));
    				newDpDescr.add(j, dpDescr.get(i));
    			}
    			else
    			{
    				newDp.add(dataPoints.get(i));
    				newDpDescr.add(dpDescr.get(i));

    			}
    		}
    	}
    	dataPoints = newDp;
    	dpDescr = newDpDescr;
    	
    	updateHighestPeak();
    }

    @Override
    public int getNumberOfDataPoints() {
	return dataPoints.size();
    }

    @Override
    public @Nonnull IsotopePatternStatus getStatus() {
	return status;
    }

    @Override
    public @Nonnull DataPoint getHighestDataPoint() {
    	updateHighestPeak();
    	return highestPeak;
    }
    
    private void updateHighestPeak()
    {
    	DataPoint max = dataPoints.get(0);
    	if(max == null)
    		return;
    	for(int i = 0; i < dataPoints.size(); i++)
    		if(dataPoints.get(i).getIntensity() > max.getIntensity())
    			max = dataPoints.get(i);
    	highestPeak = max;
    }

    @Override
    public @Nonnull String getDescription() {
	return description;
    }

    @Override
    public String toString() {
	return "Isotope pattern: " + description;
    }

    @Override
    @Nonnull
    public Range<Double> getDataPointMZRange() {
	return mzRange;
    }

    @Override
    public double getTIC() {
	return 0;
    }

    @Override
    public MassSpectrumType getSpectrumType() {
	return MassSpectrumType.CENTROIDED;
    }

    @Override
    @Nonnull
    public DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {
	throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public DataPoint[] getDataPointsOverIntensity(double intensity) {
	throw new UnsupportedOperationException();
    }

}