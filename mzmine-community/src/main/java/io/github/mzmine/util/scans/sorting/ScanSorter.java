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

package io.github.mzmine.util.scans.sorting;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.exceptions.MissingMassListException;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * Scan needs at least one MassList
 * 
 * @author Robin Schmid
 *
 */
public class ScanSorter implements Comparator<Scan> {

  private final MassListSorter comp;

  /**
   * Scans need a MassList
   *
   * @param noiseLevel
   * @param sortMode sorting mode
   */
  public ScanSorter(double noiseLevel, ScanSortMode sortMode) {
    comp = new MassListSorter(noiseLevel, sortMode);
  }

  @Override
  public int compare(Scan a, Scan b) {
    MassList ma = a.getMassList();
    MassList mb = b.getMassList();
    if (ma == null)
      throw new RuntimeException(new MissingMassListException(a));
    else if (mb == null)
      throw new RuntimeException(new MissingMassListException(b));
    else
      return comp.compare(ma, mb);
  }

}
