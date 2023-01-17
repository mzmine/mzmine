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
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

import io.github.mzmine.main.MZmineCore;

/**
 * Item label generator for XYPlots adds the name of a feature in form of a label
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class NameItemLabelGenerator implements XYItemLabelGenerator {

  private FeatureListRow rows[];

  public NameItemLabelGenerator(FeatureListRow rows[]) {
    this.rows = rows;
  }

  @Override
  public String generateLabel(XYDataset dataset, int series, int item) {

    // Create label
    String label = null;
    if (rows[item].getPreferredFeatureIdentity() != null) {
      label = rows[item].getPreferredFeatureIdentity().getName();
    } else {
      // get charge
      int charge = 1;
      if (rows[item].getRowCharge() == 0) {
        charge = 1;
      } else {
        charge = rows[item].getRowCharge();
      }
      label = "m/z: "
          + String.valueOf(
              MZmineCore.getConfiguration().getMZFormat().format(rows[item].getAverageMZ()))
          + " charge: " + charge;
    }
    return label;
  }

}
