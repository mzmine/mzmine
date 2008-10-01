package net.sf.mzmine.modules.isotopes.isotopeprediction;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;

public class IsotopePatternCalculatorParameters extends SimpleParameterSet {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public static final Parameter formula = new SimpleParameter(
            ParameterType.STRING, "Chemical formula",
            "empirical formula of a chemical compound", null, null, null);

    public static final Parameter minimalAbundance = new SimpleParameter(
            ParameterType.FLOAT, "Minimum % of relative abundance", "Mininum relative % of abundance per peak", "%",
            new Float(0.01f), new Float(0.0001f), new Float(0.1f),
            null);

	public static final Parameter autoHeight = new SimpleParameter(
			ParameterType.BOOLEAN, "Set automatic height",
			"Set automatic height of most abundance isotope", null, null,
			null, null, null);
	
    public static final Parameter isotopeHeight = new SimpleParameter(
            ParameterType.FLOAT, "Height of most abundance isotope", "Height of most abundance isotope", "absolute",
            new Float(10000.0f), new Float(1.0f), null,
            MZmineCore.getIntensityFormat());

    public IsotopePatternCalculatorParameters() {
        super(new Parameter[] { formula, minimalAbundance, autoHeight, isotopeHeight });
    }

}
