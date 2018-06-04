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
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

/**
 * Simple implementation of IsotopePattern interface
 */
public class ExtendedIsotopePattern implements IsotopePattern {

    private ArrayList<DataPoint> dataPoints;
    private DataPoint highestIsotope;
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

   public void addElement(String element_count)
    {
    	IMolecularFormula form = MolecularFormulaManipulator.getMajorIsotopeMolecularFormula(element_count, builder);
    	addElement(form);
    }
    
    private void addElement(IMolecularFormula element)
    {
    	ArrayList<DataPoint> dp_new = new ArrayList<DataPoint>();
    	
    	for(IIsotope iso : element.isotopes())
    	{
    		IIsotope[] isos = ifac.getIsotopes(iso.getSymbol());
    		
	    	for(int j = 0; j < dataPoints.size(); j++)
	    	{
	    		for(int i = 0; i < element.getIsotopeCount(iso); i++)
	        	{
	    			for(int k = 0; k < isos.length; k++)
	    				dp_new.add(new SimpleDataPoint(dataPoints.get(j).getMZ() + isos[k].getExactMass(),
	    					dataPoints.get(j).getIntensity() * isos[k].getNaturalAbundance()));
	    		}
	    		dataPoints = dp_new;
	    		dp_new = new ArrayList<DataPoint>();
    		}
    	}
    }
    
    @Override
    public @Nonnull DataPoint[] getDataPoints() {
    	DataPoint[] dp = new DataPoint[dataPoints.size()];
    	for(int i = 0; i < dataPoints.size(); i++)
    		dp[i] = new SimpleDataPoint(dataPoints.get(i));
    	return dp;
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
	return highestIsotope;
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