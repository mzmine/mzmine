/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.gui.chartbasics.simplechart.providers.impl.series;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.gui.chartbasics.simplechart.providers.SimpleXYProvider;
import io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges.ExtractMzRangesIonSeriesFunction;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.awt.Color;
import java.util.List;
import javafx.beans.property.Property;
import org.jetbrains.annotations.NotNull;

public class MzRangeChromatogramProvider extends SimpleXYProvider {

  private final Range<Double> mz;
  private final List<? extends Scan> scans;

  public MzRangeChromatogramProvider(Range<Double> mz, List<? extends Scan> scans, String seriesKey,
      Color awt) {
    super(seriesKey, awt);

    this.mz = mz;
    this.scans = scans;
  }

  @Override
  public void computeValues(Property<TaskStatus> status) {
    final ExtractMzRangesIonSeriesFunction extraction = new ExtractMzRangesIonSeriesFunction(
        scans.getFirst().getDataFile(), scans, List.of(mz), ScanDataType.MASS_LIST, null);
    final @NotNull BuildingIonSeries[] result = extraction.get();
    if (result.length == 0) {
      throw new RuntimeException("Could not calculate chromatogram");
    }
    setyValues(result[0].getIntensities());
  }

  @Override
  public double getDomainValue(int index) {
    return scans.get(index).getRetentionTime();
  }

  @Override
  public int getValueCount() {
    return scans.size();
  }
}
