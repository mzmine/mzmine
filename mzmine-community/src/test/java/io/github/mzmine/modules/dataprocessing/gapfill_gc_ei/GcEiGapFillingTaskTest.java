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

package io.github.mzmine.modules.dataprocessing.gapfill_gc_ei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import io.github.mzmine.datamodel.impl.SimplePseudoSpectrum;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.datamodel.impl.masslist.ScanPointerMassList;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.ResolvingDimension;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverModule;
import io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.minimumsearch.MinimumSearchFeatureResolverParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.FeatureSmoothingOptions;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingModule;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.SmoothingParameters;
import io.github.mzmine.modules.dataprocessing.featdet_smoothing.savitzkygolay.SavitzkyGolayParameters;
import io.github.mzmine.modules.dataprocessing.filter_groupms2.GroupMS2SubParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance.Unit;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataTypeUtils;
import java.time.Instant;
import java.util.ArrayList;
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
import testutils.MZmineTestUtil;

/**
 * Tests the GC-EI gap filling on synthetic GC-EI data. One compound with the fragments
 * {@link #QUANTIFIER_MZ}, {@link #SECOND_MZ}, and {@link #THIRD_MZ} is detected in the reference
 * file and needs to be re-detected in the other files.
 */
@TestInstance(Lifecycle.PER_CLASS)
class GcEiGapFillingTaskTest {

  private static final double QUANTIFIER_MZ = 100.05;
  private static final double SECOND_MZ = 150.07;
  private static final double THIRD_MZ = 200.09;
  // small fragment that is not part of the top 3 signals
  private static final double MINOR_MZ = 250.11;

  private static final float ROW_RT = 1.0f;
  private static final int NUM_SCANS = 200;
  private static final float SCAN_INTERVAL = 0.01f;
  private static final double PEAK_SIGMA = 0.02;
  private static final double HEIGHT = 1E5;

  private static final MZTolerance MZ_TOLERANCE = new MZTolerance(0.005, 10);

  private MZmineProjectImpl project;
  private RawDataFile reference;

  @BeforeAll
  void startCore() {
    MZmineTestUtil.startMzmineCore();
  }

  @BeforeEach
  void initProject() {
    project = new MZmineProjectImpl();
    reference = createFile("reference", Map.of(QUANTIFIER_MZ, ROW_RT, SECOND_MZ, ROW_RT, THIRD_MZ,
        ROW_RT, MINOR_MZ, ROW_RT));
  }

  @Test
  void fillsGapIfAllTopSignalsCoelute() {
    final RawDataFile sample = createFile("sample",
        Map.of(QUANTIFIER_MZ, ROW_RT + 0.01f, SECOND_MZ, ROW_RT + 0.01f, THIRD_MZ,
            ROW_RT + 0.01f));
    final ModularFeatureList flist = createAlignedList(false, reference, sample);

    runGapFilling(flist, OriginalFeatureListOption.PROCESS_IN_PLACE);

    final FeatureListRow row = flist.getRow(0);
    final Feature filled = row.getFeature(sample);
    assertNotNull(filled, "the gap should be filled");
    assertEquals(FeatureStatus.ESTIMATED, filled.getFeatureStatus());
    assertEquals(QUANTIFIER_MZ, filled.getMZ(), 0.001, "the quantifier feature should be added");
    assertEquals(ROW_RT + 0.01f, filled.getRT(), SCAN_INTERVAL);
    assertEquals(HEIGHT, filled.getHeight(), HEIGHT * 0.01);
    assertEquals(FeatureStatus.DETECTED, row.getFeature(reference).getFeatureStatus());
  }

  @Test
  void doesNotFillGapIfTopSignalIsMissing() {
    final RawDataFile sample = createFile("sample",
        Map.of(QUANTIFIER_MZ, ROW_RT, SECOND_MZ, ROW_RT, MINOR_MZ, ROW_RT));
    final ModularFeatureList flist = createAlignedList(false, reference, sample);

    runGapFilling(flist, OriginalFeatureListOption.PROCESS_IN_PLACE);

    assertNull(flist.getRow(0).getFeature(sample), "third most intense signal is missing");
  }

  @Test
  void doesNotFillGapOutsideRtTolerance() {
    final RawDataFile sample = createFile("sample",
        Map.of(QUANTIFIER_MZ, ROW_RT + 0.5f, SECOND_MZ, ROW_RT + 0.5f, THIRD_MZ, ROW_RT + 0.5f));
    final ModularFeatureList flist = createAlignedList(false, reference, sample);

    runGapFilling(flist, OriginalFeatureListOption.PROCESS_IN_PLACE);

    assertNull(flist.getRow(0).getFeature(sample), "compound elutes outside of RT tolerance");
  }

