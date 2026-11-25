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

package io.github.mzmine.datamodel.features.annotationpriority;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.annotations.AnalogSpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RIType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.scores.SiriusCsiScoreType;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.ILipidAnnotation;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.matched_levels.MatchedLipid;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidClasses;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidFragment;
import io.github.mzmine.modules.dataprocessing.id_lipidid.utils.LipidFactory;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.util.FeatureListTestUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntryFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class AnnotationSummaryTest {

  private static final double MZ = 760.585;
  private static final float RT = 5f;
  private static final float RI = 1200f;
  private static final float CCS = 250f;

  private ModularFeatureListRow row;

  /**
   * Creates a row with measured m/z, RT, RI, CCS, and isotope evidence.
   */
  @BeforeEach
  void setUp() {
    final var file = new RawDataFileImpl("sample", null, null);
    final var featureList = new ModularFeatureList("Annotations", null, file);
    row = FeatureListTestUtils.addRow(featureList, 1, List.of(file), List.of(100f), MZ, RT);
    row.getFeature(file).setIsotopePattern(isotopePattern(MZ, IsotopePatternStatus.DETECTED));
    row.set(RIType.class, RI);
    row.set(CCSType.class, CCS);
  }

  /**
   * Checks how different evidence affects each confidence scale.
   *
   * @param description test case name
   * @param annotation  annotation to evaluate
   * @param rank        expected mzmine annotation type rank, lower is more confident
   * @param msi         expected MSI (Sumner) confidence level
   * @param exposomics  expected exposomics confidence level
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("confidenceCases")
  void derivesConfidenceLevels(@NotNull final String description,
      @Nullable final FeatureAnnotation annotation, final int rank,
      @NotNull final MsiAnnotationLevel msi, @NotNull final ExposomicsAnnotationLevel exposomics) {
    final var summary = AnnotationSummary.of(row, annotation);
    Assertions.assertAll(
        () -> Assertions.assertEquals(rank, summary.mzmineAnnotationTypeRank(), "mzmine rank"),
        () -> Assertions.assertEquals(msi, summary.deriveMsiLevel(), "MSI (Sumner) level"),
        () -> Assertions.assertEquals(exposomics, summary.deriveExposomicsLevel(),
            "Exposomics level"));
  }

  /**
   * Checks the confidence order and the combined-score tie-breaker in each sort mode.
   */
  @Test
  void sortsSummariesByCurrentConfidenceRules() {
    final var libraryWithMzAndRt = AnnotationSummary.of(row, spectral(MZ, RT, 0.6, false));
    final var libraryWithRt = AnnotationSummary.of(row, spectral(null, RT, 0.9, false));
    final var libraryWithoutRetention = AnnotationSummary.of(row, spectral(MZ, null, 0.95, false));
    final var confirmedLipid = AnnotationSummary.of(row, lipid(false, true, 0.7));
    final var databaseWithRt = AnnotationSummary.of(row, database(RT, null, null, null));
    final var databaseWithCsi = AnnotationSummary.of(row, database(null, null, 0.01f, null));
    final var analog = AnnotationSummary.of(row, spectral(MZ, RT, 0.99, true));
    final var unannotated = AnnotationSummary.of(row);
    final var input = List.of(analog, libraryWithRt, unannotated, confirmedLipid,
        libraryWithoutRetention, databaseWithRt, libraryWithMzAndRt, databaseWithCsi);

    final var mzmineOrder = List.of(libraryWithMzAndRt, libraryWithRt, confirmedLipid,
        libraryWithoutRetention, databaseWithCsi, databaseWithRt, analog, unannotated);
    final var msiOrder = List.of(libraryWithMzAndRt, libraryWithRt, libraryWithoutRetention,
        databaseWithRt, confirmedLipid, databaseWithCsi, analog, unannotated);
    final var exposomicsOrder = List.of(libraryWithMzAndRt, libraryWithRt, libraryWithoutRetention,
        confirmedLipid, databaseWithCsi, analog, databaseWithRt, unannotated);

    Assertions.assertAll(() -> Assertions.assertEquals(mzmineOrder,
            input.stream().sorted(AnnotationSummaryOrder.MZMINE.getComparatorHighFirst()).toList(),
            "mzmine high first"),
        () -> Assertions.assertEquals(mzmineOrder.reversed(), input.stream().sorted().toList(),
            "Natural order"), () -> Assertions.assertEquals(msiOrder,
            input.stream().sorted(AnnotationSummaryOrder.MSI.getComparatorHighFirst()).toList(),
            "MSI high first"), () -> Assertions.assertEquals(msiOrder.reversed(),
            input.stream().sorted(AnnotationSummaryOrder.MSI.getComparatorLowFirst()).toList(),
            "MSI low first"), () -> Assertions.assertEquals(exposomicsOrder,
            input.stream().sorted(AnnotationSummaryOrder.EXPOSOMICS.getComparatorHighFirst())
                .toList(), "Exposomics high first"),
        () -> Assertions.assertEquals(exposomicsOrder.reversed(),
            input.stream().sorted(AnnotationSummaryOrder.EXPOSOMICS.getComparatorLowFirst())
                .toList(), "Exposomics low first"));
  }

  /**
   * Checks that the score tie-breaker sorts from low to high confidence: worse annotation type rank
   * first, then lower combined score first.
   */
  @Test
  void scoresLowToHighSortsByReversedRankThenCombinedScore() {
    final var unannotated = AnnotationSummary.of(row);
    // analog has the higher combined score but the worse type rank
    final var analog = AnnotationSummary.of(row, spectral(MZ, RT, 0.99, true));
    final var libraryWithoutRetention = AnnotationSummary.of(row, spectral(MZ, null, 0.95, false));
    // same type rank, so the combined score decides
    final var libraryWithRt = AnnotationSummary.of(row, spectral(null, RT, 0.9, false));
    final var libraryWithMzAndRt = AnnotationSummary.of(row, spectral(MZ, RT, 0.6, false));

    final var expected = List.of(unannotated, analog, libraryWithoutRetention, libraryWithRt,
        libraryWithMzAndRt);
    final var input = List.of(libraryWithMzAndRt, unannotated, libraryWithRt, analog,
        libraryWithoutRetention);

    Assertions.assertTrue(analog.combinedScore() > libraryWithoutRetention.combinedScore(),
        "Precondition: rank must win over combined score");
    Assertions.assertTrue(libraryWithMzAndRt.combinedScore() > libraryWithRt.combinedScore(),
        "Precondition: combined score breaks the rank tie");
    Assertions.assertEquals(expected,
        input.stream().sorted(AnnotationSummaryOrder.SCORES_LOW_TO_HIGH).toList());
  }

  /**
   * Checks that reference retention metadata requires a measured value on the row.
   */
  @Test
  void referenceRetentionWithoutRowMeasurementDoesNotImproveConfidence() {
    final var rowWithoutRetention = new ModularFeatureListRow(row.getFeatureList(), 2);
    final var summary = AnnotationSummary.of(rowWithoutRetention, spectral(MZ, RT, 0.9, false));

    Assertions.assertAll(() -> Assertions.assertEquals(3, summary.mzmineAnnotationTypeRank()),
        () -> Assertions.assertEquals(MsiAnnotationLevel.LEVEL_2, summary.deriveMsiLevel()),
        () -> Assertions.assertEquals(ExposomicsAnnotationLevel.LEVEL_2a,
            summary.deriveExposomicsLevel()));
  }

  /**
   * @return annotations with their expected mzmine rank, MSI level, and exposomics level
   */
  private static @NotNull Stream<Arguments> confidenceCases() {
    final var matchingRi = spectral(MZ, null, 0.9, false);
    matchingRi.set(RIType.class, RI);
    final var matchingIsotopes = isotopePattern(MZ, IsotopePatternStatus.PREDICTED);
    final var mismatchingIsotopes = isotopePattern(MZ + 0.1, IsotopePatternStatus.PREDICTED);
    // MS1-only path in LipidAnnotationUtils creates lipids with null fragments and a zero score
    final var ms1OnlyLipid = new MatchedLipid(
        new LipidFactory().buildSpeciesLevelLipid(LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES, 34, 1,
            0), MZ, IonizationType.POSITIVE_HYDROGEN, null, 0d);

    // mzmine rank: lower is more confident, 1 = library match with RT/RI ... 10 = unannotated
    return Stream.of(Arguments.of("Unannotated row", null, 10, MsiAnnotationLevel.LEVEL_4,
            ExposomicsAnnotationLevel.LEVEL_5),
        Arguments.of("Library match without retention", spectral(MZ, null, 0.9, false), 3,
            MsiAnnotationLevel.LEVEL_2, ExposomicsAnnotationLevel.LEVEL_2a),
        Arguments.of("Library match with matching RT", spectral(MZ, RT, 0.9, false), 1,
            MsiAnnotationLevel.LEVEL_2, ExposomicsAnnotationLevel.LEVEL_1),
        Arguments.of("Library match with matching numeric RI", matchingRi, 1,
            MsiAnnotationLevel.LEVEL_2, ExposomicsAnnotationLevel.LEVEL_1),
        Arguments.of("Library match just inside the RT tolerance",
            spectral(MZ, RT + 0.299f, 0.9, false), 3, MsiAnnotationLevel.LEVEL_2,
            ExposomicsAnnotationLevel.LEVEL_2a),
        Arguments.of("Library match outside the RT tolerance", spectral(MZ, RT + 1f, 0.9, false), 3,
            MsiAnnotationLevel.LEVEL_2, ExposomicsAnnotationLevel.LEVEL_2a),
        Arguments.of("Analog library match even with matching RT", spectral(MZ, RT, 0.99, true), 6,
            MsiAnnotationLevel.LEVEL_3, ExposomicsAnnotationLevel.LEVEL_3),
        Arguments.of("Molecular-species lipid with fragments", lipid(true, true, 0.8), 2,
            MsiAnnotationLevel.LEVEL_2, ExposomicsAnnotationLevel.LEVEL_2a),
        Arguments.of("Species-level lipid with fragments", lipid(false, true, 0.8), 2,
            MsiAnnotationLevel.LEVEL_3, ExposomicsAnnotationLevel.LEVEL_2b),
        Arguments.of("Lipid with a positive MS/MS score and no fragments", lipid(true, false, 0.8),
            4, MsiAnnotationLevel.LEVEL_2, ExposomicsAnnotationLevel.LEVEL_2a),
        Arguments.of("Unconfirmed lipid with zero MS/MS score", lipid(false, false, 0d), 4,
            MsiAnnotationLevel.LEVEL_4, ExposomicsAnnotationLevel.LEVEL_5),
        Arguments.of("Lipid without MS/MS evidence", lipid(false, false, null), 4,
            MsiAnnotationLevel.LEVEL_4, ExposomicsAnnotationLevel.LEVEL_5),
        Arguments.of("MS1-only lipid with null fragments", ms1OnlyLipid, 4,
            MsiAnnotationLevel.LEVEL_4, ExposomicsAnnotationLevel.LEVEL_5),
        Arguments.of("Database match with m/z and CCS only", database(null, null, null, null), 4,
            MsiAnnotationLevel.LEVEL_4, ExposomicsAnnotationLevel.LEVEL_5),
        Arguments.of("Database match with matching RT", database(RT, null, null, null), 4,
            MsiAnnotationLevel.LEVEL_2, ExposomicsAnnotationLevel.LEVEL_5),
        Arguments.of("Database match with matching RI", database(null, RI, null, null), 4,
            MsiAnnotationLevel.LEVEL_2, ExposomicsAnnotationLevel.LEVEL_5),
        Arguments.of("Database match with mismatching RT and RI",
            database(RT + 1f, RI + 10f, null, null), 4, MsiAnnotationLevel.LEVEL_4,
            ExposomicsAnnotationLevel.LEVEL_5),
        Arguments.of("SIRIUS match with a CSI score", database(null, null, 0.8f, null), 4,
            MsiAnnotationLevel.LEVEL_3, ExposomicsAnnotationLevel.LEVEL_3),
        Arguments.of("SIRIUS match with retention and isotope evidence",
            database(RT, null, 0.8f, matchingIsotopes), 4, MsiAnnotationLevel.LEVEL_2,
            ExposomicsAnnotationLevel.LEVEL_3),
        Arguments.of("Database match with matching isotopes",
            database(null, null, null, matchingIsotopes), 4, MsiAnnotationLevel.LEVEL_4,
            ExposomicsAnnotationLevel.LEVEL_4),
        Arguments.of("Database match with mismatching isotopes",
            database(null, null, null, mismatchingIsotopes), 4, MsiAnnotationLevel.LEVEL_4,
            ExposomicsAnnotationLevel.LEVEL_5));
  }

  /**
   * @param mz         library precursor m/z, if available
   * @param rt         library retention time, if available
   * @param similarity spectral similarity score
   * @param analog     whether the match is to an analog
   * @return spectral library match with the supplied metadata
   */
  private static @NotNull SpectralDBAnnotation spectral(@Nullable final Double mz,
      @Nullable final Float rt, final double similarity, final boolean analog) {
    final Map<DBEntryField, Object> fields = new HashMap<>();
    if (mz != null) {
      fields.put(DBEntryField.PRECURSOR_MZ, mz);
    }
    if (rt != null) {
      fields.put(DBEntryField.RT, rt);
    }
    final var entry = SpectralLibraryEntryFactory.create(null, fields, new DataPoint[0]);
    final var score = new SpectralSimilarity("test", similarity, 3, 1d);
    return new SpectralDBAnnotation(entry, score, null, null,
        analog ? AnalogSpectralLibraryMatchesType.class : SpectralLibraryMatchesType.class);
  }

  /**
   * @param molecularSpecies whether both lipid chains are identified
   * @param hasFragments     whether a fragment was matched
   * @param ms2Score         lipid MS/MS score, if available
   * @return lipid match with the supplied evidence
   */
  private static @NotNull MatchedLipid lipid(final boolean molecularSpecies,
      final boolean hasFragments, @Nullable final Double ms2Score) {
    final var factory = new LipidFactory();
    final var lipidClass = LipidClasses.DIACYLGLYCEROPHOSPHOCHOLINES;
    final ILipidAnnotation lipid =
        molecularSpecies ? factory.buildMolecularSpeciesLevelLipid(lipidClass, new int[]{16, 18},
            new int[]{0, 1}, new int[]{0, 0})
            : factory.buildSpeciesLevelLipid(lipidClass, 34, 1, 0);
    final Set<LipidFragment> fragments =
        hasFragments ? Set.of(Mockito.mock(LipidFragment.class)) : Set.of();
    return new MatchedLipid(lipid, MZ, IonizationType.POSITIVE_HYDROGEN, fragments, ms2Score);
  }

  /**
   * @param rt       database retention time, if available
   * @param ri       database retention index, if available
   * @param csiScore SIRIUS CSI score, if available
   * @param pattern  predicted isotope pattern, if available
   * @return compound database match with matching m/z and CCS and the supplied metadata
   */
  private static @NotNull SimpleCompoundDBAnnotation database(@Nullable final Float rt,
      @Nullable final Float ri, @Nullable final Float csiScore,
      @Nullable final IsotopePattern pattern) {
    final var annotation = new SimpleCompoundDBAnnotation();
    annotation.put(PrecursorMZType.class, MZ);
    annotation.put(CCSType.class, CCS);
    annotation.putIfNotNull(RTType.class, rt);
    annotation.putIfNotNull(RIType.class, ri);
    annotation.putIfNotNull(SiriusCsiScoreType.class, csiScore);
    annotation.putIfNotNull(IsotopePatternType.class, pattern);
    return annotation;
  }

  /**
   * @param mz     m/z of the monoisotopic peak
   * @param status whether the pattern was detected or predicted
   * @return a two-peak isotope pattern
   */
  private static @NotNull IsotopePattern isotopePattern(final double mz,
      @NotNull final IsotopePatternStatus status) {
    return new SimpleIsotopePattern(new double[]{mz, mz + 1.00335}, new double[]{1d, 0.4}, 1,
        status, "test");
  }
}
