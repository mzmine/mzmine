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

package io.github.mzmine.gui.chartbasics.chartutils;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.text.NumberFormat;
import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import io.github.mzmine.main.MZmineCore;

/**
 * Tooltip generator for any type of scatter plots
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class ScatterPlotToolTipGenerator implements XYZToolTipGenerator, PublicCloneable {

  private String xAxisLabel, yAxisLabel, zAxisLabel;
  private NumberFormat numberFormat = MZmineCore.getConfiguration().getMZFormat();
  private FeatureListRow rows[];
  private String featureIdentity;

  public ScatterPlotToolTipGenerator(String xAxisLabel, String yAxisLabel, String zAxisLabel,
      FeatureListRow rows[]) {
    super();
    this.xAxisLabel = xAxisLabel;
    this.yAxisLabel = yAxisLabel;
    this.zAxisLabel = zAxisLabel;
    this.rows = rows;

  }

  @Override
  public String generateToolTip(XYZDataset dataset, int series, int item) {
    if (rows[item].getPreferredFeatureIdentity() != null) {
      featureIdentity = rows[item].getPreferredFeatureIdentity().getName();
      return String.valueOf(featureIdentity + "\n" + xAxisLabel + ": "
          + numberFormat.format(dataset.getXValue(series, item)) + " " + yAxisLabel + ": "
          + numberFormat.format(dataset.getYValue(series, item)) + " " + zAxisLabel + ": "
          + numberFormat.format(dataset.getZValue(series, item)));
    } else {
      return String.valueOf(xAxisLabel + ": " + numberFormat.format(dataset.getXValue(series, item))
          + " " + yAxisLabel + ": " + numberFormat.format(dataset.getYValue(series, item)) + " "
          + zAxisLabel + ": " + numberFormat.format(dataset.getZValue(series, item)));
    }
  }

  @Override
  public String generateToolTip(XYDataset dataset, int series, int item) {
    if (rows[item].getPreferredFeatureIdentity() != null) {
      featureIdentity = rows[item].getPreferredFeatureIdentity().getName();
      return String.valueOf(featureIdentity + "\n" + xAxisLabel + ": "
          + numberFormat.format(dataset.getXValue(series, item)) + " " + yAxisLabel + ": "
          + numberFormat.format(dataset.getYValue(series, item)));
    } else {
      return String.valueOf(xAxisLabel + ": " + numberFormat.format(dataset.getXValue(series, item))
          + " " + yAxisLabel + ": " + numberFormat.format(dataset.getYValue(series, item)));
    }
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

}