  @Test
  void doesNotFillGapIfTopSignalsDoNotCoelute() {
    // second signal is within the RT tolerance of the row but not co-eluting with the quantifier
    final RawDataFile sample = createFile("sample",
        Map.of(QUANTIFIER_MZ, ROW_RT, SECOND_MZ, ROW_RT + 0.08f, THIRD_MZ, ROW_RT));
    final ModularFeatureList flist = createAlignedList(false, reference, sample);

    runGapFilling(flist, OriginalFeatureListOption.PROCESS_IN_PLACE);

    assertNull(flist.getRow(0).getFeature(sample), "second signal does not co-elute");
  }

  @Test
  void fillsGapsWithSmoothingInNewFeatureList() {
    final RawDataFile filledSample = createFile("filled",
        Map.of(QUANTIFIER_MZ, ROW_RT, SECOND_MZ, ROW_RT, THIRD_MZ, ROW_RT));
    final RawDataFile emptySample = createFile("empty", Map.of());
    final ModularFeatureList flist = createAlignedList(true, reference, filledSample,
        emptySample);

    runGapFilling(flist, OriginalFeatureListOption.KEEP);

    assertNull(flist.getRow(0).getFeature(filledSample), "the original list stays unchanged");
    final ModularFeatureList gapFilled = project.getCurrentFeatureLists().stream()
        .map(ModularFeatureList.class::cast).filter(f -> f != flist).findFirst().orElseThrow();
    final FeatureListRow row = gapFilled.getRow(0);
    assertNotNull(row.getFeature(filledSample));
    assertNull(row.getFeature(emptySample));
    // smoothing lowers the apex of the synthetic gaussian peak slightly
    final float height = row.getFeature(filledSample).getHeight();
    assertTrue(height < HEIGHT && height > HEIGHT * 0.9, "unexpected height " + height);
    assertEquals(2, row.getNumberOfFeatures());
  }

