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

package io.github.mzmine.modules.visualization.spectra.simplespectra.renderers;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ExtendedIsotopePatternDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.IsotopesDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.PeakListDataSet;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Tooltip generator for raw data points
 */
public class SpectraToolTipGenerator implements XYToolTipGenerator {

  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();
  private NumberFormat percentFormat = new DecimalFormat("0.00");

  /**
   * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  @Override
  public String generateToolTip(XYDataset dataset, int series, int item) {

    double intValue = dataset.getYValue(series, item);
    double mzValue = dataset.getXValue(series, item);

    if (dataset instanceof PeakListDataSet) {

      PeakListDataSet peakListDataSet = (PeakListDataSet) dataset;

      Feature peak = peakListDataSet.getPeak(series, item);

      FeatureList peakList = peakListDataSet.getFeatureList();
      FeatureListRow row = peakList.getFeatureRow(peak);

      String tooltip = "Peak: " + peak + "\nStatus: " + peak.getFeatureStatus()
          + "\nFeature list row: " + row + "\nData point m/z: " + mzFormat.format(mzValue)
          + "\nData point intensity: " + intensityFormat.format(intValue);

      return tooltip;

    }

    if (dataset instanceof IsotopesDataSet) {

      IsotopesDataSet isotopeDataSet = (IsotopesDataSet) dataset;

      IsotopePattern pattern = isotopeDataSet.getIsotopePattern();

      double relativeIntensity = 0.0;
      Integer basePeak = pattern.getBasePeakIndex();
      if (basePeak == null) {
        relativeIntensity = intValue / pattern.getBasePeakIntensity() * 100;
      }

      String tooltip = "Isotope pattern: " + pattern.getDescription() + "\nStatus: "
          + pattern.getStatus() + "\nData point m/z: " + mzFormat.format(mzValue)
          + "\nData point intensity: " + intensityFormat.format(intValue) + "\nRelative intensity: "
          + percentFormat.format(relativeIntensity) + "%";

      return tooltip;

    }

    if (dataset instanceof ExtendedIsotopePatternDataSet) {
      return "Isotope pattern: "
          + ((ExtendedIsotopePatternDataSet) dataset).getIsotopePattern().getDescription()
          + "\nm/z: " + mzFormat.format(mzValue) + "\nIdentity: "
          + ((ExtendedIsotopePatternDataSet) dataset).getItemDescription(series, item)
          + "\nRelative intensity: "
          + percentFormat.format((dataset.getY(series, item).doubleValue() * 100)) + "%";
    }

    String tooltip =
        "m/z: " + mzFormat.format(mzValue) + "\nIntensity: " + intensityFormat.format(intValue);

    return tooltip;

  }

}
