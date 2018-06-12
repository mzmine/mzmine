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
    
    public static final BooleanParameter checkRT = new BooleanParameter("Check rt", "Compare rt of peaks to parent.");
    
    public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

    public static final StringParameter element = new StringParameter("Element pattern", "Element (combibation) whose isotope pattern to be searched for. Please enter the two letter Symbol. (e.g. \"Gd\", Cl2Br)");
    
    public static final PercentParameter minAbundance = new PercentParameter("Minimum abundance", "The minimum abundance (%) of Isotopes. Small values "
    		+ "might increase accuracy but will decrease sensitivity.");
    
    public static final DoubleParameter minHeight = new DoubleParameter("Minimum height", "Minimum peak height to be considered as an isotope peak.", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0);
    
    public static final DoubleParameter mergeFWHM = new DoubleParameter("Merge FWHM(m/z)", " Full width at half maximun of the relevant peaks.\nThis will be used to merge peaks in the calculated isotope pattern if they overlap in the spectrum.\n", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0003, 0.000001, 10.0);
   
    public static final DoubleParameter minPatternIntensity = new DoubleParameter("Min. pattern intensity", "The minimum intensity of a peak in the final calculated isotope pattern. Depends on the sensitivity of your MS.\nMin = 0.0, Max = 0.99...", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.01, 0.0, 0.99999);
    
    public static final BooleanParameter checkIntensity = new BooleanParameter("Check intensity", "Compare intesity of peaks to the natural abundance.");
        
    public static final DoubleParameter minRating = new DoubleParameter("Minimun rating", "Minimum rating to be considered as an isotope peak. min = 0.0, max = 1.0", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.95, 0.0, 1.0);
    
    public static final StringParameter suffix =
    	      new StringParameter("Name suffix", "Suffix to be added to peak list name", "auto");
    
    public static final DoubleParameter neutralLoss = new DoubleParameter("Neutral loss? (Y/N)", "Enter exact mass if yes else leave 0.0", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0);
    
    public static final IntegerParameter charge = new IntegerParameter("Charge", "Amount and polarity (e.g.: [M+]=+1 / [M-]=-1");
    
    //public static final BooleanParameter avgIntensity = new BooleanParameter("Average Intensity", "Use avg Intensity to calculate final rating.");
    
    public static final OptionalParameter<MassListParameter> massList = new OptionalParameter<MassListParameter>(new MassListParameter("Average Intensity", "Please select a mass list.\nThis method will use averaged intensitys over all scans the relevant masses were detected in. This will only be done for peaks that match the defined rating-calculation with the given rating."));
    
    
    public IsotopePeakScannerParameters() {
    	super(new Parameter[] {PEAK_LISTS, mzTolerance, checkRT, rtTolerance, element, minPatternIntensity, mergeFWHM, charge, minAbundance, minHeight, checkIntensity, minRating, suffix, neutralLoss, massList});
    }
}
