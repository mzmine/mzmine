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

package io.github.mzmine.modules.visualization.twod;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.text.NumberFormat;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import io.github.mzmine.main.MZmineCore;

/**
 * Tooltip generator for 2D visualizer
 */
class FeatureToolTipGenerator implements XYToolTipGenerator {

  private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

  /**
   * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  public String generateToolTip(XYDataset dataset, int series, int item) {

    FeatureDataSet featureDataSet = (FeatureDataSet) dataset;
    FeatureDataPoint dataPoint = featureDataSet.getDataPoint(series, item);

    FeatureList featureList = featureDataSet.getFeatureList();
    Feature feature = featureDataSet.getFeature(series);
    FeatureListRow row = featureList.getFeatureRow(feature);
    float rtValue = dataPoint.getRT();
    double intValue = dataPoint.getIntensity();
    double mzValue = dataPoint.getMZ();
    Scan scanNumber = dataPoint.getScan();

    String toolTip =
        "Feature: " + feature + "\nStatus: " + feature.getFeatureStatus() + "\nFeature list row: " + row
            + "\nScan #" + scanNumber + "\nRetention time: " + rtFormat.format(rtValue) + "\nm/z: "
            + mzFormat.format(mzValue) + "\nIntensity: " + intensityFormat.format(intValue);

    return toolTip;
  }

}