  @Test
  void settingsRequireChromatogramBuilderAndResolver() {
    final ModularFeatureList flist = new ModularFeatureList("no history", null, reference);
    assertThrows(IllegalStateException.class,
        () -> GcEiFeatureFindingSettings.fromAppliedMethods(flist.getAppliedMethods()));

    flist.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(ModularADAPChromatogramBuilderModule.class,
            createChromatogramParameters(), Instant.now()));
    assertThrows(IllegalStateException.class,
        () -> GcEiFeatureFindingSettings.fromAppliedMethods(flist.getAppliedMethods()));
  }

  @Test
  void settingsOnlyUseSmoothingBeforeResolving() {
    final ModularFeatureList flist = new ModularFeatureList("history", null, reference);
    addFeatureFindingHistory(flist, false);
    // smoothing after resolving is not part of the feature detection
    flist.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(SmoothingModule.class, createSmoothingParameters(),
            Instant.now()));

    final GcEiFeatureFindingSettings settings = GcEiFeatureFindingSettings.fromAppliedMethods(
        flist.getAppliedMethods());
    assertNull(settings.smoothingParameters());
    assertEquals(4, settings.minConsecutiveScans());

    final ModularFeatureList smoothed = new ModularFeatureList("smoothed", null, reference);
    addFeatureFindingHistory(smoothed, true);
    assertNotNull(GcEiFeatureFindingSettings.fromAppliedMethods(smoothed.getAppliedMethods())
        .smoothingParameters());
  }

  private void runGapFilling(@NotNull final ModularFeatureList flist,
      @NotNull final OriginalFeatureListOption handleOriginal) {
    project.addFeatureList(flist);
    final ParameterSet params = GcEiGapFillingParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.ALL_FEATURELISTS), 3,
        new RTTolerance(0.1f, Unit.MINUTES), new RTTolerance(0.04f, Unit.MINUTES), "gaps",
        handleOriginal);
    final GcEiGapFillingTask task = new GcEiGapFillingTask(project, flist, null, Instant.now(),
        params, GcEiGapFillingModule.class);
    task.run();
    assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
  }

  /**
   * An aligned feature list with one row that is only detected in the first file. The row carries
   * the GC-EI pseudo spectrum of the compound.
   */
  private @NotNull ModularFeatureList createAlignedList(final boolean withSmoothing,
      @NotNull final RawDataFile... files) {
    final ModularFeatureList flist = new ModularFeatureList("aligned", null, files);
    DataTypeUtils.addDefaultChromatographicTypeColumns(flist);
    for (final RawDataFile file : files) {
      flist.setSelectedScans(file, file.getScans());
    }
    addFeatureFindingHistory(flist, withSmoothing);

    final RawDataFile detectedIn = files[0];
    final List<Scan> scans = new ArrayList<>(detectedIn.getScans());
    final double[] mzs = new double[scans.size()];
    final double[] intensities = new double[scans.size()];
    for (int i = 0; i < scans.size(); i++) {
      mzs[i] = QUANTIFIER_MZ;
      intensities[i] = gaussian(scans.get(i).getRetentionTime(), ROW_RT);
    }
    final ModularFeature feature = new ModularFeature(flist, detectedIn,
        new SimpleIonTimeSeries(null, mzs, intensities, scans), FeatureStatus.DETECTED);
    feature.setAllMS2FragmentScans(List.of(
        new SimplePseudoSpectrum(detectedIn, 1, ROW_RT, null,
            new double[]{QUANTIFIER_MZ, SECOND_MZ, THIRD_MZ, MINOR_MZ},
            new double[]{HEIGHT, HEIGHT * 0.8, HEIGHT * 0.6, HEIGHT * 0.1}, PolarityType.POSITIVE,
            "Pseudo spectrum", PseudoSpectrumType.GC_EI)));
    flist.addRow(new ModularFeatureListRow(flist, 1, feature));
    return flist;
  }

  private void addFeatureFindingHistory(@NotNull final ModularFeatureList flist,
      final boolean withSmoothing) {
    flist.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(ModularADAPChromatogramBuilderModule.class,
            createChromatogramParameters(), Instant.now()));
    if (withSmoothing) {
      flist.addDescriptionOfAppliedTask(
          new SimpleFeatureListAppliedMethod(SmoothingModule.class, createSmoothingParameters(),
              Instant.now()));
    }
    final MinimumSearchFeatureResolverParameters resolverParameters = MinimumSearchFeatureResolverParameters.create(
        new FeatureListsSelection(FeatureListsSelectionType.BATCH_LAST_FEATURELISTS), "r",
        OriginalFeatureListOption.KEEP, false, GroupMS2SubParameters.createDefault(),
        ResolvingDimension.RETENTION_TIME, 0.85, 0.05, 0d, 5E3, 1.7, Range.closed(0d, 1d), 4);
    flist.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(MinimumSearchFeatureResolverModule.class,
            resolverParameters, Instant.now()));
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

  private static @NotNull ParameterSet createChromatogramParameters() {
    return ADAPChromatogramBuilderParameters.create(
        new RawDataFilesSelection(RawDataFilesSelectionType.BATCH_LAST_FILES), new ScanSelection(1),
        4, MZ_TOLERANCE, "chromatograms", 1E3, 5E3, false);
  }

  /**
   * @param apexRtPerMz the apex retention time of each signal. Signals are scaled like in the
   *                    pseudo spectrum of the compound.
   */
  private static @NotNull RawDataFile createFile(@NotNull final String name,
      @NotNull final Map<Double, Float> apexRtPerMz) {
    final Map<Double, Double> relativeHeights = Map.of(QUANTIFIER_MZ, 1d, SECOND_MZ, 0.8d, THIRD_MZ,
        0.6d, MINOR_MZ, 0.1d);
    final RawDataFile file = new RawDataFileImpl(name, null, null);
    for (int i = 0; i < NUM_SCANS; i++) {
      final float rt = i * SCAN_INTERVAL;
      // sorted by m/z
      final TreeMap<Double, Double> signals = new TreeMap<>();
      for (final Map.Entry<Double, Float> entry : apexRtPerMz.entrySet()) {
        final double intensity =
            gaussian(rt, entry.getValue()) * relativeHeights.get(entry.getKey());
        if (intensity > 10) {
          signals.put(entry.getKey(), intensity);
        }
      }
      final SimpleScan scan = new SimpleScan(file, i, 1, rt, null,
          signals.keySet().stream().mapToDouble(Double::doubleValue).toArray(),
          signals.values().stream().mapToDouble(Double::doubleValue).toArray(),
          MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(50d, 300d));
      scan.addMassList(new ScanPointerMassList(scan));
      file.addScan(scan);
    }
    return file;
  }

  private static double gaussian(final float rt, final @Nullable Float apexRt) {
    if (apexRt == null) {
      return 0d;
    }
    final double delta = rt - apexRt;
    return HEIGHT * Math.exp(-delta * delta / (2 * PEAK_SIGMA * PEAK_SIGMA));
  }
}
