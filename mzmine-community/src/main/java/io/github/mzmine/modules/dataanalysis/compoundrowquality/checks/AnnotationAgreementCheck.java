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

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.CompoundRowQualityCheckParameters;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Checks whether the {@link FeatureAnnotation}s on the member rows agree about the underlying
/// compound. Three modes ({@link AnnotationAgreementCheckType}) control which annotations are
/// compared:
/// - PREFERRED_ANNOTATION: only {@link FeatureListRow#getPreferredAnnotation()} per member.
/// - BEST_PER_ANNOTATION_METHOD: one top annotation per annotation type per member, analog matches
/// excluded.
/// - ALL_ANNOTATIONS: every annotation across all annotation methods of every member.
///
/// Compares InChIKey first block (connectivity, ignores stereochemistry / isotopes), molecular
/// formula, mean pairwise Tanimoto similarity over CDK fingerprints, and — when two or more
/// {@link MatchedLipid}s are present — whether their lipid labels conflict.
public final class AnnotationAgreementCheck implements QualityCheck {

  private static final Logger logger = Logger.getLogger(AnnotationAgreementCheck.class.getName());

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.ANNOTATION_AGREEMENT;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final AnnotationAgreementCheckType mode = resolveMode(context.checkParameters());

    final List<FeatureListRow> involved = new ArrayList<>();
    // (annotation, sourceRow) pairs — order matters: this is the order the rows appear on the
    // compound, which we later collapse using a score-sorted second pass.
    final List<FeatureAnnotationWithRow> withRow = collectAnnotations(row, mode, involved);

