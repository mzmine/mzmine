package io.github.mzmine.modules.dataprocessing.featuretest;

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

public class TestParameters extends SimpleParameterSet {
    public static final FeatureListsParameter featurelists = new FeatureListsParameter();
    public static final IntegerParameter minSignals = new IntegerParameter("Min signals", "", 6);

    public TestParameters() {
        super(featurelists, minSignals);
    }
}
