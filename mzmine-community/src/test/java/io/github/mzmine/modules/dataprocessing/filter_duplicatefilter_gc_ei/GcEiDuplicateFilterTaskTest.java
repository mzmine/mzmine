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

package io.github.mzmine.modules.dataprocessing.filter_duplicatefilter_gc_ei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.PseudoSpectrumType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.compoundlist.CompoundFeatureMember;
import io.github.mzmine.datamodel.features.compoundlist.CompoundList;
import io.github.mzmine.datamodel.features.compoundlist.CompoundMemberRole;
import io.github.mzmine.datamodel.features.compoundlist.ModularCompoundRow;
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.FeatureSmoothingOptions;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolayParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import testutils.MZmineTestUtil;

/**
 * Tests the GC-EI duplicate filter on synthetic data. Compound X with the fragments
 * {@link #MZ_A}, {@link #MZ_B}, and {@link #MZ_C} is present in all files and was split into two
 * rows by the alignment.
 */
@TestInstance(Lifecycle.PER_CLASS)
class GcEiDuplicateFilterTaskTest {

  private static final double MZ_A = 100.05;
  private static final double MZ_B = 150.07;
  private static final double MZ_C = 200.09;
  private static final double[] SPECTRUM_X = {MZ_A, MZ_B, MZ_C};
  // a different compound that shares the fragment MZ_A
  private static final double[] SPECTRUM_Y = {MZ_A, 301.2, 355.3};

  private static final float RT = 1.0f;
  private static final int NUM_SCANS = 200;
  private static final float SCAN_INTERVAL = 0.01f;
  private static final double PEAK_SIGMA = 0.02;
  private static final double HEIGHT = 1E5;

  private static final MZTolerance MZ_TOLERANCE = new MZTolerance(0.005, 10);

  private MZmineProjectImpl project;
  private RawDataFile fileA;
  private RawDataFile fileB;
  private RawDataFile fileC;
  private ModularFeatureList flist;

  @BeforeAll
  void startCore() {
    MZmineTestUtil.startMzmineCore();
  }

  @BeforeEach
  void init() {
    project = new MZmineProjectImpl();
    fileA = createFile("a");
    fileB = createFile("b");
    fileC = createFile("c");
    flist = new ModularFeatureList("aligned", null, fileA, fileB, fileC);
    DataTypeUtils.addDefaultChromatographicTypeColumns(flist);
    for (final RawDataFile file : List.of(fileA, fileB, fileC)) {
      flist.setSelectedScans(file, file.getScans());
    }
  }

