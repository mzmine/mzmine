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
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
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

  private void run(@NotNull final GcEiDuplicateMzCheck mzCheck,
      @NotNull final GcEiDuplicateHandling handling) {
    project.addFeatureList(flist);
    final ParameterSet params = GcEiDuplicateFilterParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS),
        new RTTolerance(0.04f, Unit.MINUTES), MZ_TOLERANCE, mzCheck,
        GcEiDuplicateFilterParameters.DEFAULT_MIN_COSINE, handling, "dup",
        OriginalFeatureListOption.PROCESS_IN_PLACE);
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
   */
  private void addRow(final int id, final double quantifierMz, final float rt,
      final double @NotNull [] spectrumMzs, @NotNull final Map<RawDataFile, FeatureStatus> files) {
    final ModularFeatureListRow row = new ModularFeatureListRow(flist, id);
    final double[] spectrumIntensities = new double[spectrumMzs.length];
    for (int i = 0; i < spectrumMzs.length; i++) {
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
      feature.setAllMS2FragmentScans(List.of(
          new SimplePseudoSpectrum(file, 1, rt, null, spectrumMzs, spectrumIntensities,
              PolarityType.POSITIVE, "Pseudo spectrum", PseudoSpectrumType.GC_EI)));
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

  private static double gaussian(final float rt, final float apexRt) {
    final double delta = rt - apexRt;
    return HEIGHT * Math.exp(-delta * delta / (2 * PEAK_SIGMA * PEAK_SIGMA));
  }
}
