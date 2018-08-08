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

package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.text.NumberFormat;
import java.util.Locale;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementsParameter;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class IsotopePeakScannerParameters extends SimpleParameterSet{
	
	public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    /*public static final IntegerParameter charge = new IntegerParameter(
	    "Charge", "Charge");*/

    /*public static final ComboParameter<IonizationType> ionization = new ComboParameter<IonizationType>(
	    "Ionization type", "Ionization type", IonizationType.values());*/

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();
    
    public static final BooleanParameter checkRT = new BooleanParameter("Check RT", "Compare rt of peaks to parent.");
    
    public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

    public static final StringParameter element = new StringParameter("Element pattern", "Element (combination) whose isotope pattern to be searched for. Please enter the two letter Symbol. (e.g. \"Gd\", \"Cl2Br\"S)");
    
    public static final PercentParameter minAbundance = new PercentParameter("Minimum abundance", "The minimum abundance (%) of Isotopes. Small values "
    		+ "might increase accuracy but will decrease sensitivity.");
    
    public static final DoubleParameter minHeight = new DoubleParameter("Minimum height", "Minimum peak height to be considered as an isotope peak.", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0);
    
    public static final DoubleParameter mergeFWHM = new DoubleParameter("Merge FWHM(m/z)", " Full width at half maximun of the relevant peaks.\nThis will be used to merge peaks in the calculated isotope pattern if they overlap in the spectrum.\n", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0005, 0.00001, 10.0);
   
    public static final DoubleParameter minPatternIntensity = new DoubleParameter("Min. pattern intensity", "The minimum intensity of a peak in the final calculated isotope pattern. Depends on the sensitivity of your MS.\nMin = 0.0, Max = 0.99...", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.01, 0.0, 0.99999);
    
    public static final BooleanParameter checkIntensity = new BooleanParameter("Check intensity", "Compare intesity of peaks to the natural abundance.");
        
    public static final DoubleParameter minRating = new DoubleParameter("Minimun rating", "Minimum rating to be considered as an isotope peak. min = 0.0, max = 1.0", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.95, 0.0, 1.0);
    
    public static final String[] ratingTypeChoices = {"Highest intensity", "Temporary average"};
    
    public static final ComboParameter<String> ratingChoices = new ComboParameter<String>("Rating type", "Method to calculate the rating with.\nHighest Intensity is the standard method and faster.\nAverage is slower but might be more accurate for more intense peaks. Select a masslist.", ratingTypeChoices);
    
    public static final StringParameter suffix =
    	      new StringParameter("Name suffix", "Suffix to be added to peak list name", "auto");
    
    public static final DoubleParameter neutralLoss = new DoubleParameter("Neutral loss? (Y/N)", "Enter exact mass if yes else leave 0.0", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0);
    
    public static final IntegerParameter charge = new IntegerParameter("Charge", "Amount and polarity (e.g.: [M+]=+1 / [M-]=-1");
    
    //public static final BooleanParameter tempAvgIntensity = new BooleanParameter("Temporary Average Intensity", "Use temporary avg intensity to calculate rating.", false);
    
    public static final OptionalParameter<MassListParameter> massList = new OptionalParameter<MassListParameter>(new MassListParameter("Calculate accurate average", "Please select a mass list.\nThis method will use averaged intensitys over all mass lists in which ALL relevant masses were detected in.\nThis will only be done for peaks that match the defined rating-calculation with the given rating.\nMake sure the mass list is contained in the peak list."));
    
    public IsotopePeakScannerParameters() {
    	super(new Parameter[] {PEAK_LISTS, mzTolerance, checkRT, rtTolerance, element, charge, minAbundance, minPatternIntensity, mergeFWHM,  minHeight, checkIntensity, minRating, ratingChoices, massList, suffix, neutralLoss});
    }
}
