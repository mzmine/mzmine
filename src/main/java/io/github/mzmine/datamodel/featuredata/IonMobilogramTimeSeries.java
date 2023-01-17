/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.datamodel.featuredata;

import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.data_access.BinningMobilogramDataAccess;
import io.github.mzmine.datamodel.featuredata.impl.ModifiableSpectra;
import io.github.mzmine.datamodel.featuredata.impl.SummedIntensityMobilitySeries;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores data points from ion mobility frames (summed across the mobility axis at one retention
 * time). Used as a tag to do instanceof checks with MsTimeSeries objects in the feature model.
 *
 * @author https://github.com/SteffenHeu
 */
public interface IonMobilogramTimeSeries extends IonTimeSeries<Frame>, ModifiableSpectra<Frame> {

  @Override
  default float getRetentionTime(int index) {
    return getSpectrum(index).getRetentionTime();
  }

  IonMobilogramTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Frame> subset, @NotNull BinningMobilogramDataAccess mobilogramBinning);

  List<IonMobilitySeries> getMobilograms();

  default IonMobilitySeries getMobilogram(int index) {
    return getMobilograms().get(index);
  }

  /**
   * @param frame The frame
   * @return The {@link IonMobilitySeries} for the given frame, null if there is no series for the
   * given frame.
   */
  @Nullable
  default IonMobilitySeries getMobilogram(@Nullable final Frame frame) {
    final int index = getSpectra().indexOf(frame);
    return index != -1 ? getMobilogram(index) : null;
  }

  SummedIntensityMobilitySeries getSummedMobilogram();

  /**
   * Allows creation of a new {@link IonMobilogramTimeSeries} with processed {@link
   * SummedIntensityMobilitySeries}.
   *
   * @param storage
   * @param summedMobilogram
   * @return
   */
  IonMobilogramTimeSeries copyAndReplace(@Nullable MemoryMapStorage storage,
      @NotNull SummedIntensityMobilitySeries summedMobilogram);

  /**
   * @param scan
   * @return The intensity value for the given scan or 0 if the no intensity was measured at that
   * scan.
   */
  @Override
  default double getIntensityForSpectrum(Frame scan) {
    int index = getSpectra().indexOf(scan);
    if (index != -1) {
      return getIntensity(index);
    }
    return 0;
  }

  /**
   * @param scan
   * @return The mz for the given scan or 0 if no intensity was measured at that scan.
   */
  @Override
  default double getMzForSpectrum(Frame scan) {
    int index = getSpectra().indexOf(scan);
    if (index != -1) {
      return getMZ(index);
    }
    return 0;
  }

  @Override
  IonMobilogramTimeSeries subSeries(@Nullable MemoryMapStorage storage,
      @NotNull List<Frame> subset);
}
