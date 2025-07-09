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
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RTMeasure;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RtStandard;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Basic implementation of a RT correction on a raw data file basis. Each instance is specific for a
 * single raw data file. The implementations of this class must have an associated
 * {@link RawFileRtCorrectionModule} and must be listed in {@link RtCorrectionFunctions}. A
 * correction can be interpolated between two neighbouring files, or created specifically for a set
 * of standards.
 * {@link RawFileRtCorrectionModule#createFromStandards(FeatureList, List, ParameterSet)} or
 * {@link RawFileRtCorrectionModule#createInterpolated(RawDataFile, List,
 * AbstractRtCorrectionFunction, double, AbstractRtCorrectionFunction, double, ParameterSet)}
 */
public abstract class AbstractRtCorrectionFunction {

  protected final RawDataFilePlaceholder filePlaceholder;

  public AbstractRtCorrectionFunction(RawDataFilePlaceholder rawDataFilePlaceholder) {
    filePlaceholder = rawDataFilePlaceholder;
  }

  public AbstractRtCorrectionFunction(FeatureList flist) {
    if (flist.getNumberOfRawDataFiles() > 1) {
      throw new IllegalStateException(
          "Cannot create a RtCorrectionFunction for a feature list with more than one data file (%s)".formatted(
              flist.getName()));
    }

    final RawDataFile file = flist.getRawDataFiles().getFirst();
    filePlaceholder = new RawDataFilePlaceholder(file);
  }

  protected static void ensureMonotonicity(double[] values) {
    if (values.length <= 1) {
      return;
    }

    for (int i = 1; i < values.length; i++) {
      if (values[i] <= values[i - 1]) {
        // Add a small increment to maintain monotonicity (10 ms). Math.nextUp is not enough.
        values[i] = values[i - 1] + 0.0001666;
      }
    }
  }

  /**
   * Adds a final RT pair for the end of the file (sets boundaries for calibration)
   *
   * @param rtSortedStandards      modified
   * @param fullRtRange            full rt range of the file to be calibrated
   *                               {@link RawDataFile#getDataRTRange()}
   * @param finalStandardAverageRt the rt of the final standard in this data file
   * @param thisRtValues           modified
   * @param calibratedRtValues     modified
   */
  protected static void addFinalRt(@NotNull List<RtStandard> rtSortedStandards,
      Range<Float> fullRtRange, float finalStandardAverageRt, DoubleArrayList thisRtValues,
      DoubleArrayList calibratedRtValues, RTMeasure rtMeasure) {

    // we keep the change after the last standard constant.
    final float correctedLastStandardRt = rtSortedStandards.getLast().getRt(rtMeasure);
    final double offset = correctedLastStandardRt - finalStandardAverageRt;
    final float timeToLastScan = fullRtRange.upperEndpoint() - finalStandardAverageRt;

    thisRtValues.add(fullRtRange.upperEndpoint());
    // is this correct? this would cause slightly different max rts for all files,
    // but i cannot think of a better way to calculate this
    calibratedRtValues.add(rtSortedStandards.getLast().getRt(rtMeasure) + timeToLastScan + offset);
  }

  @Nullable
  public RawDataFile getRawDataFile() {
    return getRawDataFilePlaceholder().getMatchingFile();
  }

  @NotNull
  protected RawDataFilePlaceholder getRawDataFilePlaceholder() {
    return filePlaceholder;
  }

  /**
   * Corrects the RT of a scan to a new RT
   */
  public abstract float getCorrectedRt(float originalRt);

  public abstract void saveToXML(Element calibrationFunctionElement);

  public abstract RawFileRtCorrectionModule getRtCalibrationModule();
}
