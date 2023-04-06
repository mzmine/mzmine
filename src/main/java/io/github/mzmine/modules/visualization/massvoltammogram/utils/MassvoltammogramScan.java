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

package io.github.mzmine.modules.visualization.massvoltammogram.utils;


import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import java.util.Arrays;
import java.util.List;

public final class MassvoltammogramScan {

  private final double potential;
  private final MassSpectrumType massSpectrumType;
  private double[] mzs;
  private double[] intensities;

  public MassvoltammogramScan(double[] mzs, double[] intensities, double potential,
      MassSpectrumType massSpectrumType) {
    this.mzs = mzs;
    this.intensities = intensities;
    this.potential = potential;
    this.massSpectrumType = massSpectrumType;
  }

  public MassvoltammogramScan(List<Double> mzs, List<Double> intensities, double potential,
      MassSpectrumType massSpectrumType) {

    setMzs(mzs);
    setIntensities(intensities);
    this.potential = potential;
    this.massSpectrumType = massSpectrumType;
  }

  public double[] getMzs() {
    return mzs;
  }

  public void setMzs(double[] mzs) {
    this.mzs = mzs;
  }

  public void setMzs(List<Double> mzs) {
    this.mzs = mzs.stream().mapToDouble(Double::doubleValue).toArray();
  }

  public double getMz(int index) {
    return mzs[index];
  }

  public double getMinMz() {
    return mzs[0];
  }

  public double getMaxMz() {
    return mzs[getNumberOfDatapoints() - 1];
  }

  /**
   * @return Returns the scans mz-values range. Returns null if the scan is empty.
   */
  public Range<Double> getMzRange() {
    if (isEmpty()) {
      return null;
    } else {
      return Range.closed(getMinMz(), getMaxMz());
    }
  }

  public double[] getIntensities() {
    return intensities;
  }

  public void setIntensities(double[] intensities) {
    this.intensities = intensities;
  }

  public void setIntensities(List<Double> intensities) {
    this.intensities = intensities.stream().mapToDouble(Double::doubleValue).toArray();
  }

  public double getIntensity(int index) {
    return intensities[index];
  }

  public double getPotential() {
    return potential;
  }

  public double[] getPotentialAsArray() {

    double[] potentialAsArray = new double[getNumberOfDatapoints()];
    Arrays.fill(potentialAsArray, potential);
    return potentialAsArray;
  }

  public MassSpectrumType getMassSpectrumType() {
    return massSpectrumType;
  }

  public int getNumberOfDatapoints() {
    return mzs.length;
  }

  public boolean isEmpty() {
    return mzs.length == 0;
  }
}
