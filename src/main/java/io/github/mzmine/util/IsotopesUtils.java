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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.Element;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.interfaces.IIsotope;

public class IsotopesUtils {

  private static final DataPointSorter mzSorter = new DataPointSorter(SortingProperty.MZ,
      SortingDirection.Ascending);
  private static Isotopes isotopes;

  static {
    try {
      isotopes = Isotopes.getInstance();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets an array of all NATURALLY OCCURRING isotopes known to the IsotopeFactory for the given
   * element symbol.
   *
   * @param elementSymbol An element symbol to search for
   * @return An array of isotopes that matches the given element symbol or an empty array
   */
  public static IIsotope[] getIsotopes(String elementSymbol) {
    return getIsotopes(elementSymbol, true);
  }

  /**
   * Gets an array of all isotopes known to the IsotopeFactory for the given element symbol.
   *
   * @param elementSymbol An element symbol to search for
   * @param onlyNatural   only elements with natural abundance > 0
   * @return An array of isotopes that matches the given element symbol or an empty array
   */
  public static IIsotope[] getIsotopes(String elementSymbol, boolean onlyNatural) {
    if (onlyNatural) {
      return isotopes.getIsotopes(elementSymbol);
    } else {
      return Arrays.stream(isotopes.getIsotopes(elementSymbol))
          .filter(iso -> iso.getNaturalAbundance() > 0).toArray(IIsotope[]::new);
    }
  }

  /**
   * The isotope of an element with mass number might include isotopes with no natural abundance.
   *
   * @param elementSymbol An element symbol to search for
   * @param massNumber    the isotope with this mass number
   * @return the isotope of an element with specific mass number or null if not available (
   */
  public static IIsotope getIsotopes(String elementSymbol, int massNumber) {
    for (IIsotope iso : getIsotopes(elementSymbol, false)) {
      if (iso.getMassNumber() == massNumber) {
        return iso;
      }
    }
    return null;
  }

  /**
   * The isotope of an element with mass number might include isotopes with no natural abundance.
   *
   * @param elementSymbol An element symbol to search for
   * @param massNumber    the isotope with this mass number
   * @return the isotope of an element with specific mass number or null if not available (
   */
  public static Isotope getIsotopeRecord(String elementSymbol, int massNumber) {
    IIsotope result = null;
    IIsotope main = null;
    for (IIsotope iso : getIsotopes(elementSymbol, false)) {
      if (main == null || main.getNaturalAbundance() < iso.getNaturalAbundance()) {
        main = iso;
      }
      if (iso.getMassNumber() == massNumber) {
        result = iso;
      }
    }
    return result == null ? null
        : new Isotope(elementSymbol, result.getAtomicNumber(), result.getExactMass(),
            Math.abs(result.getExactMass() - main.getExactMass()),
            result.getNaturalAbundance() / main.getNaturalAbundance());
  }

  /**
   * The isotope of an element with mass number might include isotopes with no natural abundance.
   *
   * @param elementSymbol An element symbol to search for
   * @return the isotope of an element with specific mass number or empty list if not available
   */
  public static List<Isotope> getIsotopeRecord(String elementSymbol) {
    final IIsotope[] isotopes = getIsotopes(elementSymbol, true);
    final IIsotope main = Arrays.stream(isotopes)
        .max(Comparator.comparingDouble(IIsotope::getNaturalAbundance)).orElse(null);
    if (main == null) {
      return List.of();
    }
    return Arrays.stream(isotopes).filter(iso -> !main.equals(iso)).map(
        result -> new Isotope(elementSymbol, result.getAtomicNumber(), result.getExactMass(),
            Math.abs(result.getExactMass() - main.getExactMass()),
            result.getNaturalAbundance() / main.getNaturalAbundance())).toList();
  }

  /**
   * Returns pairwise m/z differences between stable isotopes within given chemical elements. Final
   * differences are obtained by dividing isotope mass differences with 1, 2, ..., maxCharge values.
   * For example, for elements == [H, C] and maxCharge == 2 this method returns [C13 - C12, (C13 -
   * C12) / 2, H2 - H1, (H2 - H1) / 2], where C12, C13, H2, H1 are corresponding isotopic masses.
   *
   * @param elements  List of chemical elements
   * @param maxCharge Maximum possible charge
   * @return List of pairwise mass differences
   */
  public static List<Double> getIsotopesMzDiffs(List<Element> elements, int maxCharge) {

    List<Double> isotopeMzDiffs = new ArrayList<>();

    // Compute pairwise mass differences within isotopes of each element
    for (Element element : elements) {

      // Filter out not stable isotopes
      List<IIsotope> abundantIsotopes = Arrays.stream(
              isotopes.getIsotopes(element.getAtomicNumber()))
          .filter(i -> Doubles.compare(i.getNaturalAbundance(), 0) > 0d).toList();

      // Compute pairwise mass differences and divide each one with charges up to maxCharge
      for (int i = 0; i < abundantIsotopes.size(); i++) {
        final double smallerMz = abundantIsotopes.get(i).getExactMass();
        for (int j = i + 1; j < abundantIsotopes.size(); j++) {
          for (int charge = 1; charge <= maxCharge; charge++) {
            final double mzDiff = (abundantIsotopes.get(j).getExactMass() - smallerMz) / charge;
            isotopeMzDiffs.add(mzDiff);
          }
        }
      }
    }
    return isotopeMzDiffs;
  }

  /**
   * Returns pairwise m/z differences between stable isotopes within given chemical elements. Final
   * differences are obtained by dividing isotope mass differences with 1, 2, ..., maxCharge values.
   * For example, for elements == [H, C] and maxCharge == 2 this method returns [C13 - C12, (C13 -
   * C12) / 2, H2 - H1, (H2 - H1) / 2], where C12, C13, H2, H1 are corresponding isotopic masses.
   *
   * @param elements  List of chemical elements
   * @param maxCharge Maximum possible charge
   * @return List of pairwise mass differences per charge state starting at index 0 for charge 1.
   */
  public static DoubleArrayList[] getIsotopesMzDiffsForCharge(List<Element> elements,
      int maxCharge) {

    DoubleArrayList[] isotopeMzDiffs = new DoubleArrayList[maxCharge];
    for (int i = 0; i < maxCharge; i++) {
      isotopeMzDiffs[i] = new DoubleArrayList();
    }

    // Compute pairwise mass differences within isotopes of each element
    for (Element element : elements) {
      // Filter out not stable isotopes
      List<IIsotope> abundantIsotopes = Arrays.stream(isotopes.getIsotopes(element.getSymbol()))
          .filter(i -> Doubles.compare(i.getNaturalAbundance(), 0) > 0d).toList();

      // Compute pairwise mass differences and divide each one with charges up to maxCharge
      for (int i = 0; i < abundantIsotopes.size(); i++) {
        final double smallerMz = abundantIsotopes.get(i).getExactMass();
        for (int j = i + 1; j < abundantIsotopes.size(); j++) {
          for (int charge = 1; charge <= maxCharge; charge++) {
            final double mzDiff = (abundantIsotopes.get(j).getExactMass() - smallerMz) / charge;
            // add mz diff to charge list
            isotopeMzDiffs[charge - 1].add(mzDiff);
          }
        }
      }
    }
    // sort each by mz diff
    for (var diffs : isotopeMzDiffs) {
      Collections.sort(diffs);
    }
    return isotopeMzDiffs;
  }

  public static List<Double> getIsotopesMzDiffsCombined(List<Element> elements, int maxCharge,
      MZTolerance mergeTolerance) {
    List<Double> isotopeMzDiffs = new ArrayList<>();

    // Filter out not stable isotopes
    HashMap<Element, List<IIsotope>> isotopesMap = new HashMap<>(elements.size());
    for (Element element : elements) {
      List<IIsotope> list = Arrays.stream(isotopes.getIsotopes(element.getSymbol()))
          .filter(i -> Doubles.compare(i.getNaturalAbundance(), 0) > 0d).toList();
      isotopesMap.put(element, list);
    }

    // Compute pairwise mass differences within isotopes of each element
    for (int charge = 1; charge <= maxCharge; charge++) {
      List<Double> currentChargeDiffs = new ArrayList<>();
      for (Element element : elements) {

        List<IIsotope> abundantIsotopes = isotopesMap.get(element);
        // Compute pairwise mass differences and divide each one with charges up to maxCharge
        for (int i = 0; i < abundantIsotopes.size(); i++) {
          for (int j = i + 1; j < abundantIsotopes.size(); j++) {
            final double deltaMZ =
                (abundantIsotopes.get(j).getExactMass() - abundantIsotopes.get(i).getExactMass())
                    / charge;
            currentChargeDiffs.add(deltaMZ);
          }
        }
      }
      // check for combinations
      Collections.sort(currentChargeDiffs);

      final int currentSize = currentChargeDiffs.size();
      // find combinations of i (smaller mz diffs) (+1, e.g. 13C)
      for (int i = 0; i < currentSize; i++) {
        final Double small = currentChargeDiffs.get(i);
        // that combine to a larger mz diff within mz tolerance (+2, e.g., Br)
        for (int k = currentSize - 1; k > i; k--) {
          final Double large = currentChargeDiffs.get(k);
          final long factor = Math.round(large / small);
          if (factor <= 1L) {
            break;
          }

          final double combined = small * factor;
          if (mergeTolerance.checkWithinTolerance(combined, large)) {
            // merge and add
            // we currently just merge the two mz differences 1:1 because we have no estimate about
            // the maximum number of elements
            currentChargeDiffs.add((combined + large) / 2d);
          }
        }
      }

      //
      isotopeMzDiffs.addAll(currentChargeDiffs);
    }
    return isotopeMzDiffs;
  }

  /**
   * Returns true if given m/z value newMz can be considered as an m/z value of some isotope
   * corresponding to given m/z values in knownMzs. Only isotope m/z differences given in the
   * isotopesMzDiffs are considered.
   *
   * @param newMz           M/z value of interest
   * @param knownMzs        List of known m/z values
   * @param isotopesMzDiffs Pairwise m/z differences between isotopes (supposed to be obtained with
   *                        {@link #getIsotopesMzDiffs(List, int)})
   * @param mzTolerance     Maximum allowed m/z difference to consider any m/z value and isotope m/z
   *                        value equal
   * @return True if new m/z corresponds to an isotope of known m/z's, false otherwise.
   */
  public static boolean isPossibleIsotopeMz(double newMz, @NotNull DoubleArrayList knownMzs,
      @NotNull List<Double> isotopesMzDiffs, @NotNull MZTolerance mzTolerance) {

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
        double realMz = knownMzs.getDouble(mzIndex);

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


  public static boolean isPossibleIsotopeMz(double newMz, @NotNull List<DataPoint> knownMzs,
      @NotNull List<Double> isotopesMzDiffs, @NotNull MZTolerance mzTolerance) {

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
        double realMz = knownMzs.get(mzIndex).getMZ();

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

  /**
   * Used in loops form high to lower m/z to add preceding isotopes first
   */
  public static boolean isPossibleIsotopeMzNegativeDirection(double newMz,
      @NotNull List<DataPoint> knownMzs, @NotNull List<Double> isotopesMzDiffs,
      @NotNull MZTolerance mzTolerance) {

    // Iterate over possible isotope m/z differences
    for (double isotopeMzDiff : isotopesMzDiffs) {

      // Compute theoretical m/z value representing the difference between possible isotope
      // candidate newMz and possible isotope difference
      double theoreticalMz = newMz + isotopeMzDiff;

      Range<Double> theoreticalMzTolRange = mzTolerance.getToleranceRange(theoreticalMz);

      // Go left over m/z's of previously detected peaks and check whether current peak is an
      // isotope of one of them
      // TODO: If in future the method will be slow for some data files it is possible to improve
      //  the speed by implementing a HashSet for doubles with given precision and use it to store
      //  all knownMzs values and check for their presence instead of the following for loop.
      //  O(n^2 / 2) -> O(n) for O(n) memory
      for (int mzIndex = knownMzs.size() - 1; mzIndex >= 0; mzIndex--) {

        // Get real m/z from knownMzs that is going to be compared with the theoretical one
        double realMz = knownMzs.get(mzIndex).getMZ();

        // Do not go left further if the theoretical m/z is higher than real
        if (realMz > theoreticalMzTolRange.upperEndpoint()) {
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


  public static List<DataPoint> findIsotopesInScan(List<Double> isoMzDiffs,
      MZTolerance isoMzTolerance, MassSpectrum spectrum, DataPoint target) {
    double maxIsoMzDiff = Collections.max(isoMzDiffs);
    // add some to the max diff to include more search space
    maxIsoMzDiff += 10 * isoMzTolerance.getMzToleranceForMass(maxIsoMzDiff);
    return findIsotopesInScan(isoMzDiffs, maxIsoMzDiff, isoMzTolerance, spectrum, target);
  }

  public static List<DataPoint> findIsotopesInScan(List<Double> isoMzDiffs, double maxIsoMzDiff,
      MZTolerance isoMzTolerance, MassSpectrum spectrum, DataPoint target) {
    if (spectrum.getNumberOfDataPoints() <= 0) {
      return List.of();
    }

    int dp = spectrum.getNumberOfDataPoints() - 1;

    List<DataPoint> candidates = new ArrayList<>();
    // add the actual data point in the scan, so we don't end up with duplicates.
    final int targetIndex = spectrum.binarySearch(target.getMZ(), true);
    candidates.add(new SimpleDataPoint(spectrum.getMzValue(targetIndex),
        spectrum.getIntensityValue(targetIndex)));

    double mz = spectrum.getMzValue(targetIndex);
    double lastMZ = mz;

    // first try to find preceding isotope signals
    // e.g. if the mono isotopic m/z (smallest mz) is not the most abundant, which is common for
    // compounds with many 13C or Br/Cl or compounds with metals (e.g., Gd)
    for (; dp >= 0 && mz >= lastMZ - maxIsoMzDiff; dp--) {
      mz = spectrum.getMzValue(dp);

      if (IsotopesUtils.isPossibleIsotopeMzNegativeDirection(mz, candidates, isoMzDiffs,
          isoMzTolerance)) {
        candidates.add(new SimpleDataPoint(mz, spectrum.getIntensityValue(dp)));
        lastMZ = mz;
      }
    }

    // sort list of candidates
    candidates.sort(mzSorter);

    mz = spectrum.getMzValue(targetIndex);
    double maxMZ = mz;
    // find all isotopes in + range
    // start at last dp spot
    dp++;
    for (; dp < spectrum.getNumberOfDataPoints() && mz <= maxMZ + maxIsoMzDiff; dp++) {
      mz = spectrum.getMzValue(dp);
      if (IsotopesUtils.isPossibleIsotopeMz(mz, candidates, isoMzDiffs, isoMzTolerance)) {
        final var dataPoint = new SimpleDataPoint(mz, spectrum.getIntensityValue(dp));
        if (mz > maxMZ) {
          candidates.add(dataPoint);
          maxMZ = Math.max(mz, maxMZ);
        } else {
          // insert sort
          insertIfNew(candidates, dataPoint);
        }
      }
    }
    candidates.sort(mzSorter);
    return candidates;
  }

  private static void insertIfNew(List<DataPoint> list, SimpleDataPoint dp) {
    for (int c = list.size() - 1; c >= 0; c--) {
      final double cmz = list.get(c).getMZ();
      switch (Double.compare(cmz, dp.getMZ())) {
        case 0:
          return;
        case -1:
          list.add(c + 1, dp);
          return;
      }
    }
  }
}
