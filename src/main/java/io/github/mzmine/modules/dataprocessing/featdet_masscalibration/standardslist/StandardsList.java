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

package io.github.mzmine.modules.dataprocessing.featdet_masscalibration.standardslist;

import com.google.common.collect.Range;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Class for maintaining a list of standard molecules
 * expected to appear in the dataset
 */
public class StandardsList {
  protected ArrayList<StandardsListItem> standardMolecules;

  public StandardsList(List<StandardsListItem> standardMolecules) {
    this.standardMolecules = new ArrayList<StandardsListItem>(standardMolecules);
    this.standardMolecules.sort(StandardsListItem.mzComparator);
  }

  protected StandardsList(List<StandardsListItem> standardMolecules, boolean sorted) {
    this.standardMolecules = new ArrayList<StandardsListItem>(standardMolecules);
    if (sorted == false) {
      this.standardMolecules.sort(StandardsListItem.mzComparator);
    }
  }

  /**
   * Return a list of standard calibrants that are within given ranges of mz ratios and retention times
   * when a null is given, the range is skipped / assumed to contain all mz or rt values
   *
   * @param mzRange
   * @param rtRange
   * @return
   */
  public StandardsList getInRanges(Range<Double> mzRange, Range<Float> rtRange) {
    if (mzRange != null && rtRange == null) {
      return getInMzRange(mzRange);
    }
    ArrayList<StandardsListItem> withinRange = new ArrayList<>();
    for (StandardsListItem molecule : standardMolecules) {
      if ((mzRange == null || mzRange.contains(molecule.getMzRatio()))
              && (rtRange == null || rtRange.contains(molecule.getRetentionTime()))) {
        withinRange.add(molecule);
      }
    }
    return new StandardsList(withinRange, true);
  }

  public StandardsList getInMzRange(Range<Double> mzRange) {
    double lower = mzRange.lowerEndpoint();
    double upper = mzRange.upperEndpoint();
    int lowerPosition =
            Collections.binarySearch(standardMolecules, new StandardsListItem(lower), StandardsListItem.mzComparator);
    int upperPosition =
            Collections.binarySearch(standardMolecules, new StandardsListItem(upper), StandardsListItem.mzComparator);
    if (lowerPosition < 0) {
      lowerPosition = -1 * (lowerPosition + 1);
    }
    if (upperPosition < 0) {
      upperPosition = -1 * (upperPosition + 1);
    }

    ArrayList<StandardsListItem> withinRange = new ArrayList<>();
    for (int i = lowerPosition; i < upperPosition; i++) {
      withinRange.add(standardMolecules.get(i));
    }
    return new StandardsList(withinRange, true);
  }

  public ArrayList<StandardsListItem> getStandardMolecules() {
    return standardMolecules;
  }
}
