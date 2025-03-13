package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;

public class RtCalibrationFunction {

  private static final Logger logger = Logger.getLogger(RtCalibrationFunction.class.getName());

  private final RawDataFile file;
  private final @NotNull PolynomialSplineFunction loess;
  private final @NotNull PolynomialSplineFunction linear;
  private double optimisedBandwidth = 0d;
  private final int iterations = 3;

  public RtCalibrationFunction(FeatureList flist, List<RtStandard> rtSortedStandards,
      double bandwidth) {
    file = flist.getRawDataFiles().getFirst();
    final Range<Float> fullRtRange = file.getDataRTRange();

    DoubleArrayList thisRtValues = new DoubleArrayList();
    DoubleArrayList calibratedRtValues = new DoubleArrayList();

    // never go below zero, so make 0 the first point of interpolation.
    thisRtValues.add(0d);
    calibratedRtValues.add(0d);

    for (RtStandard standard : rtSortedStandards) {
      thisRtValues.add(standard.standards().get(file).getAverageRT());
      calibratedRtValues.add(standard.getMedianRt());
    }

    final FeatureListRow row = rtSortedStandards.getLast().standards().get(file);
    final float averageRT = row.getAverageRT();
    addFinalRt(rtSortedStandards, fullRtRange, averageRT, thisRtValues, calibratedRtValues);

    loess = getInterpolatorIteratively(file, bandwidth, thisRtValues, calibratedRtValues);
    linear = new LinearInterpolator().interpolate(thisRtValues.toDoubleArray(),
        calibratedRtValues.toDoubleArray());
  }

  public RtCalibrationFunction(@NotNull final RawDataFile file,
      @NotNull final List<RtStandard> rtSortedStandards,
      @NotNull final RtCalibrationFunction previousRunCalibration, final double previousRunWeight,
      @NotNull final RtCalibrationFunction nextRunCalibration, final double nextRunWeight,
      double bandwidth) {

    this.file = file;
    final Range<Float> fullRtRange = file.getDataRTRange();

    final DoubleArrayList thisRtValues = new DoubleArrayList();
    final DoubleArrayList calibratedRtValues = new DoubleArrayList();

    // never go below zero, so make 0 the first point of interpolation.
    thisRtValues.add(0d);
    calibratedRtValues.add(0d);

    final RawDataFile prevFile = previousRunCalibration.getRawDataFile();
    final RawDataFile nextFile = nextRunCalibration.getRawDataFile();
    for (RtStandard standard : rtSortedStandards) {
      final double previous = standard.standards().get(prevFile).getAverageRT() * previousRunWeight;
      final double next = standard.standards().get(nextFile).getAverageRT() * nextRunWeight;

      thisRtValues.add(previous + next);
      calibratedRtValues.add(standard.getMedianRt());
    }

    final double previous =
        rtSortedStandards.getLast().standards().get(prevFile).getAverageRT() * previousRunWeight;
    final double next =
        rtSortedStandards.getLast().standards().get(nextFile).getAverageRT() * nextRunWeight;
    final float lastStandardRt = (float) (previous + next);

    addFinalRt(rtSortedStandards, fullRtRange, lastStandardRt, thisRtValues, calibratedRtValues);

    loess = getInterpolatorIteratively(file, bandwidth, thisRtValues, calibratedRtValues);
    linear = new LinearInterpolator().interpolate(thisRtValues.toDoubleArray(),
        calibratedRtValues.toDoubleArray());
  }

  private static void addFinalRt(@NotNull List<RtStandard> rtSortedStandards,
      Range<Float> fullRtRange, float averageRt, DoubleArrayList thisRtValues,
      DoubleArrayList calibratedRtValues) {

    // if this changes the rt range, we need to add an additional point.
    // we keep the change after the last standard constant.
    if (rtSortedStandards.getLast().getMedianRt() > fullRtRange.upperEndpoint()) {
      final float medianRt = rtSortedStandards.getLast().getMedianRt();
      final double offset = medianRt - averageRt;
      final float timeToLastScan = fullRtRange.upperEndpoint() - averageRt;

      thisRtValues.add(fullRtRange.upperEndpoint() + offset);
      // is this correct? this would cause slightly different max rts for all files,
      // but i cannot think of a better way to calculate this
      calibratedRtValues.add(rtSortedStandards.getLast().getMedianRt() + timeToLastScan);
    } else {
      // keep the rt range of this file as it was.
      thisRtValues.add(fullRtRange.upperEndpoint());
      calibratedRtValues.add(fullRtRange.upperEndpoint());
    }
  }

  private PolynomialSplineFunction getInterpolatorIteratively(@NotNull RawDataFile file,
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
  }

  public float getCorrectedRtLoess(float originalRt) {
    if (!loess.isValidPoint(originalRt)) {
      return originalRt;
    }
    return (float) loess.value(originalRt);
  }

  public RawDataFile getRawDataFile() {
    return file;
  }

  public double getOptimisedBandwidth() {
    return optimisedBandwidth;
  }

  public float getCorrectedRtLinear(float originalRt) {
    if (!linear.isValidPoint(originalRt)) {
      return originalRt;
    }
    return (float) linear.value(originalRt);
  }
}
