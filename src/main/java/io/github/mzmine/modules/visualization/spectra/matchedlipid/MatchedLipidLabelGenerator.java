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

package io.github.mzmine.modules.visualization.spectra.matchedlipid;

import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidAnnotationLevel;
import io.github.mzmine.modules.dataprocessing.id_lipididentification.lipids.LipidFragment;
import java.util.List;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

public class MatchedLipidLabelGenerator implements XYItemLabelGenerator {

  /*
   * Number of screen pixels to reserve for each label, so that the labels do not overlap
   */
 private static final int POINTS_RESERVE_X = 100;

  private final ChartViewer plot;
  private final List<LipidFragment> fragments;

  public MatchedLipidLabelGenerator(ChartViewer plot,List<LipidFragment> fragments) {
    this.plot = plot;
    this.fragments = fragments;
  }


  /**
   * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  @Override
  public String generateLabel(XYDataset dataset, int series, int item) {

    // X and Y values of current data point
    double originalX = dataset.getX(series, item).doubleValue();
    double originalY = dataset.getY(series, item).doubleValue();

    // Calculate data size of 1 screen pixel
    double xLength = plot.getChart().getXYPlot().getDomainAxis().getRange().getLength();
    double pixelX = xLength / plot.getWidth();

    // Size of data set
    int itemCount = dataset.getItemCount(series);

    // Search for data points higher than this one in the interval
    // from limitLeft to limitRight
    double limitLeft = originalX - ((POINTS_RESERVE_X / 2) * pixelX);
    double limitRight = originalX + ((POINTS_RESERVE_X / 2) * pixelX);

    // Iterate data points to the left and right
    for (int i = 1; (item - i > 0) || (item + i < itemCount); i++) {

      // If we get out of the limit we can stop searching
      if ((item - i > 0) && (dataset.getXValue(series, item - i) < limitLeft)
          && ((item + i >= itemCount) || (dataset.getXValue(series, item + i) > limitRight)))
        break;

      if ((item + i < itemCount) && (dataset.getXValue(series, item + i) > limitRight)
          && ((item - i <= 0) || (dataset.getXValue(series, item - i) < limitLeft)))
        break;

      // If we find higher data point, bail out
      if ((item - i > 0) && (originalY <= dataset.getYValue(series, item - i)))
        return null;

      if ((item + i < itemCount) && (originalY <= dataset.getYValue(series, item + i)))
        return null;

    }

    // Create label
    String label = null;
    if (dataset.getSeriesKey(1).equals("Matched Signals")) {
      if (fragments != null) {
        return buildFragmentAnnotation(fragments.get(item));
      } else {
        return null;
      }
    }
    return null;

  }

  private String buildFragmentAnnotation(LipidFragment lipidFragment) {
    StringBuilder sb = new StringBuilder();
    if (lipidFragment.getLipidFragmentInformationLevelType()
        .equals(LipidAnnotationLevel.MOLECULAR_SPECIES_LEVEL)) {
      sb.append(lipidFragment.getLipidChainType().toString()).append(" ")
          .append(lipidFragment.getChainLength()).append(":")
          .append(lipidFragment.getNumberOfDBEs());
    } else {
      sb.append(lipidFragment.getRuleType().toString()).append(" ").append(lipidFragment.getMzExact());
    }
    return sb.toString();
  }

}
