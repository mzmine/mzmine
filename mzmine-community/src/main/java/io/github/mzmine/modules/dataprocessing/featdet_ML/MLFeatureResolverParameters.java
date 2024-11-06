package io.github.mzmine.modules.dataprocessing.featdet_ML;

import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.GeneralResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.addionannotations.AddIonNetworkingParameters.Setup;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.ranges.DoubleRangeParameter;
import java.text.DecimalFormat;
import com.google.common.collect.Range;
import org.jetbrains.annotations.Nullable;

public class MLFeatureResolverParameters extends GeneralResolverParameters {

    public static final PercentParameter threshold = new PercentParameter(
            "Confidence level from 0 to 1", "description for ML resolver",
            0.5d, 0d, 1d);

    public static final IntegerParameter MIN_NUMBER_OF_DATAPOINTS = new IntegerParameter(
            "Minimal number of datapoints per range",
            "The minimal number of datapoints that are required to lie in the Range", 3);

    public static final BooleanParameter correctRanges = new BooleanParameter("Correct ranges (for debugging)", "extends ranges if slope to next data point is sufficiently high");

    // public static final BooleanParameter correctIntersections = new BooleanParameter("correct intersections (for debugging)", "Resizes ranges in case of overlap");
    public static final BooleanParameter withOffset = new BooleanParameter("(For testing) Use model which uses offset for later predictions", "");

    public MLFeatureResolverParameters() {
        super(createParams(MLSetup.FULL),
                "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_local_minimum/local-minimum-resolver.html");
    }

    public MLFeatureResolverParameters(MLSetup setup) {
        super(createParams(setup),
                "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_resolver_local_minimum/local-minimum-resolver.html");
    }

    private static Parameter[] createParams(MLSetup setup) {
        return switch (setup) {
            case FULL -> new Parameter[] { PEAK_LISTS, SUFFIX, handleOriginal, groupMS2Parameters,
                    dimension, threshold,
                    correctRanges, MIN_NUMBER_OF_DATAPOINTS, withOffset  };
            case INTEGRATED -> new Parameter[] { threshold,correctRanges,
                    MIN_NUMBER_OF_DATAPOINTS };
        };
    }

    @Nullable
    @Override
    public Resolver getResolver(ParameterSet parameters, ModularFeatureList flist) {
        return new MLFeatureResolver(parameters, flist);
    }

    public enum MLSetup {
        FULL, INTEGRATED;
    }

}
