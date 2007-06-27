package net.sf.mzmine.modules.normalization.simplestandardcompound;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

/*
 * Unfortunately clone() of SimpleParameterSet calls its contructor so this class can't extend it.
 */
public class SimpleStandardCompoundNormalizerParameterSet implements ParameterSet {

    protected static final String StandardUsageTypeNearest = "Nearest standard";
    protected static final String StandardUsageTypeWeighted = "Weighted contribution of all standards";

    protected static final Object[] StandardUsageTypePossibleValues = {
    	StandardUsageTypeNearest, StandardUsageTypeWeighted};

    protected static final Parameter StandardUsageType = new SimpleParameter(
            ParameterType.STRING, "Normalization type",
            "Normalize intensities using ", StandardUsageTypeNearest,
            StandardUsageTypePossibleValues);

    
    protected static final String PeakMeasurementTypeHeight = "Peak height";
    protected static final String PeakMeasurementTypeArea = "Peak area";

    protected static final Object[] PeakMeasurementTypePossibleValues = {
    	PeakMeasurementTypeHeight, PeakMeasurementTypeArea};
    
    protected static final Parameter PeakMeasurementType = new SimpleParameter(
            ParameterType.STRING, "Peak measurement type",
            "Measure peaks using ", PeakMeasurementTypeArea,
            PeakMeasurementTypePossibleValues);
    
    
    public static final Parameter MZvsRTBalance = new SimpleParameter(
            ParameterType.FLOAT, "M/Z vs RT balance",
            "Used in distance measuring as multiplier of M/Z difference", "",
            new Float(10.0), new Float(0.0), null);
	

    private SimpleParameterSet parameters;
    
	private PeakListRow[] selectedPeaks;

	public SimpleStandardCompoundNormalizerParameterSet() {
		parameters = new SimpleParameterSet(
                new Parameter[] { StandardUsageType, PeakMeasurementType, MZvsRTBalance });
 
	}
	
	public ParameterSet clone() {
		SimpleStandardCompoundNormalizerParameterSet clone
		 = new SimpleStandardCompoundNormalizerParameterSet();
		
		clone.setParameters(parameters.clone());
		
		if (getSelectedStandardPeakListRows()!=null) {
			PeakListRow[] cloneSelectedPeaks = new PeakListRow[getSelectedStandardPeakListRows().length];
			for (int ind=0; ind<selectedPeaks.length; ind++) 
				cloneSelectedPeaks[ind] = getSelectedStandardPeakListRows()[ind]; 
			clone.setSelectedStandardPeakListRows(cloneSelectedPeaks);
		}
		
		return clone;
		
	}
	
	public void setParameters(SimpleParameterSet parameters) { this.parameters = parameters; }
	
	public SimpleParameterSet getParameters() { return parameters; }
	
	public void setSelectedStandardPeakListRows(PeakListRow[] selectedPeaks) { this.selectedPeaks = selectedPeaks; }
	
	public PeakListRow[] getSelectedStandardPeakListRows() { return selectedPeaks; }
	
    public String toString() {
        String s = "";
        s = s.concat(StandardUsageType.getName() + ": " + parameters.getParameterValue(StandardUsageType) + ", ");
        s = s.concat(PeakMeasurementType.getName() + ": " + parameters.getParameterValue(PeakMeasurementType) + ", ");
        s = s.concat(MZvsRTBalance.getName() + ": " + parameters.getParameterValue(MZvsRTBalance));
        return s;
    }	
}
