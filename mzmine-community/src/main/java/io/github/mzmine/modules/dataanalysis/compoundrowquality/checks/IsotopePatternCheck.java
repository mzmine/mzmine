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

package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardColoring;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.IsotopePatternQualityResult.PredictedPattern;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.checks.IsotopePatternQualityResult.RowIsotopes;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.tools.isotopeprediction.IsotopePatternCalculator;
import io.github.mzmine.util.FormulaUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/// Collects the MS1 isotope evidence of every member row: the detected pattern (one entry per
/// charge-state hypothesis) and the pattern predicted for the row's formula. Every row is resolved
/// here on the background thread so [IsotopePatternQualityResult] can follow the selected adduct
/// row on the FX thread without recomputing.
public final class IsotopePatternCheck implements QualityCheck {

  /// Same threshold {@link FeatureAnnotation#calculateIsotopePattern()} uses, so both prediction
  /// paths agree.
  private static final double MIN_PREDICTED_ABUNDANCE = 0.005;

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.ISOTOPE_PATTERN;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final FeatureListRow preferred = row.getPreferredRow();
    // every row that can become the selected adduct row: the member rows plus their nested
    // sub-rows, the same set the dashboard's legend and coloring use
    final Map<FeatureListRow, RowIsotopes> byRow = new HashMap<>();
    byRow.put(preferred, resolve(preferred));
    for (final FeatureListRow member : CompoundDashboardColoring.flattenAllMemberRows(row)) {
      byRow.computeIfAbsent(member, IsotopePatternCheck::resolve);
    }

