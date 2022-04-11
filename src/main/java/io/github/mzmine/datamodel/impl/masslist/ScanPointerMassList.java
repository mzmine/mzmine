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

package io.github.mzmine.datamodel.impl.masslist;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_mzml.MSDKmzMLImportTask;
import java.util.Iterator;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class points back to the scan to access data. (Useful if the scan is already centroided /
 * thresholded) See its use in {@link AllSpectralDataImportModule} or in {@link MSDKmzMLImportTask}
 */
public class ScanPointerMassList implements MassList {

  // points to this scan data
  private final Scan scan;

  public ScanPointerMassList(Scan scan) {
    super();
    this.scan = scan;
  }

  /**
   * Use mzValues and intensityValues constructor
   */
  @Override
  public DataPoint[] getDataPoints() {
    final double[][] mzIntensity = new double[2][];
    final int numDp = getNumberOfDataPoints();

    mzIntensity[0] = new double[numDp];
    mzIntensity[1] = new double[numDp];
    getMzValues(mzIntensity[0]);
    getIntensityValues(mzIntensity[1]);

    DataPoint[] dps = new DataPoint[numDp];
    for (int i = 0; i < numDp; i++) {
      dps[i] = new SimpleDataPoint(mzIntensity[0][i], mzIntensity[1][i]);
    }

    return dps;
  }

  @Override
  public int getNumberOfDataPoints() {
    return scan.getNumberOfDataPoints();
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return scan.getSpectrumType();
  }

  @Override
  public double[] getMzValues(@NotNull double[] dst) {
    return scan.getMzValues(dst);
  }

  @Override
  public double[] getIntensityValues(@NotNull double[] dst) {
    return scan.getIntensityValues(dst);
  }

  @Override
  public double getMzValue(int index) {
    return scan.getMzValue(index);
  }

  @Override
  public double getIntensityValue(int index) {
    return scan.getIntensityValue(index);
  }

  @Nullable
  @Override
  public Double getBasePeakMz() {
    return scan.getBasePeakMz();
  }

  @Nullable
  @Override
  public Double getBasePeakIntensity() {
    return scan.getBasePeakIntensity();
  }

  @Nullable
  @Override
  public Integer getBasePeakIndex() {
    return scan.getBasePeakIndex();
  }

  @Nullable
  @Override
  public Range<Double> getDataPointMZRange() {
    return scan.getDataPointMZRange();
  }

  @Nullable
  @Override
  public Double getTIC() {
    return scan.getTIC();
  }

  @Override
  public Stream<DataPoint> stream() {
    return scan.stream();
  }

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    return scan.iterator();
  }

}
