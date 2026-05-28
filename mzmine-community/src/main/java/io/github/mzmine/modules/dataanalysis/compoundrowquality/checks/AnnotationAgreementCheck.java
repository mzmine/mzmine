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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

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
    final List<FeatureAnnotation> annotations = collectAnnotations(row, mode, involved);

    if (annotations.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.ANNOTATION_AGREEMENT,
          QualityCheckStatus.UNAVAILABLE, "No annotations to compare", List.of(), involved);
    }

    // Collapse to "Single annotation" when every annotation maps to the same identity key
    // (InChIKey first block, or molecular formula when the key is missing).
    if (CompoundAnnotationUtils.countUniqueAnnotations(annotations) <= 1) {
      return AnnotationAgreementQualityResult.singleAnnotation(mode, involved);
    }

    final boolean structuresEqual = CompoundAnnotationUtils.allShareInchiKeyFirstBlock(annotations);
    final boolean formulasEqual = CompoundAnnotationUtils.allShareFormula(annotations);
    // Skip the Tanimoto computation when structures already match — the score would be redundant
    // and we hide that row in the UI anyway.
    final Double similarity =
        structuresEqual ? null : CompoundAnnotationUtils.meanPairwiseTanimoto(annotations);
    final boolean lipidConflict = hasLipidConflict(annotations);

    final boolean allAgree = structuresEqual && formulasEqual && !lipidConflict;
    final QualityCheckStatus status = allAgree ? QualityCheckStatus.PASS : QualityCheckStatus.FAIL;

    final List<FeatureAnnotation> sortedUnique = uniqueByInchiKeyBlockSortedByScore(annotations);

    return new AnnotationAgreementQualityResult(status, mode, structuresEqual, formulasEqual,
        lipidConflict, similarity, sortedUnique, involved);
  }

  /// Resolve the {@link AnnotationAgreementCheckType} from the persisted parameter set; falls back
  /// to {@link AnnotationAgreementCheckType#PREFERRED_ANNOTATION} when the parameters are absent
  /// (e.g. the pane is used in a standalone context without a controller-wired ConfigService).
  private static @NotNull AnnotationAgreementCheckType resolveMode(
      final ParameterSet checkParameters) {
    if (checkParameters == null) {
      return AnnotationAgreementCheckType.PREFERRED_ANNOTATION;
    }
    final AnnotationAgreementCheckType value = checkParameters.getValue(
        CompoundRowQualityCheckParameters.annotationAgreementCheckType);
    return value != null ? value : AnnotationAgreementCheckType.PREFERRED_ANNOTATION;
  }

  /// Collect annotations across all member rows according to the chosen mode. Populates
  /// {@code involved} with the member rows that actually contributed at least one annotation.
  private static @NotNull List<@NotNull FeatureAnnotation> collectAnnotations(
      @NotNull CompoundRow row, @NotNull AnnotationAgreementCheckType mode,
      @NotNull List<@NotNull FeatureListRow> involved) {
    final List<FeatureAnnotation> annotations = new ArrayList<>();
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
      if (!contributions.isEmpty()) {
        involved.add(memberRow);
        annotations.addAll(contributions);
      }
    }
    return annotations;
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

  /// Sort {@code annotations} by descending score and deduplicate by InChIKey first block. Falls
  /// back to canonical SMILES then object identity so annotations with no InChIKey still appear.
  /// Annotations without a {@link MolecularStructure} are skipped — only structures can be rendered
  /// in the sub-pane grid.
  private static @NotNull List<@NotNull FeatureAnnotation> uniqueByInchiKeyBlockSortedByScore(
      @NotNull List<@NotNull FeatureAnnotation> annotations) {
    final List<FeatureAnnotation> sorted = annotations.stream()
        .sorted(CompoundAnnotationUtils.getSorterMaxScoreFirst()).toList();
    final LinkedHashMap<String, FeatureAnnotation> byKey = new LinkedHashMap<>();
    for (final FeatureAnnotation a : sorted) {
      final MolecularStructure mol = a.getStructure();
      if (mol == null) {
        continue;
      }
      String key = CompoundAnnotationUtils.inchiKeyFirstBlock(a.getInChIKey());
      if (key == null || key.isBlank()) {
        final String smi = mol.canonicalSmiles();
        key = (smi != null && !smi.isBlank()) ? smi : ("id:" + System.identityHashCode(mol));
      }
      byKey.putIfAbsent(key, a);
    }
    return new ArrayList<>(byKey.values());
  }
}
