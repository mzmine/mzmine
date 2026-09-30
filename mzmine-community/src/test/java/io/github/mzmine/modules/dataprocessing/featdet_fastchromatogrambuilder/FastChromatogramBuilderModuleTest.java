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
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
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
 * Runs the module on the small DOM test files like the ADAP builder in FeatureFindingTest.
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
    final ParameterSet parameters = FastChromatogramBuilderParameters.create(
        new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES), new ScanSelection(1), 4,
        TOLERANCE, SUFFIX, 1E5, 3E5, false);
    final TaskResult finished = MZmineTestUtil.callModuleWithTimeout(60,
        FastChromatogramBuilderModule.class, parameters);
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
    final ParameterSet parameters = FastChromatogramBuilderParameters.create(
        new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES), new ScanSelection(1), 4,
        MZToleranceOrAuto.auto(TOLERANCE), suffix, 1E5, 3E5, false);
    final TaskResult finished = MZmineTestUtil.callModuleWithTimeout(60,
        FastChromatogramBuilderModule.class, parameters);
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, finished, finished.description());

    final MZmineProject project = ProjectService.getProject();
    MZTolerance used = null;
    for (final RawDataFile raw : project.getCurrentRawDataFiles()) {
      final FeatureList flist = project.getFeatureList(raw.getName() + " " + suffix);
      Assertions.assertNotNull(flist, "No chromatograms for " + raw.getName());
      Assertions.assertTrue(flist.getNumberOfRows() > 500, "rows " + flist.getNumberOfRows());
      final MZToleranceOrAuto stored = ParameterUtils.getValueFromAppliedMethods(
          flist.getAppliedMethods(), FastChromatogramBuilderParameters.class,
          FastChromatogramBuilderParameters.mzTolerance).orElseThrow();
      Assertions.assertTrue(stored.isAuto());
      final MZTolerance tolerance = stored.tolerance();
      Assertions.assertNotNull(tolerance);
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
