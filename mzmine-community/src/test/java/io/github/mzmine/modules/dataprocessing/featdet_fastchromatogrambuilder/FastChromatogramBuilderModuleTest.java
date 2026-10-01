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

package io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ChromatogramBuilderAlgorithms;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ChromatogramBuilderSettings;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MZToleranceOrAuto;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.scans.ScanUtils;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import testutils.MZmineTestUtil;
import testutils.TaskResult;

/**
 * Runs the fast algorithms of the chromatogram builder module on the small DOM test files like the
 * ADAP builder in FeatureFindingTest.
 */
@TestInstance(Lifecycle.PER_CLASS)
class FastChromatogramBuilderModuleTest {

  private static final MZTolerance TOLERANCE = new MZTolerance(0.002, 10);
  private static final String SUFFIX = "fastchrom";

  @BeforeAll
  void init() throws InterruptedException {
    MZmineTestUtil.startMzmineCore();
    MZmineTestUtil.clearProjectAndLibraries();
    final AdvancedSpectraImportParameters advanced = AdvancedSpectraImportParameters.create(
        MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL, 0d, 0d, null, ScanSelection.ALL_SCANS,
        false);
    MZmineTestUtil.importFiles(List.of("rawdatafiles/DOM_a.mzML", "rawdatafiles/DOM_b.mzXML"), 60,
        advanced);
  }

  @AfterAll
  void tearDown() {
    MZmineTestUtil.cleanProject();
  }

