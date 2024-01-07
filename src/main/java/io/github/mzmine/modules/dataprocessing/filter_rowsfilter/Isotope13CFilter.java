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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.Isotope;
import io.github.mzmine.util.IsotopePatternUtils;
import io.github.mzmine.util.IsotopesUtils;
import java.util.ArrayList;
import java.util.List;
import org.openscience.cdk.Element;

/**
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class Isotope13CFilter {

  private final MZTolerance mzTolerance;
  private final int maxCharge;
  private final boolean filter13Isotope;
  private final boolean applyMinCEstimation;
  private final Isotope[] excludedIsotopes;

  public Isotope13CFilter(MZTolerance mzTolerance, int maxCharge, boolean filter13Isotope,
      List<Element> elements, boolean applyMinCEstimation) {
    this.mzTolerance = mzTolerance;
    this.maxCharge = maxCharge;
    this.filter13Isotope = filter13Isotope;
    this.applyMinCEstimation = applyMinCEstimation;

    if (elements != null && !elements.isEmpty()) {
      List<Isotope> isotopes = new ArrayList<>();
      for (Element element : elements) {
        isotopes.addAll(IsotopesUtils.getIsotopeRecord(element.getSymbol()));
      }
      excludedIsotopes = isotopes.toArray(new Isotope[0]);
    } else {
      excludedIsotopes = null;
    }
  }

  /**
   * Check if main m/z has a 13C isotope signal. If set filter13CIsotope is set, the signal must not
   * be preceded by another 13C isotope signal.
   *
   * @param pattern search in this pattern
   * @param mainMZ  check this mz for 13C isotope signals
   * @return true if mainMZ has a 13C isotope signal and is itself not a 13C or other isotope signal
   * of preceding signals.
   */
  public boolean accept(IsotopePattern pattern, double mainMZ) {
    return IsotopePatternUtils.check13CPattern(pattern, mainMZ, mzTolerance, maxCharge,
        filter13Isotope, excludedIsotopes, applyMinCEstimation);
  }
}
