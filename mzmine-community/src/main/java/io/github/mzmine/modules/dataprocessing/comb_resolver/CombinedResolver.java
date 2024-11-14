package io.github.mzmine.modules.dataprocessing.comb_resolver;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.comb_resolver.CombinedResolverResult.DetectedBy;
import io.github.mzmine.modules.dataprocessing.featdet_ML.MLFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.AbstractResolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.Resolver;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComboParameter;
import io.github.mzmine.util.ArrayUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class CombinedResolver extends AbstractResolver {

  CombinedResolverParameters parameters;
  private static final Logger logger = Logger.getLogger("CombinedResolver logger");
  private static final double varianceThreshold = 2;

  private Resolver firstResolver;
  private Resolver secondResolver;

  private final List<CombinedResolverResult> debug = new ArrayList<>();

  protected CombinedResolver(@NotNull ParameterSet parameters, @NotNull ModularFeatureList flist) {
    super(parameters, flist);
    if (!(parameters instanceof CombinedResolverParameters)) {
      logger.warning("Wrong resolver parameters");
      return;
    }
    loadEmbeddedResolvers((CombinedResolverParameters) parameters, flist);
    this.parameters = (CombinedResolverParameters) parameters;
  }

  public void loadEmbeddedResolvers(CombinedResolverParameters params,
      ModularFeatureList originalFeatureList) {
    ModuleOptionsEnumComboParameter<CombinedResolverEnum> firstResolverParameters = CombinedResolverParameters.firstResolverParameters;
    ModuleOptionsEnumComboParameter<CombinedResolverEnum> secondResolverParameters = CombinedResolverParameters.secondResolverParameters;
    ParameterSet firstEmbeddedParameters = firstResolverParameters.getEmbeddedParameters()
        .cloneParameterSet();
    ParameterSet secondEmbeddedParameters = secondResolverParameters.getEmbeddedParameters()
        .cloneParameterSet();
    switch (firstResolverParameters.getValue()) {
      case CombinedResolverEnum.LOCAL_MIN -> {
        firstResolver = ((MinimumSearchFeatureResolverParameters) firstEmbeddedParameters).getResolver(
            firstEmbeddedParameters, originalFeatureList);
      }
      case CombinedResolverEnum.ML_RESOLVER -> {
        firstResolver = ((MLFeatureResolverParameters) firstEmbeddedParameters).getResolver(
            firstEmbeddedParameters, originalFeatureList);
      }
    }
    switch (secondResolverParameters.getValue()) {
      case CombinedResolverEnum.LOCAL_MIN -> {
        secondResolver = ((MinimumSearchFeatureResolverParameters) secondEmbeddedParameters).getResolver(
            secondEmbeddedParameters, originalFeatureList);
      }
      case CombinedResolverEnum.ML_RESOLVER -> {
        secondResolver = ((MLFeatureResolverParameters) secondEmbeddedParameters).getResolver(
            secondEmbeddedParameters, originalFeatureList);
      }
    }
  }

  @Override
  public @NotNull Class<? extends MZmineModule> getModuleClass() {
    return CombinedResolverModule.class;
  }

  private FilterOutput filterRanges(double[] time, double[] intensity,
      List<Range<Double>> firstResolverRanges, List<Range<Double>> secondResolverRanges) {
    // If at least one resolver result is empty, then there is nothing to check
    if (firstResolverRanges.size() == 0 || secondResolverRanges.size() == 0) {
      return new FilterOutput(firstResolverRanges, secondResolverRanges,
          new ArrayList<Range<Double>>());
    }
    List<Range<Double>> onlyFirstResolverRanges = new ArrayList<Range<Double>>();
    List<Range<Double>> onlySecondResolverRanges = new ArrayList<Range<Double>>();
    List<Range<Double>> bothResolverRanges = new ArrayList<Range<Double>>();
    // index for first list
    int i = 0;
    // index for second list
    int j = 0;

    Range<Double> currentFirst = firstResolverRanges.get(0);
    Range<Double> currentSecond = firstResolverRanges.get(0);

    Double firstLeft = currentFirst.lowerEndpoint();
    Double firstRight = currentFirst.upperEndpoint();

    Double secondLeft = currentSecond.lowerEndpoint();
    Double secondRight = currentSecond.upperEndpoint();

    while (i < firstResolverRanges.size() && j < secondResolverRanges.size()) {
      currentFirst = firstResolverRanges.get(i);
      firstLeft = currentFirst.lowerEndpoint();
      firstRight = currentFirst.upperEndpoint();
      currentSecond = secondResolverRanges.get(j);
      secondLeft = currentSecond.lowerEndpoint();
      secondRight = currentSecond.upperEndpoint();
      // ranges do not intersect and first is lower
      if (firstRight <= secondLeft) {
        onlyFirstResolverRanges.add(currentFirst);
        i++;
        // ranges do not intersect and second is lower
      } else if (secondRight <= firstLeft) {
        onlySecondResolverRanges.add(currentSecond);
        j++;
        // currentFirst is contained in currentSecond
      } else if (currentFirst.intersection(currentSecond).equals(currentFirst)) {
        // temp code to accept loc min results
        bothResolverRanges.add(currentFirst);
        i++;
        j++;

        // currentSecond is contained in currentFirst
      } else if (currentSecond.intersection(currentFirst).equals(currentSecond)) {
        // temp code to accept loc min results
        while (firstRight > secondLeft) {
          j++;
          if (j < secondResolverRanges.size()) {
            currentSecond = secondResolverRanges.get(j);
            secondLeft = currentSecond.lowerEndpoint();
            secondRight = currentSecond.upperEndpoint();
          } else {
            break;
          }
        }
        bothResolverRanges.add(currentFirst);
        i++;
        // intersection but not contained
      } else if (firstLeft < secondLeft) {
        // currently always accepts results from first resolver
        bothResolverRanges.add(currentFirst);
        i++;
        j++;
      } else {
        // currently always accepts results from first resolver
        bothResolverRanges.add(currentFirst);
        i++;
        j++;
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

  private double getVarianceRatio(double[] time, double[] intensity, double leftBound,
      double rightBound) {
    int leftIndex = ArrayUtils.indexOf(leftBound, time);
    int rightIndex = ArrayUtils.indexOf(rightBound, time);
    if (leftIndex == -1 || rightIndex == -1) {
      logger.fine("Could not find Index of bounds");
      return 0d;
    }
    // calculate width with both endpoints indluded
    int peakWidth = rightIndex - leftIndex + 1;
    int extendedLeftIndex = Math.max(leftIndex - peakWidth, 0);
    int extendedRightIndex = Math.min(rightIndex + 1 + peakWidth, intensity.length - 1);
    // take a copy of the original range and an extended range to compare variance
    double[] rangeIntensity = Arrays.copyOfRange(intensity, leftIndex, rightIndex + 1);
    double[] extendedRangeIntensity = Arrays.copyOfRange(intensity, extendedLeftIndex,
        extendedRightIndex);

    if (rangeIntensity.length == 0 || extendedRangeIntensity.length == 0) {
      logger.fine("Can not calculate variance for width 0");
      return 0d;
    }
    // calculate the means for both ranges
    double mean =
        Arrays.stream(rangeIntensity).reduce((x, y) -> x + y).orElse(0.0) / rangeIntensity.length;
    double extendedMean = Arrays.stream(extendedRangeIntensity).reduce((x, y) -> x + y).orElse(0.0)
        / rangeIntensity.length;

    // calculate both variances
    double variance =
        Arrays.stream(rangeIntensity).map(x -> Math.pow((x - mean), 2)).reduce((x, y) -> x + y)
            .orElse(0.0) / rangeIntensity.length;
    double extendedVariance =
        Arrays.stream(extendedRangeIntensity).map(x -> Math.pow((x - mean), 2))
            .reduce((x, y) -> x + y).orElse(0.0) / extendedRangeIntensity.length;

    return variance / extendedVariance;
  }

  public double calculateZigZagIndex(double[] featureIntensity) {
    double zigZagSum = 0;
    for (int i = 1; i < featureIntensity.length - 1; i++) {
      zigZagSum += Math.pow(
          (2 * featureIntensity[i] - featureIntensity[i - 1] - featureIntensity[i + 1]), 2);
    }
    // TODO implement baseline calculation
    double baseline = 0;
    double EPI = Arrays.stream(featureIntensity).max().orElse(0) - baseline;
    double zigZagIndex = zigZagSum / (featureIntensity.length * EPI);
    return zigZagIndex;
  }


  public double calculateSharpness(double[] featureIntensities) {
    if (featureIntensities.length < 2) {
      return 0;
    }
    double peakIntensity = Arrays.stream(featureIntensities).max().orElse(0);
    int peakIndex = ArrayUtils.indexOf(peakIntensity, featureIntensities);
    double leftSharpness = 0;
    double rightSharpness = 0;
    for (int i = 1; i <= peakIndex; i++) {
      leftSharpness +=
          (featureIntensities[i] - featureIntensities[i - 1]) / featureIntensities[i - 1];
    }
    for (int i = peakIndex; i < featureIntensities.length - 1; i++) {
      rightSharpness +=
          (featureIntensities[i] - featureIntensities[i + 1]) / featureIntensities[i + 1];
    }
    return leftSharpness + rightSharpness;
  }

  @Override
  public @NotNull List<Range<Double>> resolve(double[] time, double[] intensity) {
    List<Range<Double>> firstResolverRanges = firstResolver.resolve(time, intensity);
    List<Range<Double>> secondResolverRanges = secondResolver.resolve(time, intensity);
    debug.clear();

    FilterOutput filteredRanges = filterRanges(time, intensity, firstResolverRanges,
        secondResolverRanges);

    List<Range<Double>> validRanges = new ArrayList<>();

    processRanges(time, intensity, filteredRanges.bothResolverRanges(), validRanges,
        DetectedBy.BOTH);

    processRanges(time, intensity, filteredRanges.onlyFirstResolverRanges(), validRanges,
        DetectedBy.FIRST);

    processRanges(time, intensity, filteredRanges.onlySecondResolverRanges(), validRanges,
        DetectedBy.SECOND);
    return validRanges;
  }

  private void processRanges(double[] time, double[] intensity, List<Range<Double>> ranges,
      List<Range<Double>> validRanges, DetectedBy detectedBy) {
    for (Range<Double> range : ranges) {
      int leftIndex = ArrayUtils.indexOf(range.lowerEndpoint(), time);
      int rightIndex = ArrayUtils.indexOf(range.upperEndpoint(), time);
      double[] featureIntensities = Arrays.copyOfRange(intensity, leftIndex, rightIndex + 1);
      final double zigZagIndex = calculateZigZagIndex(featureIntensities);
      final double varianceRatio = getVarianceRatio(time, intensity, range.lowerEndpoint(),
          range.upperEndpoint());
      final double sharpness = calculateSharpness(featureIntensities);
      validRanges.add(range);
      debug.add(new CombinedResolverResult(zigZagIndex, varianceRatio, 0d, sharpness, detectedBy));
    }
  }

  public List<CombinedResolverResult> getDebug() {
    return debug;
  }
}
