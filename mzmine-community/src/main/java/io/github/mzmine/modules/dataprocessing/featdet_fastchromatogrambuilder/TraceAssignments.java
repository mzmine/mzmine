/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The trace of every data point of the first pass, so the second pass routes the data points
 * without detecting the traces again. A data point is stored as the slot of its trace in the
 * {@link MassTraceSweeper} and whether it started the trace: trace ids are assigned in the order in
 * which traces start, the slot of a trace is valid while it is active. Two bytes per data point,
 * four if the sweeper needs more than 32767 slots in a scan.
 */
final class TraceAssignments {

  // decision: slots of one scan fit in a char in all benchmark data sets (max ~10000 active
  // traces), int arrays only as fallback
  private static final int MAX_NARROW_CODE = Character.MAX_VALUE;

  // one char[] or int[] per scan
  private final List<Object> scans = new ArrayList<>();

  /**
   * @param code    code of each data point of the scan, see {@link #code(int, boolean)}
   * @param size    number of data points of the scan
   * @param maxCode the largest code of the scan
   */
  void addScan(@NotNull int[] code, int size, int maxCode) {
    if (maxCode <= MAX_NARROW_CODE) {
      final char[] narrow = new char[size];
      for (int i = 0; i < size; i++) {
        narrow[i] = (char) code[i];
      }
      scans.add(narrow);
    } else {
      scans.add(Arrays.copyOf(code, size));
    }
  }

  /**
   * @return the code of a data point that joined (newTrace false) or started the trace in the slot
   */
  static int code(int slot, boolean newTrace) {
    return slot << 1 | (newTrace ? 1 : 0);
  }

  static int slot(int code) {
    return code >>> 1;
  }

  static boolean isNewTrace(int code) {
    return (code & 1) != 0;
  }

  /**
   * @return the codes of the data points of a scan, a char[] or an int[]
   */
  @NotNull Object scan(int scanIndex) {
    return scans.get(scanIndex);
  }

  /**
   * Releases the codes of a scan that was routed.
   */
  void release(int scanIndex) {
    scans.set(scanIndex, null);
  }

  int getNumScans() {
    return scans.size();
  }
}
