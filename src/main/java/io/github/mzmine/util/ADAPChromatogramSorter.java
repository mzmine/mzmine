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

import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogram;
import java.util.Comparator;


/**
 * This is a helper class required for sorting adap chromatograms
 */
public class ADAPChromatogramSorter implements Comparator<ADAPChromatogram> {

  private SortingProperty property;
  private SortingDirection direction;

  public ADAPChromatogramSorter(SortingProperty property, SortingDirection direction) {
    this.property = property;
    this.direction = direction;
  }

  public int compare(ADAPChromatogram peak1, ADAPChromatogram peak2) {

    Double peak1Value = getValue(peak1);
    Double peak2Value = getValue(peak2);

    if (direction == SortingDirection.Ascending) {
      return peak1Value.compareTo(peak2Value);
    } else {
      return peak2Value.compareTo(peak1Value);
    }
  }

  private double getValue(ADAPChromatogram peak) {
    switch (property) {
      case Height:
        return peak.getHeight();
      case MZ:
        return peak.getMZ() + peak.getRT() / 1000000.0;
      case RT:
        return peak.getRT() + peak.getMZ() / 1000000.0;
      default:
        // We should never get here, so throw exception
        throw (new IllegalStateException());
    }

  }

}
