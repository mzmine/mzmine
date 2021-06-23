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

package io.github.mzmine.util;

import com.google.common.primitives.Doubles;
import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.DetectIsotopesParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

public class IsotopesUtils {

  // Memoization variables for this::getIsotopesMassDiffs
  private static List<Element> memElements;
  private static int memMaxCharge;
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
   * Returns pairwise m/z differences between stable isotopes within given chemical elements. Final
   * differences are obtained by dividing isotope mass differences with 1, 2, ..., maxCharge values.
   *
   * @param elements List of chemical elements
   * @param maxCharge Maximum possible charge
   * @return List of pairwise mass differences
   */
  public static List<Double> getIsotopesMzDiffs(List<Element> elements, int maxCharge) {

    // Test whether input parameters equal the ones in a previous function call. If yes then return
    // them and do not compute it one more time
    if (Objects.equals(elements, memElements)
        && Objects.equals(maxCharge, memMaxCharge)) {
      return memRes;
    }

    List<Double> isotopeMzDiffs = new ArrayList<>();

    // Compute pairwise mass differences within isotopes of each element
    for (Element element : elements) {

      // Filter out not stable isotopes
      List<IIsotope> abundantIsotopes = Arrays.stream(isotopes.getIsotopes(element.getSymbol()))
          .filter(i -> Doubles.compare(i.getNaturalAbundance(), 0) > 0d)
          .toList();

      // Compute pairwise mass differences and divide them with maxCharge
      for (int i = 0; i < abundantIsotopes.size(); i++) {
        for (int j = i + 1; j < abundantIsotopes.size(); j++) {
          for (int charge = 1; charge <= maxCharge; charge++) {
            isotopeMzDiffs.add((abundantIsotopes.get(j).getExactMass()
                - abundantIsotopes.get(i).getExactMass()) / charge);
          }
        }
      }
    }

    // Store the inputs and the computed output
    memElements = elements;
    memMaxCharge = maxCharge;
    memRes = isotopeMzDiffs;

    return isotopeMzDiffs;
  }

  /**
   * Returns true if given m/z value newMz can be considered as an m/z value of some isotope corresponding
   * to given m/z values in knownMzs. Chemical elements which isotopes are considered and maximum
   * possible charge are passed in isotopesParameters argument (see {@link #getIsotopesMzDiffs(List, int)}).
   *
   * @param newMz M/z value of interest
   * @param knownMzs List of known m/z values
   * @param isotopesParameters DetectIsotopesParameter parameters
   * @return True if new m/z corresponds to an isotope of known m/z's, false otherwise.
   */
  public static boolean isIsotopeMz(double newMz, TDoubleArrayList knownMzs,
      @NotNull DetectIsotopesParameter isotopesParameters) {

    // List of possible isotope m/z differences
    List<Double> isotopeMzDiffs = getIsotopesMzDiffs(
        isotopesParameters.getParameter(DetectIsotopesParameter.elements).getValue(),
        isotopesParameters.getParameter(DetectIsotopesParameter.maxCharge).getValue());

    // Isotope m/z tolerance
    MZTolerance mzTolerance = isotopesParameters.getParameter(DetectIsotopesParameter.isotopeMzTolerance)
        .getValue();

    // Iterate over possible isotope m/z differences
    for (double isotopeMzDiff : isotopeMzDiffs) {

      // Go left over m/z's of previously detected peaks and check whether current peak is an
      // isotope of one of them
      // TODO: contains in a new set of added mzs instead of for loop over list (if slow),
      //  O(n^2 / 2) -> O(n) for O(n) memory
      for (int mzIndex = knownMzs.size() - 1; mzIndex >= 0; mzIndex--) {

        // Compute m/z difference between current peak and previously detected one
        double mzDiff = newMz - knownMzs.get(mzIndex);

        // Do not go left further if m/z difference is higher than isotope difference
        if (Doubles.compare(mzDiff, isotopeMzDiff) > 0) {
          break;
        }

        // If m/z difference equals isotope difference up to tolerance, then m/z of the peak
        // corresponds to the mass of the isotope
        if (mzTolerance.getToleranceRange(mzDiff).contains(isotopeMzDiff)) {
          return true;
        }
      }
    }

    return false;
  }

}