    // QualityCheckItem renders the status icon once, so it cannot follow the live selection and
    // instead describes the row the card starts on
    final FeatureListRow initial = initialRow(context, preferred, byRow);
    final QualityCheckStatus status =
        byRow.getOrDefault(initial, RowIsotopes.EMPTY).chargeStates().isEmpty()
            ? QualityCheckStatus.WARN : QualityCheckStatus.PASS;
    return new IsotopePatternQualityResult(status, byRow, preferred, context.selectedMemberRow(),
        List.of(preferred));
  }

  /// The row the card shows first. Reading the selection property off the FX thread is a plain
  /// value read, no scene graph involved.
  private static @NotNull FeatureListRow initialRow(@NotNull final QualityCheckContext context,
      @NotNull final FeatureListRow preferred,
      @NotNull final Map<FeatureListRow, RowIsotopes> byRow) {
    if (context.selectedMemberRow() == null) {
      return preferred;
    }
    final FeatureListRow selected = context.selectedMemberRow().getValue();
    return selected != null && byRow.containsKey(selected) ? selected : preferred;
  }

  /// The detected hypotheses plus the predicted pattern of one member row.
  private static @NotNull RowIsotopes resolve(@NotNull final FeatureListRow row) {
    final List<IsotopePattern> chargeStates = chargeStates(row.getBestIsotopePattern());
    final IsotopePattern best = chargeStates.isEmpty() ? null : chargeStates.getFirst();
    return new RowIsotopes(chargeStates,
        normalizeToMeasured(resolvePredictedPattern(row), best, row));
  }

  /// Unpack a [MultiChargeStateIsotopePattern] into its hypotheses, best first.
  private static @NotNull List<@NotNull IsotopePattern> chargeStates(
      @Nullable final IsotopePattern best) {
    if (best instanceof MultiChargeStateIsotopePattern multi) {
      return List.copyOf(multi.getPatterns());
    }
    return best == null ? List.of() : List.of(best);
  }

  /// The predicted pattern shown next to the detected one, plus the formula it belongs to (the
  /// dataset label). Preference order: the annotation's stored pattern, one predicted from its
  /// formula + adduct, then the row's best formula prediction. Null when no formula is available at
  /// all, and the card then shows the detected pattern alone.
  private static @Nullable PredictedPattern resolvePredictedPattern(
      @NotNull final FeatureListRow row) {
    final FeatureAnnotation annotation = row.getPreferredAnnotation();
    if (annotation != null) {
      final IsotopePattern stored = annotation.getIsotopePattern();
      final IsotopePattern pattern = stored != null ? stored : annotation.calculateIsotopePattern();
      if (pattern != null) {
        return new PredictedPattern(pattern, annotationFormula(annotation));
      }
    }
    // fall back to formula prediction; the list is ranked, so the first entry is the best formula
    final List<ResultFormula> formulas = row.getFormulas();
    if (formulas.isEmpty()) {
      return null;
    }
    final ResultFormula best = formulas.getFirst();
    IsotopePattern pattern = best.getPredictedIsotopes();
    if (pattern == null) {
      final IMolecularFormula formula = best.getFormulaAsObject();
      final PolarityType polarity = row.getRepresentativePolarity();
      if (formula == null || polarity == null || !polarity.isDefined()) {
        return null;
      }
      // assumption: formula prediction stores the ION formula, so no adduct has to be applied
      final Integer formulaCharge = formula.getCharge();
      final int charge = formulaCharge == null || formulaCharge == 0 ? 1 : Math.abs(formulaCharge);
      pattern = IsotopePatternCalculator.calculateIsotopePattern(formula, MIN_PREDICTED_ABUNDANCE,
          charge, polarity, false);
    }
    return pattern == null ? null : new PredictedPattern(pattern, best.getFormulaAsString());
  }

  private static @Nullable String annotationFormula(@NotNull final FeatureAnnotation annotation) {
    final String formula = annotation.getFormula();
    if (formula != null && !formula.isBlank()) {
      return formula;
    }
    final MolecularStructure structure = annotation.getStructure();
    return structure == null ? null : FormulaUtils.getFormulaString(structure.formula());
  }

  /// Scale the predicted pattern onto the measured intensity scale, or its 0..1 abundances would be
  /// invisible next to raw MS1 intensities. Prefers the detected pattern's base peak, else the
  /// tallest measured signal inside the predicted m/z window.
  private static @Nullable PredictedPattern normalizeToMeasured(
      @Nullable final PredictedPattern predicted, @Nullable final IsotopePattern detected,
      @NotNull final FeatureListRow row) {
    if (predicted == null || predicted.pattern().getNumberOfDataPoints() == 0) {
      return null;
    }
    final IsotopePattern pattern = predicted.pattern();
    Double target = detected == null ? null : detected.getBasePeakIntensity();
    if (target == null || target <= 0d) {
      final Range<Double> mzRange = pattern.getDataPointMZRange();
      target = maxIntensityInRange(pickRepresentativeScan(row), mzRange.lowerEndpoint() - 2.5,
          mzRange.upperEndpoint() + 2.5);
    }
    if (target == null || target <= 0d) {
      return predicted;
    }
    return new PredictedPattern(IsotopePatternCalculator.normalizeIsotopePattern(pattern, target),
        predicted.formula());
  }

  /// Tallest intensity within {@code [minMZ, maxMZ]}, or null when there is no signal there.
  private static @Nullable Double maxIntensityInRange(@Nullable final Scan scan, final double minMZ,
      final double maxMZ) {
    if (scan == null) {
      return null;
    }
    double max = 0d;
    for (int i = 0; i < scan.getNumberOfDataPoints(); i++) {
      final double mz = scan.getMzValue(i);
      if (mz < minMZ) {
        continue;
      }
      // assumption: scan data points are sorted by m/z, so we can stop at the upper bound.
      if (mz > maxMZ) {
        break;
      }
      max = Math.max(max, scan.getIntensityValue(i));
    }
    return max > 0d ? max : null;
  }

  /// Only used to scale a predicted pattern when nothing was detected.
  private static @Nullable Scan pickRepresentativeScan(@NotNull final FeatureListRow row) {
    final Feature best = row.getBestFeature();
    return best == null ? null : best.getRepresentativeScan();
  }
}
