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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results;

import java.text.NumberFormat;
import org.jfree.data.xy.XYDataset;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import io.github.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraItemLabelGenerator;

public class DPPResultsLabelGenerator extends SpectraItemLabelGenerator {

  public DPPResultsLabelGenerator(SpectraPlot plot) {
    super(plot);
  }

  /**
   * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
   *      int, int)
   */
  public String generateLabel(XYDataset dataset, int series, int item) {

    // X and Y values of current data point
    double originalX = dataset.getX(series, item).doubleValue();
    double originalY = dataset.getY(series, item).doubleValue();

    // Calculate data size of 1 screen pixel
    double xLength = (double) plot.getXYPlot().getDomainAxis().getRange().getLength();
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
    if (dataset instanceof ScanDataSet) {
      label = ((ScanDataSet) dataset).getAnnotation(item);
    } else if (dataset instanceof DPPResultsDataSet) {
      DataPoint[] dps = ((DPPResultsDataSet) dataset).getDataPoints();
      if (dps[item] instanceof ProcessedDataPoint) {
        ProcessedDataPoint p = (ProcessedDataPoint) dps[item];
        label = createLabel(p);
      }
    }

    if (label == null || label.equals("")) {
      double mzValue = dataset.getXValue(series, item);
      label = mzFormat.format(mzValue);
    }

    return label;
  }

  private String createLabel(ProcessedDataPoint dp) {
    String label = "";

    if (!dp.hasResults())
      return label;

    NumberFormat mzForm = MZmineCore.getConfiguration().getMZFormat();
    String mz;
    String formulas = "";
    mz = mzForm.format(dp.getMZ());
    // System.out.println(dp.getMZ() + " has " + keys.length + " keys " +
    // keys.toString());

    if (dp.resultTypeExists(ResultType.SUMFORMULA)) {
      for (DPPResult<?> r : dp.getAllResultsByType(ResultType.SUMFORMULA)) {
        if (r instanceof DPPSumFormulaResult) {
          formulas += r.toString() + " ";
        }
      }
    }

    label = mz + " ";

    if (!formulas.equals(""))
      label += "\n" + formulas;

    return label;
  }
}
