package io.github.mzmine.modules.dataprocessing.comb_resolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Range;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.featdet_ML.MLFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;

public class CombinedResolver extends AbstractResolver {

    CombinedResolverParameters parameters;
    private static final Logger logger = Logger.getLogger("CombinedResolver logger");
    private static final double varianceThreshold = 2;

    private static Resolver firstResolver;
    private static Resolver secondResolver;

    protected CombinedResolver(@NotNull ParameterSet parameters, @NotNull ModularFeatureList flist) {
        super(parameters, flist);
        if (!(parameters instanceof CombinedResolverParameters)) {
            logger.warning("Wrong resolver parameters");
            return;
        }
        this.parameters = (CombinedResolverParameters) parameters;
    }

    public void loadEmbeddedResolvers(CombinedResolverParameters params, ModularFeatureList originalFeatureList) {
        ModuleOptionsEnumComboParameter<CombinedResolverEnum> firstResolverParameters = CombinedResolverParameters.firstResolverParameters;
        ModuleOptionsEnumComboParameter<CombinedResolverEnum> secondResolverParameters = CombinedResolverParameters.secondResolverParameters;
        ParameterSet firstEmbeddedParameters = firstResolverParameters.getEmbeddedParameters();
        ParameterSet secondEmbeddedParameters = secondResolverParameters.getEmbeddedParameters();
        switch (firstResolverParameters.getValue()) {
            case CombinedResolverEnum.LOCAL_MIN ->
                firstResolver = ((MinimumSearchFeatureResolverParameters) firstEmbeddedParameters)
                        .getResolver(firstEmbeddedParameters, originalFeatureList);
            case CombinedResolverEnum.ML_RESOLVER ->
                firstResolver = ((MLFeatureResolverParameters) firstEmbeddedParameters)
                        .getResolver(firstEmbeddedParameters, originalFeatureList);
        }
        switch (secondResolverParameters.getValue()) {
            case CombinedResolverEnum.LOCAL_MIN ->
                secondResolver = ((MinimumSearchFeatureResolverParameters) secondEmbeddedParameters)
                        .getResolver(secondEmbeddedParameters, originalFeatureList);
            case CombinedResolverEnum.ML_RESOLVER ->
                secondResolver = ((MLFeatureResolverParameters) secondEmbeddedParameters)
                        .getResolver(secondEmbeddedParameters, originalFeatureList);
        }
    }

    // @Override
    // public @NotNull Map<Resolver, List<Range<Double>>> resolve(double[] x,
    // double[] y) {
    // List<Resolver> resolvers = ((CombinedResolverParameters)
    // this.parameters).getResolvers(this.parameters,
    // this.flist);
    // if (resolvers.size() == 0) {
    // logger.warning("No resolvers selected");
    // return null;
    // }
    // Map<Resolver, List<Range<Double>>> resolvedRanges = new HashMap<>();
    // // List<Range<Double>> resolvedRanges = new ArrayList<>();
    // for (Resolver resolver : resolvers) {
    // resolvedRanges.put(resolver, resolver.resolve(x, y));
    // // resolvedRanges.addAll(resolver.resolve(x, y));
    // }
    // List<Range<Double>> totalResolvedRanges = new ArrayList<>();

    // return resolvedRanges;
    // }

    @Override
    public @NotNull Class<? extends MZmineModule> getModuleClass() {
        return CombinedResolverModule.class;
    }

