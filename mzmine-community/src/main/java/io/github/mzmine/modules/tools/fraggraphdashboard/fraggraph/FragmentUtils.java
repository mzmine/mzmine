/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.FormulaWithExactMz;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.openscience.cdk.config.Isotopes;
import org.openscience.cdk.formula.MolecularFormulaRange;
import org.openscience.cdk.interfaces.IIsotope;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class FragmentUtils {

  @NotNull
  public static MolecularFormulaRange setupFormulaRange(@NotNull List<IonType> ionTypes) {
    final MolecularFormulaRange elementCounts = new MolecularFormulaRange();

    try {
      final Isotopes isos = Isotopes.getInstance();
      elementCounts.addIsotope(isos.getMajorIsotope("C"), 0, 100);
      elementCounts.addIsotope(isos.getMajorIsotope("H"), 0, 300);
      elementCounts.addIsotope(isos.getMajorIsotope("O"), 0, 10);
      elementCounts.addIsotope(isos.getMajorIsotope("N"), 0, 10);
      elementCounts.addIsotope(isos.getMajorIsotope("P"), 0, 2);
      elementCounts.addIsotope(isos.getMajorIsotope("S"), 0, 2);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    for (IonType ionType : ionTypes) {
      if (ionType.isUndefinedAdduct()) {
        continue;
      }

      // check if we have "unusual" adducts (other than m+h and m+nh4) which are not covered by the above
      reflectIonTypeInFormulaRange(ionType, elementCounts);
    }
    return elementCounts;
  }

  public static void reflectIonTypeInFormulaRange(IonType ionType,
      MolecularFormulaRange elementCounts) {
    final IMolecularFormula adductFormula = ionType.getAdduct().getCDKFormula();

    if (adductFormula != null) {
      reflectFormulaInMolecularFormulaRange(elementCounts, adductFormula);
    }

    if (ionType.getModification() != null && ionType.getModification().getCDKFormula() != null) {
      final IMolecularFormula modificationFormula = ionType.getModification().getCDKFormula();
      reflectFormulaInMolecularFormulaRange(elementCounts, modificationFormula);
    }
  }

  public static void reflectIonTypeInFormulaRange(IonizationType ionType,
      MolecularFormulaRange elementCounts) {
    final IMolecularFormula adductFormula = ionType.getAddedFormula();

    if (adductFormula != null) {
      reflectFormulaInMolecularFormulaRange(elementCounts, adductFormula);
    }
  }

  public static void reflectFormulaInMolecularFormulaRange(MolecularFormulaRange elementCounts,
      IMolecularFormula formula) {
    for (IIsotope isotope : formula.isotopes()) {
      if (!elementCounts.contains(isotope)) {
        elementCounts.addIsotope(isotope, 0, 1);
      } else {
        increaseMaxIsotopeCountByOne(isotope, elementCounts);
      }
    }
  }

  private static void increaseMaxIsotopeCountByOne(IIsotope isotope,
      MolecularFormulaRange elementCounts) {
    final int maxCount = elementCounts.getIsotopeCountMax(isotope);
    final int minCount = elementCounts.getIsotopeCountMin(isotope);
    elementCounts.removeIsotope(isotope);
    elementCounts.addIsotope(isotope, minCount, maxCount + 1);
  }

  /**
   * @return A list of peaks with all possible formulae for all peaks. The peaks are sorted by
   * ascending mz.
   */
  public static List<SignalWithFormulae> getPeaksWithFormulae(IMolecularFormula ionFormula,
      MassSpectrum mergedMs2, SpectralSignalFilter defaultSignalFilter,
      MZTolerance fragmentFormulaTol) {
    if (mergedMs2.getNumberOfDataPoints() == 0) {
      return List.of();
    }

    final List<FormulaWithExactMz> subFormulae = List.of(
        FormulaUtils.getAllFormulas(ionFormula, mergedMs2.getMzValue(0) - 1));

    double precursorMz = FormulaUtils.calculateMzRatio(ionFormula);
    DataPoint[] intenseSignals = ScanUtils.extractDataPoints(mergedMs2);
    intenseSignals = defaultSignalFilter.applyFilterAndSortByIntensity(intenseSignals, precursorMz);
    Arrays.sort(intenseSignals, DataPointSorter.DEFAULT_MZ_ASCENDING);

    List<SignalWithFormulae> peaksWithFormulae = new ArrayList<>();
    for (DataPoint signal : intenseSignals) {
      final IndexRange indexRange = BinarySearch.indexRange(
          fragmentFormulaTol.getToleranceRange(signal.getMZ()), subFormulae,
          FormulaWithExactMz::mz);
      peaksWithFormulae.add(new SignalWithFormulae(signal, indexRange.sublist(subFormulae)));
    }
    return peaksWithFormulae;
  }
}
