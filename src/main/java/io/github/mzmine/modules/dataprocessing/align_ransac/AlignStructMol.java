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
package io.github.mzmine.modules.dataprocessing.align_ransac;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.Comparator;

import io.github.mzmine.datamodel.RawDataFile;

public class AlignStructMol implements Comparator<AlignStructMol> {

  public FeatureListRow row1, row2;
  public double RT, RT2;
  public boolean Aligned = false;
  public boolean ransacMaybeInLiers;
  public boolean ransacAlsoInLiers;

  public AlignStructMol(FeatureListRow row1, FeatureListRow row2) {
    this.row1 = row1;
    this.row2 = row2;
    RT = row1.getAverageRT();
    RT2 = row2.getAverageRT();
  }

  public AlignStructMol(FeatureListRow row1, FeatureListRow row2, RawDataFile file, RawDataFile file2) {
    this.row1 = row1;
    this.row2 = row2;
    if (row1.getFeature(file) != null) {
      RT = row1.getFeature(file).getRT();
    } else {
      RT = row1.getAverageRT();
    }

    if (row2.getFeature(file2) != null) {
      RT2 = row2.getFeature(file2).getRT();
    } else {
      RT = row1.getAverageRT();
    }
  }

  AlignStructMol() {

  }

  public int compare(AlignStructMol arg0, AlignStructMol arg1) {
    if (arg0.RT < arg1.RT) {
      return -1;
    } else {
      return 1;
    }
  }
}
