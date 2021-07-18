/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.datasets;

import io.github.mzmine.datamodel.MassList;
import org.jfree.data.xy.AbstractXYDataset;

/**
 * Data set for MassList
 */
public class MassListDataSet extends AbstractXYDataset {

  private final double mzValues[], intensityValues[];
  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public MassListDataSet(MassList massList) {
    this.mzValues = massList.getMzValues(null);
    this.intensityValues = massList.getIntensityValues(null);
  }

  public MassListDataSet(double mzValues[], double intensityValues[]) {
    this.mzValues = mzValues;
    this.intensityValues = intensityValues;
  }


  @Override
  public int getSeriesCount() {
    return 1;
  }

  @Override
  public Comparable getSeriesKey(int series) {
    return 1;
  }

  @Override
  public int getItemCount(int series) {
    return mzValues.length;
  }

  @Override
  public Number getX(int series, int item) {
    return mzValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return intensityValues[item];
  }
}
