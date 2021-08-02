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

package io.github.mzmine.util.scans.sorting;

import io.github.mzmine.datamodel.MassSpectrum;
import java.util.Comparator;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.util.scans.ScanUtils;

public class MassListSorter implements Comparator<MassSpectrum> {
  private double noiseLevel;
  private ScanSortMode sort;

  public MassListSorter(double noiseLevel, ScanSortMode sort) {
    this.sort = sort;
    this.noiseLevel = noiseLevel;
  }

  @Override
  public int compare(MassSpectrum a, MassSpectrum b) {
    switch (sort) {
      case NUMBER_OF_SIGNALS:
        int result = Integer.compare(getNumberOfSignals(a), getNumberOfSignals(b));
        // same number of signals? use max TIC
        if (result == 0)
          return Double.compare(getTIC(a), getTIC(b));
        else
          return result;
      case MAX_TIC:
        return Double.compare(getTIC(a), getTIC(b));
    }
    throw new IllegalArgumentException("Should not reach. Not all cases of sort are considered");
  }

  /**
   * sum of intensity
   * 
   * @param a
   * @return
   */
  private double getTIC(MassSpectrum a) {
    return ScanUtils.getTIC(a, noiseLevel);
  }

  /**
   * Number of DP greater noise level
   * 
   * @param a
   * @return
   */
  private int getNumberOfSignals(MassSpectrum a) {
    return ScanUtils.getNumberOfSignals(a, noiseLevel);
  }

}
