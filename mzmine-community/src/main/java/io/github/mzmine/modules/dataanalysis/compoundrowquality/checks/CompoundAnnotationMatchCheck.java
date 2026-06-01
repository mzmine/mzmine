package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.annotationpriority.AnnotationSummary;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Reports the best preferred annotation across all member rows.
public final class CompoundAnnotationMatchCheck implements QualityCheck {

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.COMPOUND_ANNOTATION;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final List<FeatureListRow> involved = new ArrayList<>();

    final Comparator<@Nullable AnnotationSummary> sorter = row.getFeatureList()
        .getAnnotationSortConfig().sortOrder().getComparatorHighFirst();

    AnnotationSummary bestAnnotation = row.getCompoundMembers().stream().map(r -> {
      final FeatureAnnotation ann = r.row().getPreferredAnnotation();
      return ann == null ? null : AnnotationSummary.of(row, ann);
    }).filter(Objects::nonNull).min(sorter).orElse(null);

    FeatureListRow bestRow = null;
    for (final CompoundFeatureMember m : row.getCompoundMembers()) {
      final FeatureAnnotation preferredAnnotation = m.row().getPreferredAnnotation();
      if (preferredAnnotation != null) {
        involved.add(m.row());
        // Identity match: AnnotationSummary.of(row, ann) stores the original `ann` reference, so
        // bestAnnotation.annotation() is == the member's preferred annotation that won the sort.
        if (bestAnnotation != null && bestRow == null
            && preferredAnnotation == bestAnnotation.annotation()) {
          bestRow = m.row();
        }
      }
    }

    if (bestAnnotation == null || bestRow == null) {
      return new DefaultQualityCheckResult(QualityCheckType.COMPOUND_ANNOTATION,
          QualityCheckStatus.UNAVAILABLE, "No compound annotation", List.of(), List.of());
    }
    final FeatureAnnotation ann = bestAnnotation.annotation();
    final String summary =
        ann.getCompoundName() != null ? "%s\nscore %s".formatted(ann.getCompoundName(),
            ann.getScoreString()) : "Best score %s".formatted(ann.getScoreString());
    return new CompoundAnnotationMatchQualityResult(QualityCheckStatus.PASS, summary, ann, bestRow,
        involved, context.onEvent());
  }
}
