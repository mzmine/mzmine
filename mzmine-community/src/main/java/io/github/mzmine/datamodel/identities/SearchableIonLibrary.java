/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.datamodel.identities;

import static io.github.mzmine.datamodel.identities.IonTypeUtils.checkMolCount;
import static io.github.mzmine.datamodel.identities.IonTypeUtils.restrictPartsOverlapToMultimers;
import static java.util.stream.Collectors.groupingBy;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Optimized for searching masses and mz
 */
public class SearchableIonLibrary {

  /**
   * In this list, all ions were grouped by molecule() and totalCharge(), then sorted by totalMass.
   * This way each list can use an early exit from comparison
   */
  private final List<ChargedIonTypeList> ionsSplitByChargeAndMol;

  private final boolean filterByRowCharge;

  /**
   *
   * @param ions
   * @param filterByRowCharge use row charge state or do not apply charge filters
   */
  public SearchableIonLibrary(@NotNull final List<IonType> ions, boolean filterByRowCharge) {
    this.filterByRowCharge = filterByRowCharge;

    // Group by molecules and charge
    final var grouped = ions.stream()
        .collect(groupingBy(IonType::totalCharge, groupingBy(IonType::molecules)));

    // sort each list by mass
    ionsSplitByChargeAndMol = grouped.values().stream()
        .<ChargedIonTypeList>mapMulti((groupMap, consumer) -> {
          // sort all individual groups by mass
          for (List<IonType> group : groupMap.values()) {
            final ArrayList<IonType> sortedGroup = new ArrayList<>(group);
            sortedGroup.sort(IonTypeSorting.MASS.getComparator());
            final IonType first = sortedGroup.getFirst();
            final int charge = first.totalCharge();
            final int molecules = first.molecules();
            consumer.accept(new ChargedIonTypeList(charge, molecules, sortedGroup));
          }
        }).toList();
  }

  /**
   * Search for all pairs of mzs. The resulting list has pairs of ion types as a and b in order of
   * the input mzs. The resulting pairs are the same for a and b also when entered as b and a, just
   * flipped pairs.
   *
   * @param a         row (order does not matter)
   * @param b         row second
   * @param tolerance the mz tolerance, applied to the neutral masses calculated with ions
   * @return list of pairs
   */
  public @NotNull List<IonTypePair> searchRows(@NotNull FeatureListRow a, @NotNull FeatureListRow b,
      @NotNull MZTolerance tolerance) {
    return searchRows(new IonSearchRow(a), new IonSearchRow(b), tolerance);
  }

  /**
   * Search for all pairs of mzs. The resulting list has pairs of ion types as a and b in order of
   * the input mzs. The resulting pairs are the same for a and b also when entered as b and a, just
   * flipped pairs.
   *
   * @param a         mz first (order does not matter)
   * @param b         mz second
   * @param tolerance the mz tolerance, applied to the neutral masses calculated with ions
   * @return list of pairs
   */
  public @NotNull List<IonTypePair> searchRows(@NotNull IonSearchRow a, @NotNull IonSearchRow b,
      @NotNull MZTolerance tolerance) {
    List<IonTypePair> pairs = new ArrayList<>();

    boolean flippedAB = false;
    if (a.mz() > b.mz()) {
      var tmp = a;
      a = b;
      b = tmp;
      flippedAB = true;
    }

    // a is always the lower number to make comparison easier
    for (ChargedIonTypeList ionChargeGroup : ionsSplitByChargeAndMol) {
      // skip whole groups by row charge and polarity if they were provided
      if (!matchesGroupChargeState(a, ionChargeGroup)) {
        continue;
      }

      for (IonType ionA : ionChargeGroup.list()) {
        final double massA = ionA.getMass(a.mz());
        final double absTol = tolerance.getMzToleranceForMass(massA);

        searchPartners(pairs, flippedAB, ionA, massA - absTol, massA + absTol, b);
      }
    }
    return pairs;
  }


  /**
   * Searches for IonTypes that match the neutral mass
   *
   * @param row         row to check against neutral mass of ionNet
   * @param neutralMass to search against
   * @return a list of matching ion identities
   */
  @NotNull
  public List<IonType> searchRows(FeatureListRow row, double neutralMass, MZTolerance mzTol) {
    return searchRows(new IonSearchRow(row), neutralMass, mzTol);
  }

