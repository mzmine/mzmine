/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_gc_ei_ion_notation;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.identities.iontype.IonParts;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.restrictions.rdbe.RDBERestrictionChecker;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.FormulaWithExactMz;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Explains EI ion m/z values by sub formulas of a molecular formula. The molecular ion [M]+ is the
 * radical cation M-e. Every other ion is the molecular ion minus a neutral loss, written as
 * [M-loss-e]+.
 */
public final class EiFragmentIonAnnotator {

  /**
   * Limit the number of enumerated sub formulas. The number is the product of all element counts
   * + 1, e.g. 6820 for C30H54OSi.
   */
  static final long MAX_SUB_FORMULAS = 2_000_000;
  /**
   * A fragment ion needs at least the RDBE of a saturated even-electron cation like CH3+ (0.5) or
   * odd-electron cation like CH4+ (0). Lower values cannot be built from the elements.
   */
  private static final double MIN_RDBE = -0.5;

  private final @NotNull IMolecularFormula molecularIon;
  private final @NotNull List<FormulaWithExactMz> subFormulas;
  private final @NotNull MZTolerance mzTolerance;

  private EiFragmentIonAnnotator(@NotNull final IMolecularFormula molecularIon,
      @NotNull final List<FormulaWithExactMz> subFormulas,
      @NotNull final MZTolerance mzTolerance) {
    this.molecularIon = molecularIon;
    this.subFormulas = subFormulas;
    this.mzTolerance = mzTolerance;
  }

  /**
   * @param molecularFormula the neutral molecular formula of the compound, any charge in the
   *                         formula string is replaced by +1
   * @param mzTolerance      tolerance to match ion m/z values
   * @param minMz            lowest m/z that needs to be explained
   * @return the annotator or null if the formula cannot be parsed or has too many sub formulas
   */
  public static @Nullable EiFragmentIonAnnotator create(@Nullable final String molecularFormula,
      @NotNull final MZTolerance mzTolerance, final double minMz) {
    // assumption: annotation formulas are the neutral molecule, EI forms the radical cation M-e
    final IMolecularFormula molecularIon = FormulaUtils.createMajorIsotopeMolFormulaWithCharge(
        molecularFormula, 1);
    if (molecularIon == null || molecularIon.getIsotopeCount() == 0
        || countSubFormulas(molecularIon) > MAX_SUB_FORMULAS) {
      return null;
    }
    // sub formulas keep the +1 charge so their m/z is electron corrected
    final FormulaWithExactMz[] subFormulas = FormulaUtils.getAllFormulas(molecularIon,
        mzTolerance.getToleranceRange(minMz).lowerEndpoint());
    if (subFormulas == null) {
      return null;
    }
    return new EiFragmentIonAnnotator(molecularIon, List.of(subFormulas), mzTolerance);
  }

  /**
   * @return the number of sub formulas including the formula itself
   */
  static long countSubFormulas(@NotNull final IMolecularFormula formula) {
    long count = 1;
    for (final IIsotope isotope : formula.isotopes()) {
      count *= formula.getIsotopeCount(isotope) + 1L;
      if (count > MAX_SUB_FORMULAS) {
        return count;
      }
    }
    return count;
  }

  /**
   * @return the monoisotopic mass of the neutral molecule M
   */
  public double getNeutralMass() {
    return FormulaUtils.getMonoisotopicMass(molecularIon, 0);
  }

  /**
   * @return the m/z of the molecular ion [M]+ (radical cation)
   */
  public double getMolecularIonMz() {
    return FormulaUtils.calculateMzRatio(molecularIon);
  }

  /**
   * Explain an m/z by the sub formula with the smallest m/z error.
   *
   * @param mz the measured m/z
   * @return the explained ion or null if no sub formula is within the tolerance
   */
  public @Nullable EiFragmentIon annotate(final double mz) {
    final Range<Double> mzRange = mzTolerance.getToleranceRange(mz);
    final IndexRange indices = BinarySearch.indexRange(mzRange, subFormulas,
        FormulaWithExactMz::mz);

    FormulaWithExactMz best = null;
    for (int i = indices.min(); i < indices.maxExclusive(); i++) {
      final FormulaWithExactMz candidate = subFormulas.get(i);
      final Double rdbe = RDBERestrictionChecker.calculateRDBE(candidate.formula());
      if (rdbe == null || rdbe < MIN_RDBE) {
        continue;
      }
      // decision: smallest m/z error wins, there is no fragmentation model for EI
      if (best == null || Math.abs(candidate.mz() - mz) < Math.abs(best.mz() - mz)) {
        best = candidate;
      }
    }
    if (best == null) {
      return null;
    }
    final IonType ionType = createIonType(best.formula());
    if (ionType == null) {
      return null;
    }
    return new EiFragmentIon(best.formula(), ionType, best.mz(), mz - best.mz());
  }

  /**
   * @return [M-e]+ for the molecular ion, otherwise [M-loss-e]+ with the neutral loss, or null if
   * the fragment is no sub formula
   */
  private @Nullable IonType createIonType(@NotNull final IMolecularFormula fragmentIon) {
    final Optional<IMolecularFormula> loss = FormulaUtils.subtractFormula(molecularIon,
        fragmentIon, true);
    if (loss.isEmpty()) {
      return null;
    }
    final IMolecularFormula neutralLoss = loss.get();
    neutralLoss.setCharge(0);
    if (neutralLoss.getIsotopeCount() == 0) {
      return IonTypes.M_PLUS.asIonType();
    }
    final String lossFormula = FormulaUtils.getFormulaString(neutralLoss, false);
    return IonType.create(IonParts.M_PLUS, IonParts.ofFormula(lossFormula, 0, -1));
  }
}
