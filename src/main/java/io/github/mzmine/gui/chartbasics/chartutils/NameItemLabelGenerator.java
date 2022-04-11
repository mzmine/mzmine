/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
