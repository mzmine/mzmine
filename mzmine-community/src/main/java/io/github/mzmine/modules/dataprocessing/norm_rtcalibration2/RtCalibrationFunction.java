package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.als.AlsCorrection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RtCalibrationFunction {

  private static final Logger logger = Logger.getLogger(RtCalibrationFunction.class.getName());

  private final RawDataFilePlaceholder filePlaceholder;
  //  private final @NotNull PolynomialSplineFunction loess;
//  private final @NotNull PolynomialSplineFunction linear;
  private final PolynomialSplineFunction movAvg;
//  private double optimisedBandwidth = 0d;
//  private final int iterations = 3;

  public RtCalibrationFunction(RawDataFile file, PolynomialSplineFunction function) {
    this.filePlaceholder = new RawDataFilePlaceholder(file);
    this.movAvg = function;
  }

  public RtCalibrationFunction(FeatureList flist, List<RtStandard> rtSortedStandards,
      double bandwidth) {
    final RawDataFile file = flist.getRawDataFiles().getFirst();
    final Range<Float> fullRtRange = file.getDataRTRange();
    filePlaceholder = new RawDataFilePlaceholder(file);

    DoubleArrayList thisRtValues = new DoubleArrayList();
    DoubleArrayList standardRtValues = new DoubleArrayList();

    // never go below zero, so make 0 the first point of interpolation.
    thisRtValues.add(0d);
    standardRtValues.add(0d);

    for (RtStandard standard : rtSortedStandards) {
      thisRtValues.add(standard.standards().get(file).getAverageRT());
      standardRtValues.add(standard.getAverageRt());
    }

    final FeatureListRow row = rtSortedStandards.getLast().standards().get(file);
    final float averageRT = row.getAverageRT();
    addFinalRt(rtSortedStandards, fullRtRange, averageRT, thisRtValues, standardRtValues);

//    loess = getInterpolatorIteratively(file, bandwidth, thisRtValues, standardRtValues);
//    linear = new LinearInterpolator().interpolate(thisRtValues.toDoubleArray(),
//        standardRtValues.toDoubleArray());

    movAvg = getInterpolatorIteratively(bandwidth, standardRtValues, thisRtValues);
  }

  public RtCalibrationFunction(@NotNull final RawDataFile file,
      @NotNull final List<RtStandard> rtSortedStandards,
      @NotNull final RtCalibrationFunction previousRunCalibration, final double previousRunWeight,
      @NotNull final RtCalibrationFunction nextRunCalibration, final double nextRunWeight,
      double bandwidth) {

    filePlaceholder = new RawDataFilePlaceholder(file);
    final Range<Float> fullRtRange = file.getDataRTRange();

    final DoubleArrayList thisRtValues = new DoubleArrayList();
    final DoubleArrayList standardRtValues = new DoubleArrayList();

    // never go below zero, so make 0 the first point of interpolation.
    thisRtValues.add(0d);
    standardRtValues.add(0d);

    final RawDataFile prevFile = previousRunCalibration.getRawDataFile();
    final RawDataFile nextFile = nextRunCalibration.getRawDataFile();
    for (RtStandard standard : rtSortedStandards) {
      final double previous = standard.standards().get(prevFile).getAverageRT() * previousRunWeight;
      final double next = standard.standards().get(nextFile).getAverageRT() * nextRunWeight;

      thisRtValues.add(previous + next);
      standardRtValues.add(standard.getAverageRt());
    }

    final double previous =
        rtSortedStandards.getLast().standards().get(prevFile).getAverageRT() * previousRunWeight;
    final double next =
        rtSortedStandards.getLast().standards().get(nextFile).getAverageRT() * nextRunWeight;
    final float lastStandardRt = (float) (previous + next);

    addFinalRt(rtSortedStandards, fullRtRange, lastStandardRt, thisRtValues, standardRtValues);

//    loess = getInterpolatorIteratively(file, bandwidth, thisRtValues, standardRtValues);
//    linear = new LinearInterpolator().interpolate(thisRtValues.toDoubleArray(),
//        standardRtValues.toDoubleArray());
    movAvg = getInterpolatorIteratively(bandwidth, standardRtValues, thisRtValues);
  }

  @NotNull
  private PolynomialSplineFunction getInterpolatorIteratively(double initialBandwidth,
      DoubleArrayList calibratedRtValues, DoubleArrayList thisRtValues) {
    final RawDataFile file = filePlaceholder.getMatchingFile();
    boolean isMonotonous = false;
    PolynomialSplineFunction movAvg = null;
    final double[] subtracted = AlsCorrection.subtract(calibratedRtValues.toDoubleArray(),
        thisRtValues.toDoubleArray());
    subtracted[0] = 0d;
    subtracted[subtracted.length - 1] = 0d;

    double[] avg = MovingAverage.calculate(subtracted,
        Math.max(1, (int) (subtracted.length * initialBandwidth)));
    double[] alsFit = AlsCorrection.asymmetricLeastSquaresBaseline(avg, 100, 0.01, 1);
    alsFit[0] = 0d;
    alsFit[alsFit.length - 1] = 0d;
    movAvg = new LinearInterpolator().interpolate(thisRtValues.toDoubleArray(),
        MovingAverage.calculate(alsFit, (int) (subtracted.length * initialBandwidth)));

    double[] corrected = new double[thisRtValues.size()];
    for (int i = 0; i < thisRtValues.size(); i++) {
      corrected[i] = thisRtValues.getDouble(i) + movAvg.value(thisRtValues.getDouble(i));
    }
    ensureMonotonicity(corrected);
    movAvg = new LinearInterpolator().interpolate(thisRtValues.toDoubleArray(), corrected);
    for (int i = 1; i < file.getNumOfScans(); i++) {
      if (movAvg.value(file.getScan(i).getRetentionTime()) <= movAvg.value(
          file.getScan(i - 1).getRetentionTime())) {
        logger.warning(
            "Cannot find monotonous calibration for file %s with bandwidth %.3f. Increasing by 0.01.".formatted(
                file.getName(), initialBandwidth));
      }
    }

    if (movAvg == null) {
      throw new IllegalStateException(
          "Cannot find monotonous calibration for file " + file.getName());
    }

    return movAvg;
  }

  private static void addFinalRt(@NotNull List<RtStandard> rtSortedStandards,
      Range<Float> fullRtRange, float averageRt, DoubleArrayList thisRtValues,
      DoubleArrayList calibratedRtValues) {

    // if this changes the rt range, we need to add an additional point.
    // we keep the change after the last standard constant.
    if (rtSortedStandards.getLast().getAverageRt() > fullRtRange.upperEndpoint()) {
      final float avgRt = rtSortedStandards.getLast().getAverageRt();
      final double offset = avgRt - averageRt;
      final float timeToLastScan = fullRtRange.upperEndpoint() - averageRt;

      thisRtValues.add(fullRtRange.upperEndpoint() + offset);
      // is this correct? this would cause slightly different max rts for all files,
      // but i cannot think of a better way to calculate this
      calibratedRtValues.add(rtSortedStandards.getLast().getAverageRt() + timeToLastScan);
    } else {
      // keep the rt range of this file as it was.
      thisRtValues.add(fullRtRange.upperEndpoint());
      calibratedRtValues.add(fullRtRange.upperEndpoint());
    }
  }

  /*private PolynomialSplineFunction getInterpolatorIteratively(@NotNull RawDataFile file,
      double bandwidth, DoubleArrayList thisRtValues, DoubleArrayList calibratedRtValues) {
    @NotNull PolynomialSplineFunction loess = null;
    for (double bw = bandwidth; bw < 1; bw += 0.01) {
      LoessInterpolator loessInterpolator = new LoessInterpolator(bw, iterations);
      var tempLoess = loessInterpolator.interpolate(thisRtValues.toDoubleArray(),
          calibratedRtValues.toDoubleArray());
      boolean fail = false;
      for (int i = 1; i < file.getNumOfScans(); i++) {
        if (tempLoess.value(file.getScan(i - 1).getRetentionTime()) >= tempLoess.value(
            file.getScan(i).getRetentionTime())) {
          logger.warning(
              "Cannot find monotonous calibration for file %s with bandwidth %.3f. Increasing by 0.01.".formatted(
                  file.getName(), bw));
          fail = true;
          break;
        }
      }
      if (!fail) {
        loess = tempLoess;
        this.optimisedBandwidth = bw;
        break;
      }
    }
    if (loess == null) {
      throw new InvalidRtCalibrationParametersException(bandwidth, file, calibratedRtValues.size());
    }

    return loess;
  }*/

//  public float getCorrectedRtLoess(float originalRt) {
//    if (!loess.isValidPoint(originalRt)) {
//      return originalRt;
//    }
//    return (float) loess.value(originalRt);
//  }

  @Nullable
  public RawDataFile getRawDataFile() {
    final RawDataFile file = filePlaceholder.getMatchingFile();
    return file;
  }

//  public double getOptimisedBandwidth() {
//    return optimisedBandwidth;
//  }

//  public float getCorrectedRtLinear(float originalRt) {
//    if (!linear.isValidPoint(originalRt)) {
//      return originalRt;
//    }
//    return (float) linear.value(originalRt);
//  }

  public float getCorrectedRtMovAvg(float originalRt) {
    if (!movAvg.isValidPoint(originalRt)) {
      return originalRt;
    }
    return (float) movAvg.value(originalRt);
  }

  private static void ensureMonotonicity(double[] values) {
    if (values.length <= 1) {
      return;
    }

    for (int i = 1; i < values.length; i++) {
      if (values[i] <= values[i - 1]) {
        // Add a small increment to maintain monotonicity
        values[i] = values[i - 1] + 0.001; // 1 millisecond increment
      }
    }
  }

  public PolynomialSplineFunction getSplineFunction() {
    return movAvg;
  }
}
