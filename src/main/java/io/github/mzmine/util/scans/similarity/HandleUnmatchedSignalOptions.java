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

package io.github.mzmine.util.scans.similarity;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.util.scans.ScanAlignment;
import java.util.List;

/**
 * Ways to handle unmatched signals
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum HandleUnmatchedSignalOptions {
  KEEP_ALL_AND_MATCH_TO_ZERO, // keep all unmatched signals and match to 0 intensity
  REMOVE_ALL, // discard unmatched
  KEEP_LIBRARY_SIGNALS, // keep all library signals - match unmatched against 0 intensity
  KEEP_EXPERIMENTAL_SIGNALS; // keep all query signals - match unmatched against 0 intensity

  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }


  /**
   * Remove unaligned signals (not present in all masslists)
   *
   * @param aligned list of aligned signals DataPoint[library, query]
   * @return filtered list
   */
  public List<DataPoint[]> handleUnmatched(List<DataPoint[]> aligned) {
    return switch (this) {
      case KEEP_ALL_AND_MATCH_TO_ZERO -> aligned;
      case KEEP_LIBRARY_SIGNALS -> remove(aligned, 0); // library is 0 by default
      case KEEP_EXPERIMENTAL_SIGNALS -> remove(aligned, 1); // query data is 1 by default
      case REMOVE_ALL -> ScanAlignment.removeUnaligned(aligned);
    };
  }

  /**
   * Remove data points if unmatched in index
   *
   * @param aligned   list of aligned signals DataPoint[library, query]
   * @param keepIndex 0 to keep library signal or 1 to keep query signals (rest is removed if
   *                  unmatched)
   * @return filtered list
   */
  private List<DataPoint[]> remove(List<DataPoint[]> aligned, int keepIndex) {
    for (int i = 0; i < aligned.size(); ) {
      DataPoint[] alDP = aligned.get(i);
      // experimental
      if (alDP[keepIndex] == null) {
        aligned.remove(i);
      } else {
        i++;
      }
    }
    return aligned;
  }
}
