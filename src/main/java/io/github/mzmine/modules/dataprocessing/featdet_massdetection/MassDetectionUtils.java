/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import com.google.common.primitives.Doubles;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.openscience.cdk.Element;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

public class MassDetectionUtils {

  // Most abundance elements in biomolecules
  public static List<Element> DEFAULT_ELEMENTS_LIST = Arrays.asList(new Element("H"),
      new Element("C"), new Element("N"), new Element("O"), new Element("P"),
      new Element("S"));

  // Memoization variables for this::getIsotopesMassDiffs
  private static List<Element> memElements;
  private static double memAbundanceLowBound;
  private static int memCharge;
  private static List<Double> memRes;
  private static Isotopes isotopes;
  static {
    try {
      isotopes = Isotopes.getInstance();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns pairwise m/z differences of isotopes corresponding to given chemical elements.
   * Only isotopes with natural abundance higher than abundanceLowBound are considered.
   *
   * @param elements List of chemical elements
   * @param abundanceLowBound Natural abundance strict lower bound
   * @param charge Charge standing for 'z' in the 'm/z' (final masses are divided with this value)
   * @return List of pairwise mass differences
   */
  public static List<Double> getIsotopesMzDiffs(List<Element> elements, double abundanceLowBound,
      int charge) {

    // Test whether input parameters equal the ones in a previous function call. If yes then return
    // them and do not compute it one more time
    if (Objects.equals(elements, memElements)
        && Objects.equals(abundanceLowBound, memAbundanceLowBound)
        && Objects.equals(charge, memCharge)) {
      return memRes;
    }

    List<Double> isotopeMzDiffs = new ArrayList<>();

    // Compute pairwise mass differences within isotopes of each element
    for (Element element : elements) {

      // Filter not abundant isotopes out (abundanceLowBound == 0 by default)
      List<IIsotope> abundantIsotopes = Arrays.stream(isotopes.getIsotopes(element.getSymbol()))
          .filter(i -> Doubles.compare(i.getNaturalAbundance(), 0) > abundanceLowBound)
          .toList();

      // Compute pairwise mass differences and divide them with charge
      for (int i = 0; i < abundantIsotopes.size(); i++) {
        for (int j = i + 1; j < abundantIsotopes.size(); j++) {
          isotopeMzDiffs.add((abundantIsotopes.get(j).getExactMass()
              - abundantIsotopes.get(i).getExactMass()) / charge);
        }
      }
    }

    // Store the inputs and the computed output
    memElements = elements;
    memAbundanceLowBound = abundanceLowBound;
    memCharge = charge;
    memRes = isotopeMzDiffs;

    return isotopeMzDiffs;
  }

}
