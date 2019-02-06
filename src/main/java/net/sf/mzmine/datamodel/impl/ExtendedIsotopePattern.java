/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package net.sf.mzmine.datamodel.impl;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassSpectrumType;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

/**
 * Simple implementation of IsotopePattern interface
 */
public class ExtendedIsotopePattern implements IsotopePattern {

  private DataPoint dataPoints[], highestIsotope;
  private int highestIsotopeIndex;
  private IsotopePatternStatus status;
  private String description;
  private Range<Double> mzRange;
  
  private String[] isotopeCompostion;
  
  public ExtendedIsotopePattern(DataPoint dataPoints[], IsotopePatternStatus status,
      String description, String[] isotopeCompostion) {

    assert dataPoints.length > 0;

    highestIsotope = dataPoints[0];
    for(int i = 0; i < dataPoints.length; i++) {
      if(highestIsotope.getIntensity() < dataPoints[i].getIntensity()) {
        highestIsotopeIndex = i;
        highestIsotope = dataPoints[0];
      }
    }
    this.dataPoints = dataPoints;
    this.status = status;
    this.description = description;
    this.mzRange = ScanUtils.findMzRange(dataPoints); 
    this.isotopeCompostion = isotopeCompostion;
    
  }

  @Override
  public @Nonnull DataPoint[] getDataPoints() {
    return dataPoints;
  }

  @Override
  public int getNumberOfDataPoints() {
    return dataPoints.length;
  }

  @Override
  public @Nonnull IsotopePatternStatus getStatus() {
    return status;
  }

  @Override
  public @Nonnull DataPoint getHighestDataPoint() {
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
  public double getTIC() {
    return 0;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return MassSpectrumType.CENTROIDED;
  }

  @Override
  @Nonnull
  public DataPoint[] getDataPointsByMass(@Nonnull Range<Double> mzRange) {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public DataPoint[] getDataPointsOverIntensity(double intensity) {
    throw new UnsupportedOperationException();
  }

  public String getIsotopeComposition(int num) {
    if(isotopeCompostion != null && num < isotopeCompostion.length)
      return isotopeCompostion[num];
    return "";
  }
  
  public String[] getIsotopeCompositions() {
    if(isotopeCompostion != null)
      return isotopeCompostion;
    return null;
  }
  
  public int getHighestDataPointIndex() {
    return highestIsotopeIndex;
  }
}
