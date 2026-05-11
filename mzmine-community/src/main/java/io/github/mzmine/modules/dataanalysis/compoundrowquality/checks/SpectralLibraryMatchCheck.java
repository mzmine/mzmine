package io.github.mzmine.modules.dataanalysis.compoundrowquality.checks;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRow;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.DefaultQualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheck;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckContext;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckResult;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckStatus;
import io.github.mzmine.modules.dataanalysis.compoundrowquality.QualityCheckType;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Reports the best spectral library match across all member rows.
public final class SpectralLibraryMatchCheck implements QualityCheck {

  @Override
  public @NotNull QualityCheckType type() {
    return QualityCheckType.SPECTRAL_LIBRARY_MATCH;
  }

  @Override
  public @NotNull QualityCheckResult evaluate(@NotNull CompoundRow row,
      @NotNull QualityCheckContext context) {
    @Nullable Float bestScore = null;
    @Nullable String bestName = null;
    final List<FeatureListRow> involved = new ArrayList<>();

    for (final CompoundFeatureMember m : row.getCompoundMembers()) {
      final List<SpectralDBAnnotation> matches = m.row().getSpectralLibraryMatches();
      if (matches.isEmpty()) {
        continue;
      }
      involved.add(m.row());
      for (final SpectralDBAnnotation match : matches) {
        final Float score = match.getScore();
        if (score == null) {
          continue;
        }
        if (bestScore == null || score > bestScore) {
          bestScore = score;
          bestName = match.getCompoundName();
        }
      }
    }

    if (bestScore == null) {
      return new DefaultQualityCheckResult(QualityCheckType.SPECTRAL_LIBRARY_MATCH,
          QualityCheckStatus.UNAVAILABLE, "No spectral library matches", List.of(), List.of());
    }
    final String summary =
        bestName != null ? "%s — score %.3f".formatted(bestName, bestScore) : "Best score %.3f".formatted(
            bestScore);
    return new DefaultQualityCheckResult(QualityCheckType.SPECTRAL_LIBRARY_MATCH, QualityCheckStatus.PASS,
        summary, List.of(), involved);
  }
}
