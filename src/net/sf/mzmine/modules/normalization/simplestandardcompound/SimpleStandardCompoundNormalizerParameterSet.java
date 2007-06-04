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

    protected static final String NormalizationTypeNearest = "Nearest standard";
    protected static final String NormalizationTypeWeighted = "Weighted contribution of all standards";

    protected static final Object[] normalizationTypePossibleValues = {
    	NormalizationTypeNearest,
    	NormalizationTypeWeighted};

    protected static final Parameter normalizationType = new SimpleParameter(
            ParameterType.STRING, "Normalization type",
            "Normalize intensities using ", NormalizationTypeNearest,
            normalizationTypePossibleValues);
	
	
	private SimpleParameterSet parameters;
	
	private PeakListRow[] selectedPeaks;

	public SimpleStandardCompoundNormalizerParameterSet() {
		parameters = new SimpleParameterSet(
                new Parameter[] { normalizationType });
 
	}
	
	public ParameterSet clone() {
		SimpleStandardCompoundNormalizerParameterSet clone
		 = new SimpleStandardCompoundNormalizerParameterSet();
		
		clone.setParameters(parameters.clone());
		
		if (getSelectedPeaks()!=null) {
			PeakListRow[] cloneSelectedPeaks = new PeakListRow[getSelectedPeaks().length];
			for (int ind=0; ind<selectedPeaks.length; ind++) 
				cloneSelectedPeaks[ind] = getSelectedPeaks()[ind]; 
			clone.setSelectedPeaks(cloneSelectedPeaks);
		}
		
		return clone;
		
	}
	
	public void setParameters(SimpleParameterSet parameters) { this.parameters = parameters; }
	
	public SimpleParameterSet getParameters() { return parameters; }
	
	public void setSelectedPeaks(PeakListRow[] selectedPeaks) { this.selectedPeaks = selectedPeaks; }
	
	public PeakListRow[] getSelectedPeaks() { return selectedPeaks; }
	
    public String toString() {
        String s = "";
        s = s.concat(normalizationType.getName() + ": " + parameters.getParameterValue(normalizationType));
        return s;
    }	
}
