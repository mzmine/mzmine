/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results;

import java.text.NumberFormat;
import org.jfree.data.xy.XYDataset;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datasets.ScanDataSet;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.renderers.SpectraItemLabelGenerator;

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
      label = createLabel(((DPPResultsDataSet) dataset).getDataPoints()[item]);
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
    // System.out.println(dp.getMZ() + " has " + keys.length + " keys " + keys.toString());

    if (dp.resultTypeExists(ResultType.SUMFORMULA)) {
      for (DPPResult<?> r : dp.getAllResultsByType(ResultType.SUMFORMULA)) {
        if (r instanceof DPPSumFormulaResult) {
          formulas += r.generateLabel() + " ";
        }
      }
    }
    
    label = mz + " ";

    if (!formulas.equals(""))
      label += "\n" + formulas;

    return label;
  }
}
