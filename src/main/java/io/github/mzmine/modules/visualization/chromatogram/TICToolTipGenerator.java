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

package io.github.mzmine.modules.visualization.chromatogram;

import io.github.mzmine.datamodel.features.Feature;
import java.text.NumberFormat;
import java.util.Map;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.main.MZmineCore;

/**
 * Tooltip generator for TIC visualizer
 */
public class TICToolTipGenerator implements XYToolTipGenerator {

  private final NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
  private final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
  private final NumberFormat intensityFormat = MZmineCore.getConfiguration().getIntensityFormat();

  @Override
  public String generateToolTip(final XYDataset dataSet, final int series, final int item) {

    final String toolTip;

    final double rtValue = dataSet.getXValue(series, item);
    final double intValue = dataSet.getYValue(series, item);

    if (dataSet instanceof TICDataSet) {

      final TICDataSet ticDataSet = (TICDataSet) dataSet;

      toolTip = "Scan #" + ticDataSet.getScan(item).getScanNumber() + "\nRetention time: "
          + rtFormat.format(rtValue) + "\nBase peak m/z: "
          + mzFormat.format(ticDataSet.getZValue(series, item)) + "\nIntensity: "
          + intensityFormat.format(intValue);

    } else if (dataSet instanceof FeatureDataSet) {

      final FeatureDataSet featureDataSet = (FeatureDataSet) dataSet;
      final Feature feature = featureDataSet.getFeature();
      FeatureInformation featureInfo = null;
      if (feature != null) {
        featureInfo = feature.getFeatureInformation();
      }

      final String label = featureDataSet.getName();
      String text = label == null || label.length() == 0 ? "" : label + '\n';
      text += "Retention time: " + rtFormat.format(rtValue) + "\nm/z: "
          + mzFormat.format(featureDataSet.getMZ(item)) + "\nIntensity: "
          + intensityFormat.format(intValue);

      NumberFormat numberFormat = NumberFormat.getInstance();

      if (featureInfo != null)
        for (Map.Entry<String, String> e : featureInfo.getAllProperties().entrySet()) {
          try {
            double value = Double.parseDouble(e.getValue());
            text += "\n" + e.getKey() + ": " + numberFormat.format(value);
          } catch (NullPointerException | NumberFormatException exception) {
            continue;
          }
        }

      toolTip = text;

    } else {

      toolTip = "Retention time: " + rtFormat.format(rtValue) + "\nIntensity: "
          + intensityFormat.format(intValue);
    }

    return toolTip;
  }
}