  /**
   * Searches for IonTypes that match the neutral mass
   *
   * @param a           row to check against neutral mass of ionNet
   * @param neutralMass to search against
   * @param mzTol
   * @return a list of matching ion identities
   */
  @NotNull
  public List<IonType> searchRows(IonSearchRow a, double neutralMass, MZTolerance mzTol) {
    final Range<Double> mzSearchRange = mzTol.getToleranceRange(neutralMass);
    List<IonType> results = new ArrayList<>();

    // a is always the lower number to make comparison easier
    for (ChargedIonTypeList ionChargeGroup : ionsSplitByChargeAndMol) {
      // skip whole groups by row charge and polarity if they were provided
      if (!matchesGroupChargeState(a, ionChargeGroup)) {
        continue;
      }

      for (IonType ionA : ionChargeGroup.list()) {
        final double massA = ionA.getMass(a.mz());
        if (mzSearchRange.contains(massA)) {
          results.add(ionA);
        }
      }
    }
    return results;
  }

  /**
   * Charge state filter may be turned off and then only filters by polarity.
   *
   * @param a              search target
   * @param ionChargeGroup the group with ions the same charge
   * @return true if charge and polarity of a matches charge and polarity of the group.
   */
  private boolean matchesGroupChargeState(@NotNull IonSearchRow a,
      ChargedIonTypeList ionChargeGroup) {
    // matches charge and polarity (or each undefined)
    return (!filterByRowCharge || a.signedCharge() == null
        || a.signedCharge() == ionChargeGroup.charge()) //
        && (a.polarity() == null || a.polarity() == ionChargeGroup.polarity());
  }

  /**
   * Find all pairs and add them to the pairs list.
   *
   * @param pairs     the resulting list
   * @param flippedAB true if a and b were flipped
   * @param ionA      the ion of the smaller mz entry
   * @param minMassA  the minimum search range neutral mass
   * @param maxMassA  the maximum search range neutral mass
   * @param b         the larger mz
   */
  private void searchPartners(List<IonTypePair> pairs, boolean flippedAB, IonType ionA,
      double minMassA, double maxMassA, @NotNull IonSearchRow b) {
    final boolean hasNeutralModificationA = IonTypeUtils.hasNeutralModification(ionA);

    // each list has the same charge + molecules
    for (ChargedIonTypeList ionChargeGroup : ionsSplitByChargeAndMol) {
      if (
        // check polarity and charge state
          !matchesGroupChargeState(b, ionChargeGroup)
              // require 1M or different M for both - to not match ions
              // like 2M+H <> 2M+Na need to match to another 1M
              // 2M <> 4M restricted as this is the same as 1M <> 2M
              || !checkMolCount(ionA.molecules(), ionChargeGroup.molecules())) {
        continue;
      }

      // quick check if last element is too large then all other can be skipped
      IonType last = ionChargeGroup.list().getLast();
      double massB = last.getMass(b.mz());
      if (massB > maxMassA) {
        // early exit where all ions have the same charge and mol multipliers
        // and ions are sorted by mass (which is subtracted from mzB)
        // so ions before last will be even larger
        continue;
      }

      // now loop through the rest of ions
      for (IonType ionB : ionChargeGroup.list()) {
        massB = ionB.getMass(b.mz());
        if (massB < minMassA) {
          // early exit where all ions have the same charge and mol multipliers
          // and ions are sorted by mass (which is subtracted from mzB)
          // so ions after this ion will be even smaller
          break;
        }

        // check all other conditions after checking mass
        // mass check first for early exit
        if (massB <= maxMassA && //
            // one needs no neutral modification otherwise we are comparing to many weird ions
            !(hasNeutralModificationA && IonTypeUtils.hasNeutralModification(ionB)) &&
            // no parts overlap allowed for single charge & single mol.
            restrictPartsOverlapToMultimers(ionA, ionB) //
        ) {
          // found a match
          if (flippedAB) {
            pairs.add(new IonTypePair(ionB, ionA));
          } else {
            pairs.add(new IonTypePair(ionA, ionB));
          }
        }
      }
    }
  }

  /**
   *
   * @param charge    group charge the same for all ions
   * @param molecules group molecules count for all ions
   * @param polarity  the polarity matching the charge
   * @param list      ions in group
   */
  private record ChargedIonTypeList(int charge, int molecules, @NotNull PolarityType polarity,
                                    @NotNull List<IonType> list) {

    public ChargedIonTypeList(int charge, int molecules, @NotNull List<IonType> list) {
      // use neutral if case adducts are actually just neutral losses
      this(charge, molecules, PolarityType.fromInt(charge, PolarityType.NEUTRAL), list);
    }
  }
}
