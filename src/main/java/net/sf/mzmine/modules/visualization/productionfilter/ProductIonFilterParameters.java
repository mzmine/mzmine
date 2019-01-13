package net.sf.mzmine.modules.visualization.productionfilter;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
//import net.sf.mzmine.parameters.parametertypes.ranges.ListDoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.MZRangeParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.RTRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

import net.sf.mzmine.parameters.parametertypes.ListDoubleParameter;

public class ProductIonFilterParameters extends SimpleParameterSet {

	public static final String xAxisPrecursor = "Precursor mass";
	public static final String xAxisRT = "Retention time";

	public static final String[] xAxisTypes = { xAxisPrecursor, xAxisRT };

	public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

	public static final ComboParameter<String> xAxisType = new ComboParameter<String>("X axis", "X axis type",
			xAxisTypes);

	public static final RTRangeParameter retentionTimeRange = new RTRangeParameter();

	public static final MZRangeParameter mzRange = new MZRangeParameter("Precursor m/z",
			"Range of precursor m/z values");


    public static final MZToleranceParameter mzDifference = new MZToleranceParameter();
	
    public static final ListDoubleParameter targetedMZ_List =
            new ListDoubleParameter("Diagnostic product ions (m/z)", 
                    "Product m/z-values that must be included in MS/MS", 
                    false, null);
    
    public static final ListDoubleParameter targetedNF_List = 
    		new ListDoubleParameter("Diagnostic neutral loss values (Da)",
    				"Neutral loss m/z-values that must be included in MS/MS",
    				false,null
    				);
    
    public static final DoubleParameter basePeakPercent = new DoubleParameter(
    	    "Minimum diagnostic ion intensity (% base peak)",
    	    "Percent of scan base peak of which ms/ms product ions must be above to be included in analysis",
    	    MZmineCore.getConfiguration().getRTFormat(), 5.0);
    
    public static final FileNameParameter fileName = new FileNameParameter(
            "Peaklist output file",
            "Name of the output CSV file containing m/z and RT of selected precursor ions. "
                    + "If the file already exists, it will be overwritten.",
            "csv");
    

	/**
	 * Windows size and position
	 */
	public static final WindowSettingsParameter windowSettings = new WindowSettingsParameter();
	

	public ProductIonFilterParameters() {
		super(new Parameter[] { dataFiles, xAxisType, retentionTimeRange, mzRange, windowSettings, mzDifference,targetedMZ_List, targetedNF_List, basePeakPercent, fileName});
	}
	
	

}
