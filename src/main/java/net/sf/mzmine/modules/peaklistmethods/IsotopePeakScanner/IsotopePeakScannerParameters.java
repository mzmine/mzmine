package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.text.NumberFormat;
import java.util.Locale;

import net.sf.mzmine.datamodel.IonizationType;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.elements.ElementsParameter;
import net.sf.mzmine.modules.peaklistmethods.identification.formulaprediction.restrictions.elements.ElementalHeuristicParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.MultiChoiceParameter;
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
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
    
    public static final BooleanParameter checkIntensity = new BooleanParameter("Check intensity", "Compare intesity of peaks to the natural abundance.");
        
    public static final DoubleParameter minRating = new DoubleParameter("Minimun rating", "Minimum rating to be considered as an isotope peak. min = 0.0, max = 1.0", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.95, 0.0, 1.0);
    
    public static final StringParameter suffix =
    	      new StringParameter("Name suffix", "Suffix to be added to peak list name", "auto");
    
    public static final DoubleParameter neutralLoss = new DoubleParameter("Neutral loss? (Y/N)", "Enter exact mass if yes else leave 0.0", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.0);
    
    public static final IntegerParameter charge = new IntegerParameter("Charge", "Amount and polarity (e.g.: [M+]=+1 / [M-]=-1");
    
    
    public IsotopePeakScannerParameters()
    {
    	super(new Parameter[] {PEAK_LISTS, mzTolerance, checkRT, rtTolerance, element, charge, minAbundance, minHeight, checkIntensity, minRating, suffix, neutralLoss});
    }
}
