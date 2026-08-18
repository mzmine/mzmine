/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 * SPDX-License-Identifier: MIT
 */
package io.github.mzmine.modules.dataprocessing.id_nist;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/** Queries NIST annotations already stored on feature-list rows by batch processing. */
public final class NistMatchUtils {

  private static final double CHART_PEAK_RT_TOLERANCE_MINUTES = 0.03d;
  private static final Pattern REVERSE_MATCH_FACTOR_PATTERN = Pattern.compile(
      "reverse\\s+MF:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

  private NistMatchUtils() {
  }

  public static List<NistMatch> findMatches(@Nullable RawDataFile rawDataFile) {
    return ProjectService.getProjectManager().getCurrentProject().getCurrentFeatureLists().stream()
        .flatMap(featureList -> featureList.getRows().stream())
        .flatMap(row -> row.getSpectralLibraryMatches().stream()
            .filter(NistMatchUtils::isNistMatch)
            .filter(match -> rawDataFile == null || belongsTo(match, row, rawDataFile))
            .map(match -> new NistMatch(row, match, rawDataFile)))
        .sorted(Comparator.comparing(NistMatch::sampleName)
            .thenComparingDouble(NistMatch::retentionTime)
            .thenComparing(Comparator.comparingInt(NistMatch::matchFactor).reversed()))
        .toList();
  }

  /**
   * Returns the highest-scoring NIST hit for each chromatographic peak. Retention time is the
   * primary peak identity so separate peaks remain separate even when they share a base ion or
   * feature-list row.
   */
  public static List<NistMatch> findBestMatches(@Nullable RawDataFile rawDataFile) {
    final List<NistMatch> bestByRetentionTime = new ArrayList<>();
    for (NistMatch candidate : findMatches(rawDataFile)) {
      int samePeakIndex = -1;
      for (int index = 0; index < bestByRetentionTime.size(); index++) {
        NistMatch existing = bestByRetentionTime.get(index);
        if (existing.sampleName().equals(candidate.sampleName())
            && isSameChartPeakRetentionTime(existing.retentionTime(), candidate.retentionTime())) {
          samePeakIndex = index;
          break;
        }
      }
      if (samePeakIndex < 0) {
        bestByRetentionTime.add(candidate);
      } else if (candidate.matchFactor() > bestByRetentionTime.get(samePeakIndex).matchFactor()) {
        bestByRetentionTime.set(samePeakIndex, candidate);
      }
    }
    return bestByRetentionTime.stream().sorted(Comparator.comparingDouble(NistMatch::retentionTime))
        .toList();
  }

  public static boolean isSameChartPeakRetentionTime(double firstRt, double secondRt) {
    return Math.abs(firstRt - secondRt) <= CHART_PEAK_RT_TOLERANCE_MINUTES;
  }

  public static boolean isNistMatch(SpectralDBAnnotation match) {
    return match.getEntry().getField(DBEntryField.SOFTWARE).map(Object::toString)
        .map(value -> value.startsWith("NIST MSPepSearch")).orElse(false);
  }

  private static boolean belongsTo(SpectralDBAnnotation match, FeatureListRow row,
      RawDataFile rawDataFile) {
    // Aligned rows carry one identification across all of their sample-specific features. The
    // original query scan identifies where the annotation came from, not the only sample where it
    // should be displayed.
    if (row.getFeature(rawDataFile) != null) {
      return true;
    }
    if (match.getQueryScan() != null && match.getQueryScan().getDataFile() != null) {
      return match.getQueryScan().getDataFile().equals(rawDataFile);
    }
    return row.getRawDataFiles().contains(rawDataFile);
  }

  public record NistMatch(FeatureListRow row, SpectralDBAnnotation match,
                          @Nullable RawDataFile displayRawDataFile) {

    public @Nullable RawDataFile rawDataFile() {
      if (displayRawDataFile != null) {
        return displayRawDataFile;
      }
      if (match.getQueryScan() != null && match.getQueryScan().getDataFile() != null) {
        return match.getQueryScan().getDataFile();
      }
      return row.getRawDataFiles().isEmpty() ? null : row.getRawDataFiles().getFirst();
    }

    public String sampleName() {
      final RawDataFile rawDataFile = rawDataFile();
      return rawDataFile == null ? "" : rawDataFile.getName();
    }

    public String compoundName() {
      return NistCommonNameResolver.preferredDisplayName(row, match);
    }

    public int matchFactor() {
      Float score = match.getScore();
      return score == null ? 0 : Math.round(score * 1000f);
    }

    public int forwardMatchFactor() {
      return matchFactor();
    }

    public int reverseMatchFactor() {
      return match.getEntry().getField(DBEntryField.COMMENT).map(Object::toString)
          .map(NistMatchUtils::parseReverseMatchFactor).orElse(0);
    }

    public double retentionTime() {
      if (displayRawDataFile != null) {
        final var feature = row.getFeature(displayRawDataFile);
        if (feature != null && feature.getRT() != null) {
          return feature.getRT();
        }
      }
      if (match.getQueryScan() != null) {
        return match.getQueryScan().getRetentionTime();
      }
      Float rt = row.getAverageRT();
      return rt == null ? 0d : rt;
    }
  }

  static int parseReverseMatchFactor(String comment) {
    if (comment == null) {
      return 0;
    }
    final var matcher = REVERSE_MATCH_FACTOR_PATTERN.matcher(comment);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
  }
}
