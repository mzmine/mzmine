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

package io.github.mzmine.util;

import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.Comparator;


/**
 * Compare feature list rows either by ID, average m/z or median area of peaks
 */
public class FeatureListRowSorter implements Comparator<FeatureListRow> {

  public static final FeatureListRowSorter DEFAULT_RT = new FeatureListRowSorter(SortingProperty.RT,
      SortingDirection.Ascending);
  public static final FeatureListRowSorter DEFAULT_ID = new FeatureListRowSorter(SortingProperty.ID,
      SortingDirection.Ascending);

  private SortingProperty property;
  private SortingDirection direction;

  public FeatureListRowSorter(SortingProperty property, SortingDirection direction) {
    this.property = property;
    this.direction = direction;
  }

  public int compare(FeatureListRow row1, FeatureListRow row2) {

    Double row1Value = getValue(row1);
    Double row2Value = getValue(row2);

    if (direction == SortingDirection.Ascending)
      return row1Value.compareTo(row2Value);
    else
      return row2Value.compareTo(row1Value);

  }

  private double getValue(FeatureListRow row) {
    switch (property) {
      case Area:
        Feature[] areaPeaks = row.getFeatures().toArray(new Feature[0]);
        double[] peakAreas = new double[areaPeaks.length];
        for (int i = 0; i < peakAreas.length; i++)
          peakAreas[i] = areaPeaks[i].getArea();
        double medianArea = MathUtils.calcQuantile(peakAreas, 0.5);
        return medianArea;
      case Intensity:
        Feature[] intensityPeaks = row.getFeatures().toArray(new Feature[0]);
        double[] peakIntensities = new double[intensityPeaks.length];
        for (int i = 0; i < intensityPeaks.length; i++)
          peakIntensities[i] = intensityPeaks[i].getArea();
        double medianIntensity = MathUtils.calcQuantile(peakIntensities, 0.5);
        return medianIntensity;
      case Height:
        Feature[] heightPeaks = row.getFeatures().toArray(new Feature[0]);
        double[] peakHeights = new double[heightPeaks.length];
        for (int i = 0; i < peakHeights.length; i++)
          peakHeights[i] = heightPeaks[i].getHeight();
        double medianHeight = MathUtils.calcQuantile(peakHeights, 0.5);
        return medianHeight;
      case MZ:
        return row.getAverageMZ() + row.getAverageRT() / 10000000.0;
      case RT:
        return row.getAverageRT() + row.getAverageMZ() / 10000000.0;
      case ID:
        return row.getID();
    }

    // We should never get here, so throw exception
    throw (new IllegalStateException());
  }

}
