package net.sf.mzmine.modules.isotopes.isotopeprediction;

import java.text.NumberFormat;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;

public class IsotopePatternCalculatorParameters extends SimpleParameterSet {

    public static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

    public static final Parameter formula = new SimpleParameter(
            ParameterType.STRING, "Chemical formula",
            "empirical formula of a chemical compound", null, null, null);

    public static final Parameter minimalAbundance = new SimpleParameter(
            ParameterType.FLOAT, "Minimum % of relative abundance", "Mininum relative % of abundance per peak", "%",
            new Float(0.01), new Float(0.0001), new Float(0.1),
            null);


    public IsotopePatternCalculatorParameters() {
        super(new Parameter[] { formula, minimalAbundance });
    }

}
