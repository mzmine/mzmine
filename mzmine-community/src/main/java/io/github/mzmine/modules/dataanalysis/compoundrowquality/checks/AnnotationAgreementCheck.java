package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Verifies that all annotated member rows resolve to the same compound identity.
public final class AnnotationAgreementCheck implements QualityCheck {

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.ANNOTATION_AGREEMENT;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    // group by InChIKey first, then SMILES, then formula. keep insertion order so the first
    // member's annotation appears first in the summary.
    final Map<String, List<FeatureListRow>> groups = new LinkedHashMap<>();
    final Map<String, String> displayNameByKey = new LinkedHashMap<>();
    final List<FeatureListRow> involved = new ArrayList<>();

    for (final CompoundFeatureMember m : row.getCompoundMembers()) {
      final List<CompoundDBAnnotation> annotations = m.row().getCompoundAnnotations();
      if (annotations.isEmpty()) {
        continue;
      }
      involved.add(m.row());
      final CompoundDBAnnotation best = annotations.getFirst();
      final String key = annotationKey(best);
      if (key == null) {
        continue;
      }
      groups.computeIfAbsent(key, _ -> new ArrayList<>()).add(m.row());
      displayNameByKey.putIfAbsent(key, annotationDisplayName(best));
    }

    if (groups.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.ANNOTATION_AGREEMENT,
          QualityCheckStatus.UNAVAILABLE, "No annotations to compare", List.of(), involved);
    }

    if (groups.size() == 1) {
      final String name = displayNameByKey.values().iterator().next();
      return new DefaultQualityCheckResult(QualityCheckType.ANNOTATION_AGREEMENT,
          QualityCheckStatus.PASS, "All point to %s".formatted(name), List.of(), involved);
    }

    final List<String> details = new ArrayList<>(groups.size());
    for (final Map.Entry<String, List<FeatureListRow>> e : groups.entrySet()) {
      details.add("%s — %d row%s".formatted(displayNameByKey.get(e.getKey()), e.getValue().size(),
          e.getValue().size() == 1 ? "" : "s"));
    }
    return new DefaultQualityCheckResult(QualityCheckType.ANNOTATION_AGREEMENT, QualityCheckStatus.FAIL,
        "%d conflicting annotations".formatted(groups.size()), details, involved);
  }

  private static @Nullable String annotationKey(@NotNull CompoundDBAnnotation a) {
    final String inchiKey = a.getInChIKey();
    if (inchiKey != null && !inchiKey.isBlank()) {
      return "K:" + inchiKey;
    }
    final String smiles = a.getSmiles();
    if (smiles != null && !smiles.isBlank()) {
      return "S:" + smiles;
    }
    final String formula = a.getFormula();
    if (formula != null && !formula.isBlank()) {
      return "F:" + formula;
    }
    final String name = a.getCompoundName();
    if (name != null && !name.isBlank()) {
      return "N:" + name;
    }
    return null;
  }

  private static @NotNull String annotationDisplayName(@NotNull CompoundDBAnnotation a) {
    final String name = a.getCompoundName();
    final String formula = a.getFormula();
    if (name != null && !name.isBlank() && formula != null && !formula.isBlank()) {
      return "%s (%s)".formatted(name, formula);
    }
    if (name != null && !name.isBlank()) {
      return name;
    }
    if (formula != null && !formula.isBlank()) {
      return formula;
    }
    final String smiles = a.getSmiles();
    if (smiles != null && !smiles.isBlank()) {
      return smiles;
    }
    return "<unnamed>";
  }
}
