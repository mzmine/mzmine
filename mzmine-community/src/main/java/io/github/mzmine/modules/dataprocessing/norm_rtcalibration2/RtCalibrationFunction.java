package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.LoessInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;

public class RtCalibrationFunction {

  private final @NotNull PolynomialSplineFunction linear;
  private final RawDataFile file;
  private final @NotNull PolynomialSplineFunction loess;

  public RtCalibrationFunction(FeatureList flist, List<RtStandard> standards) {
    file = flist.getRawDataFiles().getFirst();
    final Range<Float> fullRtRange = file.getDataRTRange();

    DoubleArrayList thisRtValues = new DoubleArrayList();
    DoubleArrayList calibratedRtValues = new DoubleArrayList();

    // never go below zero, so make 0 the first point of interpolation.
    thisRtValues.add(0d);
    calibratedRtValues.add(0d);

    for (RtStandard standard : standards) {
      thisRtValues.add(standard.standards().get(flist).getAverageRT());
      calibratedRtValues.add(standard.getMedianRt());
    }

    if (standards.getLast().getMedianRt() > fullRtRange.upperEndpoint()) {
      // if this changes the rt range, we need to add an additional point.
      // we keep the change after the last standard constant.
      final FeatureListRow row = standards.getLast().standards().get(flist);
      final float medianRt = standards.getLast().getMedianRt();
      final double offset = medianRt - row.getAverageRT();
      final float timeToLastScan = fullRtRange.upperEndpoint() - row.getAverageRT();

      thisRtValues.add(fullRtRange.upperEndpoint() + offset);
      // is this correct? this would cause slightly different max rts for all files,
      // but i cannot think of a better way to calculate this
      calibratedRtValues.add(standards.getLast().getMedianRt() + timeToLastScan);
    } else {
      // keep the rt range of this file as it was.
      thisRtValues.add(fullRtRange.upperEndpoint());
      calibratedRtValues.add(fullRtRange.upperEndpoint());
    }

    linear = new LinearInterpolator().interpolate(thisRtValues.toDoubleArray(),
        calibratedRtValues.toDoubleArray());
    LoessInterpolator loessInterpolator = new LoessInterpolator(0.2, 1);
    loess = loessInterpolator.interpolate(thisRtValues.toDoubleArray(),
        calibratedRtValues.toDoubleArray());
  }

  public RtCalibrationFunction(@NotNull RawDataFile file,
      @NotNull List<@NotNull RtCalibrationFunction> functions, @NotNull MetadataTable metadata, RtStandard standard) {

    final long totalTimeDistance =
        Math.abs(runDate.until(nextRunDate, ChronoUnit.SECONDS)) + Math.abs(
            runDate.until(previousRunDate, ChronoUnit.SECONDS));
    final double previousWeight =
        (double) Math.abs(runDate.until(nextRunDate, ChronoUnit.SECONDS)) / totalTimeDistance;
    final double nextRunWeight =
        (double) Math.abs(runDate.until(previousRunDate, ChronoUnit.SECONDS)) / totalTimeDistance;


  }

  public float getCorrectedRtLinear(float originalRt) {
    if (!linear.isValidPoint(originalRt)) {
      return originalRt;
    }
    return (float) linear.value(originalRt);
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
}
