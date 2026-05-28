package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

/// Verifies that all member rows' {@link FeatureListRow#getPreferredAnnotation} preferred
/// annotations agree on the underlying compound. Compares the InChIKey first block (connectivity
/// only), the molecular formula, and — when the structures do not match — the mean pairwise
/// Tanimoto similarity between CDK molecular fingerprints.
public final class AnnotationAgreementCheck implements QualityCheck {

  private static final Logger logger = Logger.getLogger(AnnotationAgreementCheck.class.getName());

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.ANNOTATION_AGREEMENT;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    final List<FeatureListRow> involved = new ArrayList<>();
    final List<FeatureAnnotation> annotations = new ArrayList<>();
    for (final CompoundFeatureMember m : row.getCompoundMembers()) {
      final FeatureAnnotation pref = m.row().getPreferredAnnotation();
      if (pref == null) {
        continue;
      }
      involved.add(m.row());
      annotations.add(pref);
    }

    if (annotations.isEmpty()) {
      return new DefaultQualityCheckResult(QualityCheckType.ANNOTATION_AGREEMENT,
          QualityCheckStatus.UNAVAILABLE, "No annotations to compare", List.of(), involved);
    }

    // Collapse to "Single annotation" when every annotation maps to the same identity key
    // (InChIKey first block, or molecular formula when the key is missing).
    if (CompoundAnnotationUtils.countUniqueAnnotations(annotations) <= 1) {
      return AnnotationAgreementQualityResult.singleAnnotation(involved);
    }

    final boolean structuresEqual = CompoundAnnotationUtils.allShareInchiKeyFirstBlock(annotations);
    final boolean formulasEqual = CompoundAnnotationUtils.allShareFormula(annotations);
    // Skip the Tanimoto computation when structures already match — the score would be redundant
    // and we hide that row in the UI anyway.
    final Double similarity =
        structuresEqual ? null : CompoundAnnotationUtils.meanPairwiseTanimoto(annotations);

    final QualityCheckStatus status =
        (structuresEqual && formulasEqual) ? QualityCheckStatus.PASS : QualityCheckStatus.FAIL;

    return new AnnotationAgreementQualityResult(status, structuresEqual, formulasEqual, similarity,
        CompoundAnnotationUtils.uniqueStructures(annotations), involved);
  }

}