  @Test
  void mergesDuplicateWithDifferentQuantifier() {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_B, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));

    run(GcEiDuplicateMzCheck.QUANTIFIER_IN_BOTH_SPECTRA, GcEiDuplicateHandling.MERGE);

    assertEquals(1, flist.getNumberOfRows());
    final FeatureListRow row = flist.getRow(0);
    assertEquals(1, row.getID(), "the row with more detected features is kept");
    assertEquals(3, row.getNumberOfFeatures());
    final Feature merged = row.getFeature(fileC);
    assertNotNull(merged);
    assertEquals(FeatureStatus.DETECTED, merged.getFeatureStatus());
    assertEquals(MZ_A, merged.getMZ(), 0.001, "re-extracted at the quantifier of the kept row");
    assertEquals(HEIGHT, merged.getHeight(), HEIGHT * 0.01);
  }

  @Test
  void detectedFeatureReplacesGapFilledFeature() {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED, fileC,
            FeatureStatus.ESTIMATED));
    addRow(2, MZ_A, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));

    run(GcEiDuplicateMzCheck.SAME_QUANTIFIER, GcEiDuplicateHandling.MERGE);

    assertEquals(1, flist.getNumberOfRows());
    assertEquals(FeatureStatus.DETECTED, flist.getRow(0).getFeature(fileC).getFeatureStatus());
  }

  @Test
  void removeModeDoesNotTransferFeatures() {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_B, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));

    run(GcEiDuplicateMzCheck.QUANTIFIER_IN_BOTH_SPECTRA, GcEiDuplicateHandling.REMOVE);

    assertEquals(1, flist.getNumberOfRows());
    assertEquals(2, flist.getRow(0).getNumberOfFeatures());
    assertNull(flist.getRow(0).getFeature(fileC));
  }

  @Test
  void keepsDifferentCompoundsWithSharedQuantifier() {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_A, RT, SPECTRUM_Y, features(fileC, FeatureStatus.DETECTED));

    run(GcEiDuplicateMzCheck.SAME_QUANTIFIER, GcEiDuplicateHandling.MERGE);

    assertEquals(2, flist.getNumberOfRows(), "spectra are not similar");
  }

  @Test
  void sameQuantifierCheckKeepsRowsWithDifferentQuantifiers() {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_B, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));

    run(GcEiDuplicateMzCheck.SAME_QUANTIFIER, GcEiDuplicateHandling.MERGE);

    assertEquals(2, flist.getNumberOfRows());
  }

  @Test
  void keepsRowsOutsideRtTolerance() {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_A, RT + 0.2f, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));

    run(GcEiDuplicateMzCheck.SAME_QUANTIFIER, GcEiDuplicateHandling.MERGE);

    assertEquals(2, flist.getNumberOfRows());
  }

  @Test
  void reExtractedFeatureIsSmoothedLikeTheProcessingHistory() {
    flist.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(SmoothingModule.class, createSmoothingParameters(),
            Instant.now()));
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_B, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));

    run(GcEiDuplicateMzCheck.QUANTIFIER_IN_BOTH_SPECTRA, GcEiDuplicateHandling.MERGE);

    final Feature merged = flist.getRow(0).getFeature(fileC);
    assertNotNull(merged);
    assertEquals(MZ_A, merged.getMZ(), 0.001);
    // smoothing lowers the apex of the synthetic gaussian peak slightly
    final float height = merged.getHeight();
    assertTrue(height < HEIGHT * 0.999 && height > HEIGHT * 0.9, "unexpected height " + height);
  }

  @Test
  void duplicatesAreNotChainedOverSeveralRows() {
    // row 2 is within the RT tolerance of rows 1 and 3, but rows 1 and 3 are not
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_A, RT + 0.03f, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));
    addRow(3, MZ_A, RT + 0.06f, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));

    run(GcEiDuplicateMzCheck.SAME_QUANTIFIER, GcEiDuplicateHandling.REMOVE);

    assertEquals(List.of(1, 3), flist.getRows().stream().map(FeatureListRow::getID).toList(),
        "row 2 is a duplicate of the best row 1, row 3 must not be removed via row 2");
  }

  @Test
  void mergesMemberRowsOfDuplicateCompounds() {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_B, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));
    // member ions: row 4 is the same ion as row 3, row 5 only exists in the duplicate compound
    addRow(3, MZ_C, RT, SPECTRUM_X, features(fileA, FeatureStatus.DETECTED));
    addRow(4, MZ_C, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));
    addRow(5, 301.2, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));
    setCompounds(List.of(1, 3), List.of(2, 4, 5));

    run(GcEiDuplicateMzCheck.QUANTIFIER_IN_BOTH_SPECTRA, GcEiDuplicateHandling.MERGE,
        OriginalFeatureListOption.KEEP);

    final ModularFeatureList filtered = project.getCurrentFeatureLists().stream()
        .map(ModularFeatureList.class::cast).filter(f -> f != flist).findFirst().orElseThrow();
    assertEquals(List.of(1, 3, 5),
        filtered.getRows().stream().map(FeatureListRow::getID).sorted().toList());
    assertEquals(3, filtered.findRowByID(1).getNumberOfFeatures());
    final Feature mergedMember = filtered.findRowByID(3).getFeature(fileC);
    assertNotNull(mergedMember, "the member of the duplicate is merged into the same ion");
    assertEquals(MZ_C, mergedMember.getMZ(), 0.001);

    assertTrue(filtered.hasCompoundList(), "the compound list is copied");
    final List<ModularCompoundRow> compounds = filtered.getCompoundList().getRows();
    assertEquals(1, compounds.size(), "the duplicate compound is merged");
    assertEquals(filtered.findRowByID(1), compounds.getFirst().getPreferredRow());
    assertEquals(List.of(1, 3, 5),
        compounds.getFirst().getMemberRows().stream().map(FeatureListRow::getID).sorted().toList());
  }

  @Test
  void mergedMemberAtKeptQuantifierKeepsPseudoSpectrumOfDuplicate() {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_B, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));
    // members have no pseudo spectrum. Row 4 is the ion of the kept quantifier in the duplicate
    addRow(3, MZ_C, RT, null, features(fileA, FeatureStatus.DETECTED));
    addRow(4, MZ_A, RT, null, features(fileC, FeatureStatus.DETECTED));
    setCompounds(List.of(1, 3), List.of(2, 4));
    final List<Scan> duplicateSpectra = flist.findRowByID(2).getFeature(fileC)
        .getAllMS2FragmentScans();

    run(GcEiDuplicateMzCheck.QUANTIFIER_IN_BOTH_SPECTRA, GcEiDuplicateHandling.MERGE);

    assertEquals(List.of(1, 3),
        flist.getRows().stream().map(FeatureListRow::getID).sorted().toList());
    final Feature merged = flist.findRowByID(1).getFeature(fileC);
    assertNotNull(merged);
    assertEquals(MZ_A, merged.getMZ(), 0.001);
    assertEquals(duplicateSpectra, merged.getAllMS2FragmentScans(),
        "the representative keeps the pseudo spectrum of the duplicate representative");
  }

  @Test
  void removeModeRemovesAllMemberRowsOfDuplicateCompounds() {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_B, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));
    addRow(3, MZ_C, RT, SPECTRUM_X, features(fileA, FeatureStatus.DETECTED));
    addRow(4, 301.2, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));
    setCompounds(List.of(1, 3), List.of(2, 4));

    run(GcEiDuplicateMzCheck.QUANTIFIER_IN_BOTH_SPECTRA, GcEiDuplicateHandling.REMOVE);

    assertEquals(List.of(1, 3),
        flist.getRows().stream().map(FeatureListRow::getID).sorted().toList());
    assertEquals(1, flist.getCompoundList().getRows().size());
  }

  @ParameterizedTest
  @EnumSource(GcEiDuplicateHandling.class)
  void keepsMemberRowsSharedWithTheKeptCompound(@NotNull final GcEiDuplicateHandling handling) {
    addRow(1, MZ_A, RT, SPECTRUM_X,
        features(fileA, FeatureStatus.DETECTED, fileB, FeatureStatus.DETECTED));
    addRow(2, MZ_B, RT, SPECTRUM_X, features(fileC, FeatureStatus.DETECTED));
    addRow(3, MZ_C, RT, SPECTRUM_X, features(fileA, FeatureStatus.DETECTED));
    // row 3 is a member of both compounds
    setCompounds(List.of(1, 3), List.of(2, 3));

    run(GcEiDuplicateMzCheck.QUANTIFIER_IN_BOTH_SPECTRA, handling);

    assertEquals(List.of(1, 3),
        flist.getRows().stream().map(FeatureListRow::getID).sorted().toList());
    assertEquals(List.of(1, 3),
        flist.getCompoundList().getRows().getFirst().getMemberRows().stream()
            .map(FeatureListRow::getID).sorted().toList());
  }

  /**
   * @param memberIdsPerCompound row IDs of each compound, the first row is the representative
   */
  @SafeVarargs
  private void setCompounds(@NotNull final List<Integer>... memberIdsPerCompound) {
    final CompoundList compoundList = new CompoundList(flist, null, memberIdsPerCompound.length);
    final List<ModularCompoundRow> compounds = new ArrayList<>();
    for (int c = 0; c < memberIdsPerCompound.length; c++) {
      final List<CompoundFeatureMember> members = new ArrayList<>();
      for (final int id : memberIdsPerCompound[c]) {
        members.add(new CompoundFeatureMember(flist.findRowByID(id),
            members.isEmpty() ? CompoundMemberRole.REPRESENTATIVE : CompoundMemberRole.CORRELATED,
            1f));
      }
      compounds.add(
          new ModularCompoundRow(compoundList, c + 1, members.getFirst().row(), members, 1f, null));
    }
    compoundList.setRows(compounds);
    flist.setCompoundList(compoundList);
  }

  private void run(@NotNull final GcEiDuplicateMzCheck mzCheck,
      @NotNull final GcEiDuplicateHandling handling) {
    run(mzCheck, handling, OriginalFeatureListOption.PROCESS_IN_PLACE);
  }

  private void run(@NotNull final GcEiDuplicateMzCheck mzCheck,
      @NotNull final GcEiDuplicateHandling handling,
      @NotNull final OriginalFeatureListOption handleOriginal) {
    project.addFeatureList(flist);
    final ParameterSet params = GcEiDuplicateFilterParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS),
        new RTTolerance(0.04f, Unit.MINUTES), MZ_TOLERANCE, mzCheck,
        GcEiDuplicateFilterParameters.DEFAULT_MIN_COSINE, handling, "dup", handleOriginal);
    final GcEiDuplicateFilterTask task = new GcEiDuplicateFilterTask(project, flist, null,
        Instant.now(), params, GcEiDuplicateFilterModule.class);
    task.run();
    assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
  }

  private static @NotNull Map<RawDataFile, FeatureStatus> features(
      @NotNull final Object... fileStatusPairs) {
    final Map<RawDataFile, FeatureStatus> map = new LinkedHashMap<>();
    for (int i = 0; i < fileStatusPairs.length; i += 2) {
      map.put((RawDataFile) fileStatusPairs[i], (FeatureStatus) fileStatusPairs[i + 1]);
    }
    return map;
  }

  /**
   * Adds a row with one feature per file at the quantifier m/z. Each feature carries a pseudo
   * spectrum with the given signals.
   *
   * @param spectrumMzs signals of the pseudo spectrum or null for compound member rows without a
   *                    pseudo spectrum
   */
  private void addRow(final int id, final double quantifierMz, final float rt,
      final double @Nullable [] spectrumMzs, @NotNull final Map<RawDataFile, FeatureStatus> files) {
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, id);
    final double[] spectrumIntensities =
        spectrumMzs == null ? null : new double[spectrumMzs.length];
    for (int i = 0; spectrumMzs != null && i < spectrumMzs.length; i++) {
      spectrumIntensities[i] = HEIGHT * (1 - 0.2 * i);
    }
    for (final Map.Entry<RawDataFile, FeatureStatus> entry : files.entrySet()) {
      final RawDataFile file = entry.getKey();
      final List<Scan> scans = new ArrayList<>(file.getScans());
      final double[] mzs = new double[scans.size()];
      final double[] intensities = new double[scans.size()];
      for (int i = 0; i < scans.size(); i++) {
        mzs[i] = quantifierMz;
        intensities[i] = gaussian(scans.get(i).getRetentionTime(), rt);
      }
      final ModularFeature feature = new ModularFeature(flist, file,
          new SimpleIonTimeSeries(null, mzs, intensities, scans), entry.getValue());
      if (spectrumMzs != null) {
        feature.setAllMS2FragmentScans(List.of(
            new SimplePseudoSpectrum(file, 1, rt, null, spectrumMzs, spectrumIntensities,
                PolarityType.POSITIVE, "Pseudo spectrum", PseudoSpectrumType.GC_EI)));
      }
      row.addFeature(file, feature);
    }
    flist.addRow(row);
  }

  /**
   * All files contain compound X at {@link #RT}.
   */
  private static @NotNull RawDataFile createFile(@NotNull final String name) {
    final RawDataFile file = new RawDataFileImpl(name, null, null);
    for (int i = 0; i < NUM_SCANS; i++) {
      final float rt = i * SCAN_INTERVAL;
      final TreeMap<Double, Double> signals = new TreeMap<>();
      for (int s = 0; s < SPECTRUM_X.length; s++) {
        final double intensity = gaussian(rt, RT) * (1 - 0.2 * s);
        if (intensity > 10) {
          signals.put(SPECTRUM_X[s], intensity);
        }
      }
      final SimpleScan scan = new SimpleScan(file, i, 1, rt, null,
          signals.keySet().stream().mapToDouble(Double::doubleValue).toArray(),
          signals.values().stream().mapToDouble(Double::doubleValue).toArray(),
          MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(50d, 400d));
      scan.addMassList(new ScanPointerMassList(scan));
      file.addScan(scan);
    }
    return file;
  }

  /**
   * Retention time smoothing like in the processing wizard
   */
  private static @NotNull ParameterSet createSmoothingParameters() {
    final ParameterSet params = new SmoothingParameters().cloneParameterSet();
    params.getParameter(SmoothingParameters.smoothingAlgorithm)
        .setValue(FeatureSmoothingOptions.SAVITZKY_GOLAY);
    final ParameterSet sgParams = params.getEmbeddedParameterValue(
        SmoothingParameters.smoothingAlgorithm);
    sgParams.setParameter(SavitzkyGolayParameters.rtSmoothing, true);
    sgParams.getParameter(SavitzkyGolayParameters.rtSmoothing).getEmbeddedParameter().setValue(5);
    sgParams.setParameter(SavitzkyGolayParameters.mobilitySmoothing, false);
    return params;
  }

  private static double gaussian(final float rt, final float apexRt) {
    final double delta = rt - apexRt;
    return HEIGHT * Math.exp(-delta * delta / (2 * PEAK_SIGMA * PEAK_SIGMA));
  }
}
