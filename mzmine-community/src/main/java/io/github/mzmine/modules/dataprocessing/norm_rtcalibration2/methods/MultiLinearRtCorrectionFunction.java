/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_baselinecorrection.als.AsymmetricLeastSquaresCorrection;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.MovingAverage;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTMeasure;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RtStandard;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.util.ParsingUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MultiLinearRtCorrectionFunction extends AbstractRtCorrectionFunction {

  private static final Logger logger = Logger.getLogger(MultiLinearRtCorrectionFunction.class.getName());

  private final PolynomialSplineFunction movAvg;

  public MultiLinearRtCorrectionFunction(RawDataFilePlaceholder file, PolynomialSplineFunction function) {
    super(new RawDataFilePlaceholder(file));
    this.movAvg = function;
  }

  public MultiLinearRtCorrectionFunction(FeatureList flist, List<RtStandard> rtSortedStandards,
      double bandwidth, RTMeasure rtMeasure) {
    super(flist);

    final RawDataFile file = flist.getRawDataFiles().getFirst();
    final Range<Float> fullRtRange = file.getDataRTRange();
    DoubleArrayList thisRtValues = new DoubleArrayList();
    DoubleArrayList standardRtValues = new DoubleArrayList();

    // never go below zero, so make 0 the first point of interpolation.
    thisRtValues.add(0d);
    standardRtValues.add(0d);

    for (RtStandard standard : rtSortedStandards) {
      thisRtValues.add(standard.standards().get(file).getAverageRT());
      standardRtValues.add(standard.getRt(rtMeasure));
    }

    final FeatureListRow lastStandard = rtSortedStandards.getLast().standards().get(file);
    final float lastStandardAverageRT = lastStandard.getAverageRT();
    addFinalRt(rtSortedStandards, fullRtRange, lastStandardAverageRT, thisRtValues,
        standardRtValues, rtMeasure);

    movAvg = getInterpolatorIteratively(bandwidth, standardRtValues, thisRtValues);
  }

  public MultiLinearRtCorrectionFunction(@NotNull final RawDataFile file,
      @NotNull final List<RtStandard> rtSortedStandards,
      @NotNull final MultiLinearRtCorrectionFunction previousRunCalibration, final double previousRunWeight,
      @NotNull final MultiLinearRtCorrectionFunction nextRunCalibration, final double nextRunWeight,
      double bandwidth, RTMeasure rtMeasure) {
    super(new RawDataFilePlaceholder(file));

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
      standardRtValues.add(standard.getRt(rtMeasure));
    }

    final double previous =
        rtSortedStandards.getLast().standards().get(prevFile).getAverageRT() * previousRunWeight;
    final double next =
        rtSortedStandards.getLast().standards().get(nextFile).getAverageRT() * nextRunWeight;
    final float lastStandardRt = (float) (previous + next);

    addFinalRt(rtSortedStandards, fullRtRange, lastStandardRt, thisRtValues, standardRtValues, rtMeasure);

    movAvg = getInterpolatorIteratively(bandwidth, standardRtValues, thisRtValues);
  }

  @NotNull
  private PolynomialSplineFunction getInterpolatorIteratively(double initialBandwidth,
      DoubleArrayList calibratedRtValues, DoubleArrayList thisRtValues) {
    final RawDataFile file = getRawDataFilePlaceholder().getMatchingFile();
    PolynomialSplineFunction movAvg = null;
    final double[] subtracted = AsymmetricLeastSquaresCorrection.subtract(
        calibratedRtValues.toDoubleArray(), thisRtValues.toDoubleArray());
    subtracted[0] = 0d; // ensure the first point is not shifted to keep all RTs > 0

    double[] avg = MovingAverage.calculate(subtracted,
        Math.max(1, (int) (subtracted.length * initialBandwidth)));
    double[] alsFit = AsymmetricLeastSquaresCorrection.asymmetricLeastSquaresBaseline(avg, 100,
        0.01, 1);
    alsFit[0] = 0d; // ensure the first point is not shifted to keep all RTs > 0

    movAvg = new LinearInterpolator().interpolate(thisRtValues.toDoubleArray(),
        MovingAverage.calculate(alsFit, (int) (subtracted.length * initialBandwidth)));

    double[] corrected = new double[thisRtValues.size()];
    for (int i = 0; i < thisRtValues.size(); i++) {
      corrected[i] = thisRtValues.getDouble(i) + movAvg.value(thisRtValues.getDouble(i));
    }
    ensureMonotonicity(corrected);
    movAvg = new LinearInterpolator().interpolate(thisRtValues.toDoubleArray(), corrected);
    for (int i = 1; i < file.getNumOfScans(); i++) {
      final float thisOriginalRt = file.getScan(i).getRetentionTime();
      final double thisCorrectedRt = movAvg.value(thisOriginalRt);
      final float previousRt = file.getScan(i - 1).getRetentionTime();
      final double correctedPreviousRt = movAvg.value(previousRt);
      if (thisCorrectedRt <= correctedPreviousRt) {
        logger.warning(
            "Cannot find monotonous calibration for file %s with bandwidth %.3f.".formatted(
                file.getName(), initialBandwidth));
        movAvg = null;
        break;
      }
    }

    if (movAvg == null) {
      throw new IllegalStateException(
          "Cannot find monotonous calibration for file " + file.getName());
    }

    return movAvg;
  }

  @Override
  public float getCorrectedRt(float originalRt) {
    if (!movAvg.isValidPoint(originalRt)) {
      return originalRt;
    }
    return (float) movAvg.value(originalRt);
  }

  @Override
  public void saveToXML(Element correctionFunctionElement) {
    final Document doc = correctionFunctionElement.getOwnerDocument();
    final Element spline = ParsingUtils.createSplineFunctionXmlElement(doc, getSplineFunction());
    correctionFunctionElement.appendChild(spline);
  }

  @Override
  public RawFileRtCorrectionModule getRtCalibrationModule() {
    return RtCorrectionFunctions.MultiLinearCorrection.getModuleInstance();
  }

  public PolynomialSplineFunction getSplineFunction() {
    return movAvg;
  }
}
