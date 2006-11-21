package net.sf.mzmine.batchmode.impl;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.methods.MethodParameters;

public class BatchModeParameters extends MethodParameters {

	protected static final Parameter methodRawDataFilter1 = new SimpleParameter( ParameterType.OBJECT,
			"1st Raw data filter",
			"First method used for filtering raw data",
			"",
			null,
			null);

	protected static final Parameter methodRawDataFilter2 = new SimpleParameter( ParameterType.OBJECT,
			"2nd Raw data filter",
			"Second method used for filtering raw data",
			"",
			null,
			null);

	protected static final Parameter methodRawDataFilter3 = new SimpleParameter( ParameterType.OBJECT,
			"3rd Raw data filter",
			"Third method used for filtering raw data",
			"",
			null,
			null);

	protected static final Parameter methodPeakPicker = new SimpleParameter( ParameterType.OBJECT,
			"Peak picker",
			"Peak picking method",
			"",
			null,
			null);

	protected static final Parameter methodPeakListProcessor1 = new SimpleParameter( ParameterType.OBJECT,
			"1st Peak list processor",
			"First method used for processing peak lists",
			"",
			null,
			null);

	protected static final Parameter methodPeakListProcessor2 = new SimpleParameter( ParameterType.OBJECT,
			"2nd Peak list processor",
			"Second method used for processing peak lists",
			"",
			null,
			null);	

	protected static final Parameter methodPeakListProcessor3 = new SimpleParameter( ParameterType.OBJECT,
			"3rd Peak list processor",
			"Third method used for processing peak lists",
			"",
			null,
			null);
	
	protected static final Parameter methodAligner = new SimpleParameter( ParameterType.OBJECT,
			"Alignment method",
			"Method used for aligning peak lists",
			"",
			null,
			null);
	
	protected static final Parameter methodAlignmentProcessor1 = new SimpleParameter( ParameterType.OBJECT,
			"1st Alignment processor",
			"First method used for processing alignment result",
			"",
			null,
			null);

	protected static final Parameter methodAlignmentProcessor2 = new SimpleParameter( ParameterType.OBJECT,
			"2nd Alignment processor",
			"Second method used for processing alignment result",
			"",
			null,
			null);

	protected static final Parameter methodAlignmentProcessor3 = new SimpleParameter( ParameterType.OBJECT,
			"3rd Alignment processor",
			"Third method used for processing alignment result",
			"",
			null,
			null);		
	
	public Parameter[] getParameters() {

		Parameter[] ps = new Parameter[11];
		ps[0] = methodRawDataFilter1;
		ps[1] = methodRawDataFilter2;
		ps[2] = methodRawDataFilter3;
		ps[3] = methodPeakPicker;
		ps[4] = methodPeakListProcessor1;
		ps[5] = methodPeakListProcessor2;
		ps[6] = methodPeakListProcessor3;
		ps[7] = methodAligner;
		ps[8] = methodAlignmentProcessor1;
		ps[9] = methodAlignmentProcessor2;
		ps[10] = methodAlignmentProcessor3;
		
		return ps;
		
	}
	
}
