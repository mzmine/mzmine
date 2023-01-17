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
