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

package io.github.mzmine.modules.visualization.ims.imsVisualizer;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.visualization.ims.ImsVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import org.jfree.data.xy.AbstractXYZDataset;

public class RetentionTimeMobilityXYZDataset extends AbstractXYZDataset {

  private RawDataFile dataFiles[];
  private Scan scans[];
  private Range<Double> mzRange;
  private double[] xValues;
  private double[] yValues;
  private double[] zValues;
  private int scanSize;

  public RetentionTimeMobilityXYZDataset(ParameterSet parameters) {
    dataFiles =
        parameters
            .getParameter(ImsVisualizerParameters.dataFiles)
            .getValue()
            .getMatchingRawDataFiles();

    scans =
        parameters
            .getParameter(ImsVisualizerParameters.scanSelection)
            .getValue()
            .getMatchingScans(dataFiles[0]);

    mzRange = parameters.getParameter(ImsVisualizerParameters.mzRange).getValue();

    scanSize = scans.length;

    // Calc xValues retention time
    xValues = new double[scanSize];
    for (int i = 0; i < scanSize; i++) {
      xValues[i] = scans[i].getRetentionTime();
    }
    // Calculte yvalues mobility value.
    yValues = new double[scanSize];
    for (int i = 0; i < scanSize; i++) {
      yValues[i] = scans[i].getMobility();
    }

    // Calculate zValues intensity.
    zValues = new double[scanSize];
    for (int i = 0; i < scanSize; i++) {
      DataPoint dataPoint[] = scans[i].getDataPointsByMass(mzRange);

      for (int j = 0; j < dataPoint.length; j++) {
        zValues[i] += dataPoint[j].getIntensity();
      }
    }
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
