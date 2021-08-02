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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;
import gnu.trove.list.array.TDoubleArrayList;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

public class IsotopesUtils {

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
   * For example, for elements == [H, C] and maxCharge == 2 this method returns [C14 - C13,
   * (C14 - C13) / 2, H2 - H1, (H2 - H1) / 2], where C14, C13, H2, H1 are corresponding isotopic masses.
   *
   * @param elements List of chemical elements
   * @param maxCharge Maximum possible charge
   * @return List of pairwise mass differences
   */
  public static List<Double> getIsotopesMzDiffs(List<Element> elements, int maxCharge) {

    List<Double> isotopeMzDiffs = new ArrayList<>();

    // Compute pairwise mass differences within isotopes of each element
    for (Element element : elements) {

      // Filter out not stable isotopes
      List<IIsotope> abundantIsotopes = Arrays.stream(isotopes.getIsotopes(element.getSymbol()))
          .filter(i -> Doubles.compare(i.getNaturalAbundance(), 0) > 0d)
          .toList();

      // Compute pairwise mass differences and divide each one with charges up to maxCharge
      for (int i = 0; i < abundantIsotopes.size(); i++) {
        for (int j = i + 1; j < abundantIsotopes.size(); j++) {
          for (int charge = 1; charge <= maxCharge; charge++) {
            isotopeMzDiffs.add((abundantIsotopes.get(j).getExactMass()
                - abundantIsotopes.get(i).getExactMass()) / charge);
          }
        }
      }
    }

    return isotopeMzDiffs;
  }

  /**
   * Returns true if given m/z value newMz can be considered as an m/z value of some isotope corresponding
   * to given m/z values in knownMzs. Only isotope m/z differences given in the isotopesMzDiffs are
   * considered.
   *
   * @param newMz M/z value of interest
   * @param knownMzs List of known m/z values
   * @param isotopesMzDiffs Pairwise m/z differences between isotopes (supposed to be obtained with
   *                        {@link #getIsotopesMzDiffs(List, int)})
   * @param mzTolerance Maximum allowed m/z difference to consider any m/z value and isotope m/z
   *                    value equal
   * @return True if new m/z corresponds to an isotope of known m/z's, false otherwise.
   */
  public static boolean isPossibleIsotopeMz(double newMz, @NotNull TDoubleArrayList knownMzs,
      @NotNull List<Double> isotopesMzDiffs,
      @NotNull MZTolerance mzTolerance) {

    // Iterate over possible isotope m/z differences
    for (double isotopeMzDiff : isotopesMzDiffs) {

      // Compute theoretical m/z value representing the difference between possible isotope
      // candidate newMz and possible isotope difference
      double theoreticalMz = newMz - isotopeMzDiff;
      Range<Double> theoreticalMzTolRange = mzTolerance.getToleranceRange(theoreticalMz);

      // Go left over m/z's of previously detected peaks and check whether current peak is an
      // isotope of one of them
      // TODO: If in future the method will be slow for some data files it is possible to improve
      //  the speed by implementing a HashSet for doubles with given precision and use it to store
      //  all knownMzs values and check for their presence instead of the following for loop.
      //  O(n^2 / 2) -> O(n) for O(n) memory
      for (int mzIndex = knownMzs.size() - 1; mzIndex >= 0; mzIndex--) {

        // Get real m/z from knownMzs that is going to be compared with the theoretical one
        double realMz = knownMzs.get(mzIndex);

        // Do not go left further if the theoretical m/z is higher than real
        if (Doubles.compare(theoreticalMzTolRange.lowerEndpoint(), realMz) > 0) {
          break;
        }

        // If the theoretical and real m/z values are equal up to tolerance, then m/z of the mzIndex
        // peak corresponds to the mass of the isotope
        if (theoreticalMzTolRange.contains(realMz)) {
          return true;
        }
      }
    }

    return false;
  }

}
