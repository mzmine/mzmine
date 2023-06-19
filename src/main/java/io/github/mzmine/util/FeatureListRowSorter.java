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
  public static final FeatureListRowSorter MZ_ASCENDING = new FeatureListRowSorter(
      SortingProperty.MZ, SortingDirection.Ascending);


  private final SortingProperty property;
  private final SortingDirection direction;

  public FeatureListRowSorter(SortingProperty property, SortingDirection direction) {
    this.property = property;
    this.direction = direction;
  }

  public int compare(FeatureListRow row1, FeatureListRow row2) {

    Double row1Value = getValue(row1);
    Double row2Value = getValue(row2);

    if (direction == SortingDirection.Ascending) {
      return row1Value.compareTo(row2Value);
    } else {
      return row2Value.compareTo(row1Value);
    }

  }

  private double getValue(FeatureListRow row) {
    switch (property) {
      case Area:
        Feature[] areaPeaks = row.getFeatures().toArray(new Feature[0]);
        double[] peakAreas = new double[areaPeaks.length];
        for (int i = 0; i < peakAreas.length; i++) {
          peakAreas[i] = areaPeaks[i].getArea();
        }
        double medianArea = MathUtils.calcQuantile(peakAreas, 0.5);
        return medianArea;
      case Intensity:
        Feature[] intensityPeaks = row.getFeatures().toArray(new Feature[0]);
        double[] peakIntensities = new double[intensityPeaks.length];
        for (int i = 0; i < intensityPeaks.length; i++) {
          peakIntensities[i] = intensityPeaks[i].getArea();
        }
        double medianIntensity = MathUtils.calcQuantile(peakIntensities, 0.5);
        return medianIntensity;
      case Height:
        Feature[] heightPeaks = row.getFeatures().toArray(new Feature[0]);
        double[] peakHeights = new double[heightPeaks.length];
        for (int i = 0; i < peakHeights.length; i++) {
          peakHeights[i] = heightPeaks[i].getHeight();
        }
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
