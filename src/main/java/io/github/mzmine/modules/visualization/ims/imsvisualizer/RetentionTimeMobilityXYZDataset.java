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

package io.github.mzmine.modules.visualization.ims.imsvisualizer;

import org.jfree.data.xy.AbstractXYZDataset;
import io.github.mzmine.datamodel.Scan;

public class RetentionTimeMobilityXYZDataset extends AbstractXYZDataset {

  private final Scan[] scans;
  private final Double[] xValues;
  private final Double[] yValues;
  private final Double[] zValues;
  private final int scanSize;

  public RetentionTimeMobilityXYZDataset(DataFactory dataFactory) {

    scans = dataFactory.getScans();
    xValues = dataFactory.getRetentionTimeretentionTimeMobility();
    yValues = dataFactory.getMobilityretentionTimeMobility();
    zValues = dataFactory.getIntensityretentionTimeMobility();
    scanSize = scans.length;
  }

  @Override
  public int getSeriesCount() {
    return 1;
  }

  public Comparable<?> getRowKey(int item) {
    return scans[item].toString();
  }

  @Override
  public Comparable getSeriesKey(int series) {
    return getRowKey(series);
  }

  @Override
  public Number getZ(int series, int item) {
    return zValues[item];
  }

  @Override
  public int getItemCount(int series) {
    return scanSize;
  }

  @Override
  public Number getX(int series, int item) {
    return xValues[item];
  }

  @Override
  public Number getY(int series, int item) {
    return yValues[item];
  }
}