    if (withRow.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.ANNOTATION_AGREEMENT,
          QualityCheckStatus.UNAVAILABLE, "No annotations to compare", List.of(), involved);
    }

    final List<FeatureAnnotation> annotations = withRow.stream().map(FeatureAnnotationWithRow::a)
        .toList();

    // Collapse to "Single annotation" when every annotation maps to the same identity key
    // (InChIKey first block, or molecular formula when the key is missing).
    if (CompoundAnnotationUtils.countUniqueAnnotations(annotations) <= 1) {
      return AnnotationAgreementQualityResult.singleAnnotation(mode, involved);
    }

    // The structure-equals check skips annotations without a MolecularStructure so a single
    // missing-structure annotation doesn't tip the verdict to "mismatch". With < 2 structure-
    // bearing annotations there is nothing to disagree with structurally.
    final List<FeatureAnnotation> annotationsWithStructure = annotations.stream()
        .filter(a -> a.getStructure() != null).toList();
    final boolean structuresEqual =
        annotationsWithStructure.size() < 2 || CompoundAnnotationUtils.allShareInchiKeyFirstBlock(
            annotationsWithStructure);
    final boolean formulasEqual = CompoundAnnotationUtils.allShareFormula(annotations);
    // Skip the Tanimoto computation when structures already match — the score would be redundant
    // and we hide that row in the UI anyway.
    final Double similarity =
        structuresEqual ? null : CompoundAnnotationUtils.meanPairwiseTanimoto(annotations);
    final boolean lipidConflict = hasLipidConflict(annotations);

    final boolean allAgree = structuresEqual && formulasEqual && !lipidConflict;
    final QualityCheckStatus status = allAgree ? QualityCheckStatus.PASS : QualityCheckStatus.FAIL;

    // The visual order of structures in the sub pane follows the feature list's AnnotationSummary
    // sort config (descending) — the same comparator used by the main "Compound annotation" check,
    // so the agreement card ranks structures the way the user has already configured ranking.
    final Comparator<@Nullable AnnotationSummary> annotationSorter = row.getFeatureList()
        .getAnnotationSortConfig().sortOrder().getComparatorHighFirst();
    final List<AnnotationAgreementGroup> groups = buildGroups(withRow, annotationSorter);

    // Bridge for the in-card source ComboBox: build a cloned ParameterSet with the new check type
    // and feed it to the controller-supplied update callback. The callback persists the clone
    // into MZmineConfiguration and pushes it onto the model, which fires the recompute
    // subscription and re-runs all checks (including this one) with the new mode.
    final Consumer<@NotNull AnnotationAgreementCheckType> onCheckTypeChange = buildOnCheckTypeChange(
        context);

    return new AnnotationAgreementQualityResult(status, mode, structuresEqual, formulasEqual,
        lipidConflict, similarity, groups, involved, context.colorAssignment(),
        context.selectedMemberRow(), context.onEvent(), onCheckTypeChange);
  }

  /// Wrap {@link QualityCheckContext#onCheckParametersUpdate()} into a typed consumer that just
  /// takes the new {@link AnnotationAgreementCheckType}. Returns null when no update callback is
  /// wired (standalone use), which the result class interprets as "make the combo read-only".
  private static @Nullable Consumer<@NotNull AnnotationAgreementCheckType> buildOnCheckTypeChange(
      @NotNull QualityCheckContext context) {
    final Consumer<@NotNull ParameterSet> sink = context.onCheckParametersUpdate();
    if (sink == null) {
      return null;
    }
    final ParameterSet base = context.checkParameters();
    return newType -> {
      // Clone so we never mutate the live model parameters in place — the recompute subscription
      // only fires when the property reference changes.
      final ParameterSet clone = base.cloneParameterSet();
      clone.setParameter(CompoundRowQualityCheckParameters.annotationAgreementCheckType, newType);
      sink.accept(clone);
    };
  }

  /// Resolve the {@link AnnotationAgreementCheckType} from the persisted parameter set. The model
  /// always seeds {@link QualityCheckContext#checkParameters()} from the module defaults, so the
  /// combo value is always present; the {@code ALL_ANNOTATIONS} fallback only guards against a
  /// hypothetical legacy parameter set without that combo entry.
  private static @NotNull AnnotationAgreementCheckType resolveMode(
      @NotNull ParameterSet checkParameters) {
    final AnnotationAgreementCheckType value = checkParameters.getValue(
        CompoundRowQualityCheckParameters.annotationAgreementCheckType);
    return value != null ? value : AnnotationAgreementCheckType.ALL_ANNOTATIONS;
  }

  /// Collect (annotation, sourceRow) pairs across all member rows according to the chosen mode.
  /// Populates {@code involved} with the member rows that actually contributed at least one
  /// annotation.
  private static @NotNull List<FeatureAnnotationWithRow> collectAnnotations(
      @NotNull CompoundRow row, @NotNull AnnotationAgreementCheckType mode,
      @NotNull List<@NotNull FeatureListRow> involved) {
    final List<FeatureAnnotationWithRow> out = new ArrayList<>();
    for (final CompoundFeatureMember m : row.getCompoundMembers()) {
      final FeatureListRow memberRow = m.row();
      final List<FeatureAnnotation> contributions = switch (mode) {
        case PREFERRED_ANNOTATION -> {
          final FeatureAnnotation pref = memberRow.getPreferredAnnotation();
          yield pref == null ? List.<FeatureAnnotation>of() : List.of(pref);
        }
        case BEST_PER_ANNOTATION_METHOD ->
          // One top annotation per annotation type — analog spectral matches excluded so we don't
          // compare against modification-aware hits the user typically considers separate.
            CompoundAnnotationUtils.getTopAnnotationsPerType(memberRow).stream()
                .filter(a -> !a.isAnalogMatch()).toList();
        case ALL_ANNOTATIONS -> memberRow.getAllFeatureAnnotations();
      };
      if (contributions.isEmpty()) {
        continue;
      }
      involved.add(memberRow);
      for (final FeatureAnnotation a : contributions) {
        out.add(new FeatureAnnotationWithRow(a, memberRow));
      }
    }
    return out;
  }

  /// Group (annotation, row) pairs by InChIKey first block (with canonical-SMILES / identity
  /// fallbacks). Pairs are sorted by the supplied {@link AnnotationSummary} comparator first, so
  /// the surviving group order (preserved by {@link LinkedHashMap}) and each group's representative
  /// match the feature list's configured annotation ranking — the same order the "Compound
  /// annotation" check uses. Groups without any structure-bearing annotation are dropped (they
  /// cannot be rendered as a 2D structure cell). The agreeing-row list collects all distinct source
  /// rows for that group, sorted by ascending m/z for stable display.
  private static @NotNull List<AnnotationAgreementGroup> buildGroups(
      @NotNull List<FeatureAnnotationWithRow> withRow,
      @NotNull Comparator<@Nullable AnnotationSummary> annotationSorter) {
    // Cache the AnnotationSummary per pair so the comparator isn't re-invoked at O(n log n) cost
    // recomputing summaries. Order matters: the first pair that wins the sort defines both the
    // group's display position and the chosen representative annotation.
    record SortedPair(@NotNull FeatureAnnotation a, @NotNull FeatureListRow row,
                      @NotNull AnnotationSummary summary) {

    }
    final List<SortedPair> sorted = new ArrayList<>(withRow.size());
    for (final FeatureAnnotationWithRow p : withRow) {
      sorted.add(new SortedPair(p.a(), p.row(), AnnotationSummary.of(p.row(), p.a())));
    }
    sorted.sort((x, y) -> annotationSorter.compare(x.summary(), y.summary()));

    final LinkedHashMap<String, GroupBuilder> byKey = new LinkedHashMap<>();
    for (final SortedPair p : sorted) {
      final String key = groupKey(p.a());
      final GroupBuilder gb = byKey.computeIfAbsent(key, k -> new GroupBuilder());
      gb.agreeingRows.add(p.row());
      if (gb.representative == null && p.a().getStructure() != null) {
        gb.representative = p.a();
        gb.representativeRow = p.row();
      }
    }

    final List<AnnotationAgreementGroup> groups = new ArrayList<>();
    for (final GroupBuilder gb : byKey.values()) {
      if (gb.representative == null || gb.representativeRow == null) {
        continue;
      }
      final List<FeatureListRow> sortedRows = new ArrayList<>(gb.agreeingRows);
      sortedRows.sort(Comparator.comparingDouble(FeatureListRow::getAverageMZ));
      groups.add(new AnnotationAgreementGroup(gb.representative, gb.representativeRow, sortedRows));
    }
    return groups;
  }

  private static @NotNull String groupKey(@NotNull FeatureAnnotation a) {
    final String firstBlock = CompoundAnnotationUtils.inchiKeyFirstBlock(a.getInChIKey());
    if (firstBlock != null && !firstBlock.isBlank()) {
      return "K:" + firstBlock;
    }
    final MolecularStructure mol = a.getStructure();
    if (mol != null) {
      final String smi = mol.canonicalSmiles();
      if (smi != null && !smi.isBlank()) {
        return "S:" + smi;
      }
    }
    final String formula = a.getFormula();
    if (formula != null && !formula.isBlank()) {
      return "F:" + formula.trim();
    }
    // last resort — keep distinct by identity so two unidentified annotations stay separate
    return "id:" + System.identityHashCode(a);
  }

  /// Detect a conflict among {@link MatchedLipid} annotations: returns true when at least two
  /// matched lipids exist and they disagree on {@link ILipidAnnotation#getAnnotation()}.
  private static boolean hasLipidConflict(@NotNull List<@NotNull FeatureAnnotation> annotations) {
    final Set<String> labels = new HashSet<>();
    int lipidCount = 0;
    for (final FeatureAnnotation a : annotations) {
      if (a instanceof MatchedLipid ml) {
        lipidCount++;
        final ILipidAnnotation la = ml.getLipidAnnotation();
        if (la == null) {
          continue;
        }
        final String label = la.getAnnotation();
        if (label != null && !label.isBlank()) {
          labels.add(label.trim());
        }
      }
    }
    return lipidCount >= 2 && labels.size() >= 2;
  }

  /// Lightweight pair used to thread the (annotation, source-row) relationship from collection to
  /// grouping without allocating a public record for an internal step.
  private record FeatureAnnotationWithRow(@NotNull FeatureAnnotation a,
                                          @NotNull FeatureListRow row) {

  }

  /// Mutable scratch state for collecting the rows + representative of one group during the pass
  /// over score-sorted annotations. Lives only inside {@link #buildGroups}.
  private static final class GroupBuilder {

    private @Nullable FeatureAnnotation representative;
    private @Nullable FeatureListRow representativeRow;
    // LinkedHashSet to preserve first-seen insertion order; we re-sort by m/z when freezing.
    private final java.util.LinkedHashSet<FeatureListRow> agreeingRows = new java.util.LinkedHashSet<>();
  }
}
