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

package io.github.mzmine.datamodel.impl;

import java.nio.DoubleBuffer;
import java.util.Iterator;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * Simple implementation of IsotopePattern interface
 */
public class SimpleIsotopePattern implements IsotopePattern {

  private double mzValues[], intensityValues[];
  private int highestIsotope;
  private IsotopePatternStatus status;
  private String description;
  private Range<Double> mzRange;
  private String[] isotopeCompostion;

  public SimpleIsotopePattern(double mzValues[], double intensityValues[],
      IsotopePatternStatus status, String description, String[] isotopeCompostion) {

    this(mzValues, intensityValues, status, description);
    this.isotopeCompostion = isotopeCompostion;
  }

  public SimpleIsotopePattern(DataPoint dataPoints[], IsotopePatternStatus status,
      String description, String[] isotopeCompostion) {

    this(dataPoints, status, description);
    this.isotopeCompostion = isotopeCompostion;
  }


  public SimpleIsotopePattern(DataPoint dataPoints[], IsotopePatternStatus status,
      String description) {

    assert mzValues.length > 0;
    assert mzValues.length == intensityValues.length;

    highestIsotope = ScanUtils.findTopDataPoint(intensityValues);
    mzValues = new double[dataPoints.length];
    intensityValues = new double[dataPoints.length];
    for (int i = 0; i < dataPoints.length; i++) {
      mzValues[i] = dataPoints[i].getMZ();
      intensityValues[i] = dataPoints[i].getIntensity();
    }
    this.status = status;
    this.description = description;
    this.mzRange = ScanUtils.findMzRange(mzValues);
  }


  public SimpleIsotopePattern(double mzValues[], double intensityValues[],
      IsotopePatternStatus status, String description) {

    assert mzValues.length > 0;
    assert mzValues.length == intensityValues.length;

    highestIsotope = ScanUtils.findTopDataPoint(intensityValues);
    this.mzValues = mzValues;
    this.intensityValues = intensityValues;
    this.status = status;
    this.description = description;
    this.mzRange = ScanUtils.findMzRange(mzValues);
  }

  @Override
  public int getNumberOfDataPoints() {
    return mzValues.length;
  }

  @Override
  public @Nonnull IsotopePatternStatus getStatus() {
    return status;
  }

  @Override
  public @Nullable Integer getBasePeakIndex() {
    return highestIsotope;
  }

  @Override
  public @Nonnull String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return "Isotope pattern: " + description;
  }

  @Override
  @Nonnull
  public Range<Double> getDataPointMZRange() {
    return mzRange;
  }

  @Override
  public @Nonnull Double getTIC() {
    return 0.0;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return MassSpectrumType.CENTROIDED;
  }

  @Override
  public DoubleBuffer getMzValues() {
    return DoubleBuffer.wrap(mzValues);
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return DoubleBuffer.wrap(intensityValues);
  }


  public String getIsotopeComposition(int num) {
    if (isotopeCompostion != null && num < isotopeCompostion.length)
      return isotopeCompostion[num];
    return "";
  }

  public String[] getIsotopeCompositions() {
    if (isotopeCompostion != null)
      return isotopeCompostion;
    return null;
  }

  private DataPoint[] getDataPoints() {
    DataPoint d[] = new DataPoint[getNumberOfDataPoints()];
    for (int i = 0; i < getNumberOfDataPoints(); i++) {
      d[i] = new SimpleDataPoint(getMzValues().get(i), getIntensityValues().get(i));
    }
    return d;
  }

  /*
   * @Override public DataPoint getHighestDataPoint() { if (highestIsotope < 0) return null; return
   * getDataPoints()[highestIsotope]; }
   */

  @Override
  public DataPoint[] getDataPointsByMass(Range<Double> mzRange) {

    DataPoint[] dataPoints = getDataPoints();
    int startIndex, endIndex;
    for (startIndex = 0; startIndex < dataPoints.length; startIndex++) {
      if (dataPoints[startIndex].getMZ() >= mzRange.lowerEndpoint()) {
        break;
      }
    }

    for (endIndex = startIndex; endIndex < dataPoints.length; endIndex++) {
      if (dataPoints[endIndex].getMZ() > mzRange.upperEndpoint()) {
        break;
      }
    }

    DataPoint pointsWithinRange[] = new DataPoint[endIndex - startIndex];

    // Copy the relevant points
    System.arraycopy(dataPoints, startIndex, pointsWithinRange, 0, endIndex - startIndex);

    return pointsWithinRange;
  }

  @Override
  public double getMzValue(int index) {
    return mzValues[index];
  }

  @Override
  public double getIntensityValue(int index) {
    return intensityValues[index];
  }

  @Override
  @Nullable
  public Double getBasePeakMz() {
    if (highestIsotope < 0)
      return null;
    else
      return mzValues[highestIsotope];
  }

  @Override
  @Nullable
  public Double getBasePeakIntensity() {
    if (highestIsotope < 0)
      return null;
    else
      return intensityValues[highestIsotope];
  }

  @Override
  public Iterator<DataPoint> iterator() {
    return new DataPointIterator(this);
  }

  @Override
  public Stream<DataPoint> stream() {
    return Streams.stream(this);
  }

  private class DataPointIterator implements Iterator<DataPoint>, DataPoint {

    // We start at -1 so the first call to next() moves us to index 0
    private int cursor = -1;
    private final MassSpectrum spectrum;

    DataPointIterator(MassSpectrum spectrum) {
      this.spectrum = spectrum;
    }

    @Override
    public boolean hasNext() {
      return (cursor + 1) < spectrum.getNumberOfDataPoints();
    }

    @Override
    public DataPoint next() {
      cursor++;
      return this;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public double getMZ() {
      return spectrum.getMzValue(cursor);
    }

    @Override
    public double getIntensity() {
      return spectrum.getIntensityValue(cursor);
    }
  }

}
