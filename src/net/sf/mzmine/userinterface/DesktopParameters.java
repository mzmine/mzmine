package net.sf.mzmine.userinterface;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Parameter.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.util.TimeNumberFormat;

public class DesktopParameters {

    /*
	public final static Parameter retentionTimeNumberFormatParameter 
		= new SimpleParameter( ParameterType.OBJECT,
								"Retention time format",
								"Number formatting style used to present retention time in the GUI",
								"",
								new SimpleParameterValue(new TimeNumberFormat()));
	
	public final static Parameter mzNumberFormatParameter
	= new SimpleParameter( ParameterType.OBJECT,
			"M/Z format",
			"Number formatting style used to present M/Z values in the GUI",
			"",
			new SimpleParameterValue(new DecimalFormat("0.000")));	
	
	public final static Parameter percentFormatParameter
	= new SimpleParameter( ParameterType.OBJECT,
			"Percent format",
			"Number formatting style used to present percent values in the GUI",
			"",
			new SimpleParameterValue(NumberFormat.getPercentInstance()));

	public final static Parameter integerFormatParameter
	= new SimpleParameter( ParameterType.OBJECT,
			"Integer format",
			"Number formatting style used to present integer values in the GUI",
			"",
			new SimpleParameterValue(NumberFormat.getIntegerInstance()));

	public final static Parameter decimalFormatParameter
	= new SimpleParameter( ParameterType.OBJECT,
			"Float format",
			"Number formatting style used to present decimal values in the GUI",
			"",
			new SimpleParameterValue(new DecimalFormat("0.0")));
	
	*/
}
