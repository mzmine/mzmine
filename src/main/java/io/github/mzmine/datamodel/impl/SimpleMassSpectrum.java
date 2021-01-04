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
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;

public class SimpleMassSpectrum implements MassSpectrum {

  @Override
  public Iterator<DataPoint> iterator() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Range<Double> getDataPointMZRange() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public @Nullable Integer getBasePeakIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public @Nonnull Double getTIC() {
    // TODO Auto-generated method stub
    return 0.0;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getNumberOfDataPoints() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public DoubleBuffer getMzValues() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getMzValue(int index) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public double getIntensityValue(int index) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Double getBasePeakMz() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Double getBasePeakIntensity() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Stream<DataPoint> stream() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DataPoint[] getDataPointsByMass(Range<Double> mzRange) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public DataPoint[] getDataPointsOverIntensity(double intensity) {
    // TODO Auto-generated method stub
    return null;
  }

}
