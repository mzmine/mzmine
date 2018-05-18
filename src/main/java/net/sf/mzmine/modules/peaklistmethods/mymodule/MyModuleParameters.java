package net.sf.mzmine.modules.peaklistmethods.mymodule;

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
import net.sf.mzmine.parameters.parametertypes.OptionalModuleParameter;
import net.sf.mzmine.parameters.parametertypes.PercentParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class MyModuleParameters extends SimpleParameterSet{
	
	public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();

    /*public static final IntegerParameter charge = new IntegerParameter(
	    "Charge", "Charge");*/

    /*public static final ComboParameter<IonizationType> ionization = new ComboParameter<IonizationType>(
	    "Ionization type", "Ionization type", IonizationType.values());*/

    public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();
    
    public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

    /*public static final ElementsParameter elements = new ElementsParameter(
	    "Elements", "Elements and ranges");

    public static final OptionalModuleParameter elementalRatios = new OptionalModuleParameter(
	    "Element count heuristics",
	    "Restrict formulas by heuristic restrictions of elemental counts and ratios",
	    new ElementalHeuristicParameters());*/

    public static final StringParameter element = new StringParameter("Element", "Element whose isotope pattern to be searched for.");
    
    public static final PercentParameter minAbundance = new PercentParameter("Minimum abundance", "The minimum abundance (%) of Isotopes. Small values "
    		+ "might increase accuracy but will decrease sensitivity.");
    public static final BooleanParameter checkIntensity = new BooleanParameter("Check intensity", "Compare intesity of peaks to the natural abundance.");
    
    public static final DoubleParameter intensityDeviation = new DoubleParameter("Intesity deviation", "Maximum (%) the intensity may deviate from the natural abundance");
    
    public static final DoubleParameter minRating = new DoubleParameter("Minimun rating", "Minimum rating to be considered as an isotope peak. min = 0.0, max = 1.0", NumberFormat.getNumberInstance(Locale.ENGLISH), 0.95, 0.0, 1.0);
    
    public MyModuleParameters()
    {
    	super(new Parameter[] {PEAK_LISTS, mzTolerance, rtTolerance, element, minAbundance, checkIntensity, minRating, intensityDeviation});
    }
}
