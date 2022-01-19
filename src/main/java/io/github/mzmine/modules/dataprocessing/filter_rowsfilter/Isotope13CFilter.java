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
