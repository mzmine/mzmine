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

  @NotNull
  @Override
  public Iterator<DataPoint> iterator() {
    return scan.iterator();
  }

}
