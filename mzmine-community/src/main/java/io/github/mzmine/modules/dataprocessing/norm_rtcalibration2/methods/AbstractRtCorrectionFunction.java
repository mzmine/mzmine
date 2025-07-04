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
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.RtStandard;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public abstract class AbstractRtCorrectionFunction {

  protected final RawDataFilePlaceholder filePlaceholder;

  public AbstractRtCorrectionFunction(RawDataFilePlaceholder rawDataFilePlaceholder) {
    filePlaceholder = rawDataFilePlaceholder;
  }

  public AbstractRtCorrectionFunction(FeatureList flist) {
    if (flist.getNumberOfRawDataFiles() > 1) {
      throw new IllegalStateException(
          "Cannot create a RtCalibrationFunction for a feature list with more than one data file (%s)".formatted(
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
        // Add a small increment to maintain monotonicity
        values[i] = Math.nextUp(values[i - 1]);
      }
    }
  }

  /**
   * Adds a final RT pair for the end of the file (sets boundaries for calibration)
   *
   * @param rtSortedStandards      modified
   * @param fullRtRange            full rt range of the file to be calibrated
   *                               {@link RawDataFile#getDataRTRange()}
   * @param finalStandardAverageRt the rt of the final standard for the data file
   * @param thisRtValues           modified
   * @param calibratedRtValues     modified
   */
  protected static void addFinalRt(@NotNull List<RtStandard> rtSortedStandards,
      Range<Float> fullRtRange, float finalStandardAverageRt, DoubleArrayList thisRtValues,
      DoubleArrayList calibratedRtValues) {

    // if this changes the rt range, we need to add an additional point.
    // we keep the change after the last standard constant.
    if (rtSortedStandards.getLast().getAverageRt() > fullRtRange.upperEndpoint()) {
      final float avgRt = rtSortedStandards.getLast().getAverageRt();
      final double offset = avgRt - finalStandardAverageRt;
      final float timeToLastScan = fullRtRange.upperEndpoint() - finalStandardAverageRt;

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

  @Nullable
  public RawDataFile getRawDataFile() {
    return getRawDataFilePlaceholder().getMatchingFile();
  }

  @NotNull
  protected RawDataFilePlaceholder getRawDataFilePlaceholder() {
    return filePlaceholder;
  }

  public abstract float getCorrectedRt(float originalRt);

  public abstract void saveToXML(Element calibrationFunctionElement);

  public abstract RawFileRtCorrectionModule getRtCalibrationModule();
}
