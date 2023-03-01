/*
 * Copyright 2006-2022 The MZmine Development Team
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
 */

package io.github.mzmine.modules.visualization.massvoltammogram.utils;


import io.github.mzmine.datamodel.MassSpectrumType;
import java.util.List;

public final class MassvoltammogramScan {

  private double[] mzs;
  private double[] intensities;
  private final double potential;

  private final MassSpectrumType massSpectrumType;

  public MassvoltammogramScan(double[] mzs, double[] intensities, double potential,
      MassSpectrumType massSpectrumType) {
    this.mzs = mzs;
    this.intensities = intensities;
    this.potential = potential;
    this.massSpectrumType = massSpectrumType;
  }

  public MassvoltammogramScan(List<Double> mzs, List<Double> intensities, double potential,
      MassSpectrumType massSpectrumType) {

    setMzsFromList(mzs);
    setIntensitiesFromList(intensities);
    this.potential = potential;
    this.massSpectrumType = massSpectrumType;
  }

  public void setMzs(double[] mzs) {
    this.mzs = mzs;
  }

  public void setMzsFromList(List<Double> mzs) {
    this.mzs = mzs.stream().mapToDouble(Double::doubleValue).toArray();
  }

  public double[] getMzs() {
    return mzs;
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

  public void setIntensities(double[] intensities) {
    this.intensities = intensities;
  }

  public void setIntensitiesFromList(List<Double> intensities) {
    this.intensities = intensities.stream().mapToDouble(Double::doubleValue).toArray();
  }

  public double[] getIntensities() {
    return intensities;
  }

  public double getIntensity(int index) {
    return intensities[index];
  }

  public double getPotential() {
    return potential;
  }

  public MassSpectrumType getMassSpectrumType() {
    return massSpectrumType;
  }

  public int getNumberOfDatapoints() {
    return mzs.length;
  }

  public double[][] toArray() {

    double[][] scanAsArray = new double[getNumberOfDatapoints()][3];

    for (int i = 0; i < getNumberOfDatapoints(); i++) {

      scanAsArray[i][0] = getMz(i);
      scanAsArray[i][1] = getIntensity(i);
      scanAsArray[i][2] = potential;
    }

    return scanAsArray;
  }
}