    private int findIndex(double[] arr, double value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == value) {
                return i;
            }
        }

        return -1;
    }

    private FilterOutput filterRanges(double[] time, double[] intensity, List<Range<Double>> firstResolverRanges,
            List<Range<Double>> secondResolverRanges) {
        // If at least one resolver result is empty, then there is nothing to check
        if (firstResolverRanges.size() == 0 || secondResolverRanges.size() == 0) {
            return new FilterOutput(firstResolverRanges, secondResolverRanges, new ArrayList<Range<Double>>());
        }
        List<Range<Double>> onlyFirstResolverRanges = new ArrayList<Range<Double>>();
        List<Range<Double>> onlySecondResolverRanges = new ArrayList<Range<Double>>();
        List<Range<Double>> bothResolverRanges = new ArrayList<Range<Double>>();
        // index for x list
        int i = 0;
        // index for y list
        int j = 0;

        Range<Double> currentFirst = firstResolverRanges.get(0);
        Range<Double> currentSecond = firstResolverRanges.get(0);

        Double firstLeft = currentFirst.lowerEndpoint();
        Double firstRight = currentFirst.upperEndpoint();

        Double secondLeft = currentSecond.lowerEndpoint();
        Double secondRight = currentSecond.upperEndpoint();

        while (i < firstResolverRanges.size() && j < secondResolverRanges.size()) {
            // ranges do not intersect and first is lower
            if (firstRight <= secondLeft) {
                onlyFirstResolverRanges.add(currentFirst);
                i++;
                currentFirst = firstResolverRanges.get(i);
                firstLeft = currentFirst.lowerEndpoint();
                firstRight = currentFirst.upperEndpoint();
                // ranges do not intersect and second is lower
            } else if (secondRight <= firstLeft) {
                onlySecondResolverRanges.add(currentSecond);
                j++;
                currentSecond = secondResolverRanges.get(j);
                secondLeft = currentSecond.lowerEndpoint();
                secondRight = currentSecond.upperEndpoint();
                // currentFirst is contained in currentSecond
            } else if (currentFirst.intersection(currentSecond).equals(currentFirst)) {
                // temp code to accept loc min results
                bothResolverRanges.add(currentFirst);
                i++;
                currentFirst = firstResolverRanges.get(i);
                firstLeft = currentFirst.lowerEndpoint();
                firstRight = currentFirst.upperEndpoint();
                j++;
                currentSecond = secondResolverRanges.get(j);
                secondLeft = currentSecond.lowerEndpoint();
                secondRight = currentSecond.upperEndpoint();

                // currentSecond is contained in currentFirst
            } else if (currentSecond.intersection(currentFirst).equals(currentSecond)) {
                // temp code to accept loc min results
                while (firstRight > secondLeft) {
                    j++;
                    currentSecond = secondResolverRanges.get(j);
                    secondLeft = currentSecond.lowerEndpoint();
                    secondRight = currentSecond.upperEndpoint();
                }
                bothResolverRanges.add(currentFirst);
                i++;
                currentFirst = firstResolverRanges.get(i);
                firstLeft = currentFirst.lowerEndpoint();
                firstRight = currentFirst.upperEndpoint();
                // intersection but not contained
            } else if (firstLeft < secondLeft) {
                // currently always accepts results from first resolver
                bothResolverRanges.add(currentFirst);
                i++;
                currentFirst = firstResolverRanges.get(i);
                firstLeft = currentFirst.lowerEndpoint();
                firstRight = currentFirst.upperEndpoint();
                j++;
                currentSecond = secondResolverRanges.get(j);
                secondLeft = currentSecond.lowerEndpoint();
                secondRight = currentSecond.upperEndpoint();
            } else {
                // currently always accepts results from first resolver
                bothResolverRanges.add(currentFirst);
                i++;
                currentFirst = firstResolverRanges.get(i);
                firstLeft = currentFirst.lowerEndpoint();
                firstRight = currentFirst.upperEndpoint();
                j++;
                currentSecond = secondResolverRanges.get(j);
                secondLeft = currentSecond.lowerEndpoint();
                secondRight = currentSecond.upperEndpoint();
            }
        }
        // adds remaining ranges. These shoulb be unproblematic
        while (i < firstResolverRanges.size()) {
            onlyFirstResolverRanges.add(firstResolverRanges.get(i));
            i++;
        }
        while (j < secondResolverRanges.size()) {
            onlySecondResolverRanges.add(secondResolverRanges.get(j));
            j++;
        }

        return new FilterOutput(onlyFirstResolverRanges, onlySecondResolverRanges, bothResolverRanges);
    }

    private boolean checkVariance(double[] time, double[] intensity, double leftBound, double rightBound) {
        int leftIndex = findIndex(time, leftBound);
        int rightIndex = findIndex(time, rightBound);
        if (leftIndex == -1 || rightIndex == -1) {
            logger.fine("Could not find Index of bounds");
            return false;
        }
        // calculate width with both endpoints indluded
        int peakWidth = rightIndex - leftIndex + 1;
        int extendedLeftIndex = Math.max(leftIndex - peakWidth, 0);
        int extendedRightIndex = Math.min(rightIndex + 1 + peakWidth, intensity.length - 1);
        // take a copy of the original range and an extended range to compare variance
        double[] rangeIntensity = Arrays.copyOfRange(intensity, leftIndex, rightIndex + 1);
        double[] extendedRangeIntensity = Arrays.copyOfRange(intensity, extendedLeftIndex, extendedRightIndex);

        if (rangeIntensity.length == 0 || extendedRangeIntensity.length == 0) {
            logger.fine("Can not calculate variance for width 0");
            return false;
        }
        // calculate the means for both ranges
        double mean = Arrays.stream(rangeIntensity).reduce((x, y) -> x + y).orElse(0.0) / rangeIntensity.length;
        double extendedMean = Arrays.stream(extendedRangeIntensity).reduce((x, y) -> x + y).orElse(0.0)
                / rangeIntensity.length;

        // calculate both variances
        double variance = Arrays.stream(rangeIntensity).map(x -> Math.pow((x - mean), 2))
                .reduce((x, y) -> x + y).orElse(0.0) / rangeIntensity.length;
        double extendedVariance = Arrays.stream(extendedRangeIntensity).map(x -> Math.pow((x - mean), 2))
                .reduce((x, y) -> x + y).orElse(0.0) / extendedRangeIntensity.length;

        if (variance / extendedVariance > varianceThreshold) {
            return true;
        }

        return false;
    }

    @Override
    public @NotNull List<Range<Double>> resolve(double[] time, double[] intensity) {
        List<Range<Double>> firstResolverRanges = firstResolver.resolve(time, intensity);
        List<Range<Double>> secondResolverRanges = secondResolver.resolve(time, intensity);

        FilterOutput filteredRanges = filterRanges(time, intensity, firstResolverRanges, secondResolverRanges);

        List<Range<Double>> totalRanges = new ArrayList<Range<Double>>();
        totalRanges.addAll(filteredRanges.bothResolverRanges());
        for (Range<Double> range : filteredRanges.onlyFirstResolverRanges()) {
            totalRanges.add(range);
            // if (checkVariance(time, intensity, range.lowerEndpoint(), range.upperEndpoint())) {
            //     totalRanges.add(range);
            // }
        }
        for (Range<Double> range : filteredRanges.onlySecondResolverRanges()) {
            if (checkVariance(time, intensity, range.lowerEndpoint(), range.upperEndpoint())) {
                totalRanges.add(range);
            }
        }
        return totalRanges;
    }
}
