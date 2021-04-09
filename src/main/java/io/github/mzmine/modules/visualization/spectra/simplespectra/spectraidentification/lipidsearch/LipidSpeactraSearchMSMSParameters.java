package io.github.mzmine.modules.visualization.spectra.simplespectra.spectraidentification.lipidsearch;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;

public class LipidSpeactraSearchMSMSParameters extends SimpleParameterSet {

	public static final BooleanParameter ionizationAutoSearch = new BooleanParameter("Auto select ionization method",
			"If checked, the slected Ionization method parameter will be ignored if a lipid class has fragmentation rules. The ionization method specified in the fragmentation rule will be used.");

	public LipidSpeactraSearchMSMSParameters() {
		super(new Parameter[] { ionizationAutoSearch });
	}

}