  @Test
  void buildsChromatogramListsLikeTheAdapBuilder() throws InterruptedException {
    final ParameterSet parameters = ADAPChromatogramBuilderParameters.createFast(
        new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES), new ScanSelection(1), 4,
        MZToleranceOrAuto.custom(TOLERANCE), SUFFIX, 1E5, 3E5, false);
    final TaskResult finished = MZmineTestUtil.callModuleWithTimeout(60,
        ModularADAPChromatogramBuilderModule.class, parameters);
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, finished, finished.description());

    final MZmineProject project = ProjectService.getProject();
    Assertions.assertEquals(2, project.getCurrentRawDataFiles().size());
    for (final RawDataFile raw : project.getCurrentRawDataFiles()) {
      final FeatureList flist = project.getFeatureList(raw.getName() + " " + SUFFIX);
      Assertions.assertNotNull(flist, "No chromatograms for " + raw.getName());
      Assertions.assertEquals(1, flist.getNumberOfRawDataFiles());
      // import and chromatogram builder
      Assertions.assertEquals(2, flist.getAppliedMethods().size());
      Assertions.assertTrue(MZmineTestUtil.isSorted(flist));
      final List<? extends Scan> selectedScans = flist.getSeletedScans(raw);
      Assertions.assertNotNull(selectedScans);
      Assertions.assertEquals(87, selectedScans.size());
      // the ADAP builder finds 974 and 1027 chromatograms, see FeatureFindingTest
      Assertions.assertTrue(flist.getNumberOfRows() > 900, "rows " + flist.getNumberOfRows());

      for (final FeatureListRow row : flist.getRows()) {
        final Feature feature = row.getFeatures().getFirst();
        assertFlankingZeros(feature.getFeatureData(), selectedScans);
        // the MS2 index must find the same fragment scans in the same order as the ADAP builder
        final Range<Double> mzRange = TOLERANCE.getToleranceRange(feature.getMZ())
            .span(feature.getRawDataPointsMZRange());
        final List<Scan> expected = ScanUtils.streamAllMS2FragmentScans(raw,
            feature.getRawDataPointsRTRange(), mzRange).toList();
        Assertions.assertEquals(expected, feature.getAllMS2FragmentScans());
      }
    }
  }

  @Test
  void autoToleranceIsEstimatedOnceAndStoredWithTheFeatureLists() throws InterruptedException {
    final String suffix = "fastauto";
    final ParameterSet parameters = ADAPChromatogramBuilderParameters.createFast(
        new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES), new ScanSelection(1), 4,
        MZToleranceOrAuto.auto(TOLERANCE), suffix, 1E5, 3E5, false);
    final TaskResult finished = MZmineTestUtil.callModuleWithTimeout(60,
        ModularADAPChromatogramBuilderModule.class, parameters);
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, finished, finished.description());

    final MZmineProject project = ProjectService.getProject();
    MZTolerance used = null;
    for (final RawDataFile raw : project.getCurrentRawDataFiles()) {
      final FeatureList flist = project.getFeatureList(raw.getName() + " " + suffix);
      Assertions.assertNotNull(flist, "No chromatograms for " + raw.getName());
      Assertions.assertTrue(flist.getNumberOfRows() > 500, "rows " + flist.getNumberOfRows());
      final MZToleranceOrAuto stored = appliedFastParameters(flist).getValue(
          FastChromatogramBuilderParameters.mzTolerance);
      Assertions.assertTrue(stored.isAuto());
      final MZTolerance tolerance = stored.tolerance();
      Assertions.assertNotNull(tolerance);
      Assertions.assertEquals(tolerance,
          ADAPChromatogramBuilderParameters.getAppliedSettings(flist.getAppliedMethods())
              .orElseThrow().mzTolerance());
      // the files are Orbitrap data, one estimate for all files
      Assertions.assertTrue(tolerance.getPpmTolerance() > 1 && tolerance.getPpmTolerance() < 30,
          tolerance.toString());
      Assertions.assertTrue(tolerance.getMzTolerance() < 0.01, tolerance.toString());
      if (used != null) {
        Assertions.assertEquals(used, tolerance);
      }
      used = tolerance;
    }
    Assertions.assertNotNull(used);
  }

  @Test
  void fastAutoDeterminesAllParametersOnceAndStoresThemInFast() throws InterruptedException {
    final String suffix = "fastautoparams";
    final ParameterSet parameters = ADAPChromatogramBuilderParameters.createFastAuto(
        new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES), new ScanSelection(1),
        ChromatogramBuilderSensitivity.MEDIUM, suffix, false);
    final TaskResult finished = MZmineTestUtil.callModuleWithTimeout(60,
        ModularADAPChromatogramBuilderModule.class, parameters);
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, finished, finished.description());

    final MZmineProject project = ProjectService.getProject();
    ChromatogramBuilderSettings used = null;
    for (final RawDataFile raw : project.getCurrentRawDataFiles()) {
      final FeatureList flist = project.getFeatureList(raw.getName() + " " + suffix);
      Assertions.assertNotNull(flist, "No chromatograms for " + raw.getName());
      Assertions.assertTrue(flist.getNumberOfRows() > 100, "rows " + flist.getNumberOfRows());
      final ParameterSet applied = flist.getAppliedMethods().getLast().getParameters();
      Assertions.assertEquals(ChromatogramBuilderAlgorithms.FAST_AUTO,
          applied.getValue(ADAPChromatogramBuilderParameters.algorithm));

      final ChromatogramBuilderSettings settings = ADAPChromatogramBuilderParameters.getAppliedSettings(
          flist.getAppliedMethods()).orElseThrow();
      // the import has no noise filter, the levels come from the data
      Assertions.assertTrue(settings.minGroupIntensity() > 0, settings.toString());
      Assertions.assertTrue(settings.minHeight() >= settings.minGroupIntensity(),
          settings.toString());
      Assertions.assertTrue(
          settings.minConsecutiveScans() >= BuilderParameterEstimation.MIN_CONSECUTIVE,
          settings.toString());
      Assertions.assertTrue(settings.mzTolerance().getPpmTolerance() < 30, settings.toString());
      // the hidden parameters of the applied method show the determined values
      final ParameterSet auto = applied.getParameter(ADAPChromatogramBuilderParameters.algorithm)
          .getEmbeddedParameters(ChromatogramBuilderAlgorithms.FAST_AUTO);
      Assertions.assertEquals(settings.minHeight(),
          auto.getValue(FastAutoChromatogramBuilderParameters.determinedMinHeight));
      final Double noiseLevel = auto.getValue(
          FastAutoChromatogramBuilderParameters.determinedNoiseLevel);
      final Double signalLevel = auto.getValue(
          FastAutoChromatogramBuilderParameters.determinedSignalLevel);
      Assertions.assertNotNull(noiseLevel);
      Assertions.assertTrue(signalLevel != null && signalLevel >= noiseLevel);
      // the module parameters stay empty, a rerun determines the values again
      Assertions.assertTrue(FastAutoChromatogramBuilderParameters.getDeterminedSettings(
          parameters.getParameter(ADAPChromatogramBuilderParameters.algorithm)
              .getEmbeddedParameters(ChromatogramBuilderAlgorithms.FAST_AUTO)).isEmpty());
      if (used != null) {
        Assertions.assertEquals(used, settings);
      }
      used = settings;
    }
    Assertions.assertNotNull(used);
  }

  /**
   * @return the parameters of the fast algorithm of the latest chromatogram builder
   */
  private static ParameterSet appliedFastParameters(FeatureList flist) {
    final ParameterSet applied = flist.getAppliedMethods().getLast().getParameters();
    return applied.getParameter(ADAPChromatogramBuilderParameters.algorithm)
        .getEmbeddedParameters(ChromatogramBuilderAlgorithms.FAST);
  }

  /**
   * Every scan next to a detected data point is part of the series, missing signals are zeros.
   */
  private static void assertFlankingZeros(IonTimeSeries<? extends Scan> series,
      List<? extends Scan> selectedScans) {
    for (int i = 0; i < series.getNumberOfValues(); i++) {
      if (series.getIntensity(i) <= 0) {
        continue;
      }
      final int scanIndex = selectedScans.indexOf(series.getSpectrum(i));
      if (scanIndex > 0) {
        Assertions.assertTrue(
            i > 0 && series.getSpectrum(i - 1) == selectedScans.get(scanIndex - 1),
            "Previous scan missing in the series");
      }
      if (scanIndex < selectedScans.size() - 1) {
        Assertions.assertTrue(
            i < series.getNumberOfValues() - 1 && series.getSpectrum(i + 1) == selectedScans.get(
                scanIndex + 1), "Next scan missing in the series");
      }
    }
  }
}
