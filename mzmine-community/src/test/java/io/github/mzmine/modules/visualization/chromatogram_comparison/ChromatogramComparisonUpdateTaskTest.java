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

package io.github.mzmine.modules.visualization.chromatogram_comparison;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.FastChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_fastchromatogrambuilder.FastChromatogramBuilderParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.project.ProjectService;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import testutils.MZmineTestUtil;
import testutils.TaskResult;

/**
 * Compares the ADAP and the fast chromatogram builder on a small DOM test file.
 */
@TestInstance(Lifecycle.PER_CLASS)
class ChromatogramComparisonUpdateTaskTest {

  private static final Logger logger = Logger.getLogger(
      ChromatogramComparisonUpdateTaskTest.class.getName());
  private static final MZTolerance TOLERANCE = new MZTolerance(0.002, 10);
  private static final double MIN_HEIGHT = 3E5;

  private FeatureList adap;
  private FeatureList fast;

  @BeforeAll
  void init() throws InterruptedException {
    MZmineTestUtil.startMzmineCore();
    MZmineTestUtil.clearProjectAndLibraries();
    final AdvancedSpectraImportParameters advanced = AdvancedSpectraImportParameters.create(
        MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL, 0d, 0d, null, ScanSelection.ALL_SCANS,
        false);
    MZmineTestUtil.importFiles(List.of("rawdatafiles/DOM_a.mzML"), 60, advanced);

    final RawDataFilesSelection files = new RawDataFilesSelection(
        RawDataFilesSelectionType.ALL_FILES);
    final ParameterSet adapParameters = ADAPChromatogramBuilderParameters.create(files,
        new ScanSelection(1), 4, TOLERANCE, "adap", 1E5, MIN_HEIGHT, false);
    final TaskResult adapResult = MZmineTestUtil.callModuleWithTimeout(60,
        ModularADAPChromatogramBuilderModule.class, adapParameters);
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, adapResult, adapResult.description());

    final ParameterSet fastParameters = FastChromatogramBuilderParameters.create(files,
        new ScanSelection(1), 4, TOLERANCE, "fast", 1E5, MIN_HEIGHT, false);
    final TaskResult fastResult = MZmineTestUtil.callModuleWithTimeout(60,
        FastChromatogramBuilderModule.class, fastParameters);
    Assertions.assertInstanceOf(TaskResult.FINISHED.class, fastResult, fastResult.description());

    final MZmineProject project = ProjectService.getProject();
    adap = project.getFeatureList("DOM_a.mzML adap");
    fast = project.getFeatureList("DOM_a.mzML fast");
    Assertions.assertNotNull(adap);
    Assertions.assertNotNull(fast);
  }

  @AfterAll
  void tearDown() {
    MZmineTestUtil.cleanProject();
  }

  @Test
  void defaultsComeFromTheChromatogramBuilders() {
    Assertions.assertEquals(TOLERANCE,
        ChromatogramComparisonController.extractGroupingTolerance(adap, fast));
    Assertions.assertEquals(TOLERANCE,
        ChromatogramComparisonController.extractGroupingTolerance(fast, adap));
  }

  @Test
  void groupsAllChromatogramsOfBothBuilders() {
    final ChromatogramComparisonModel model = new ChromatogramComparisonModel();
    model.setFeatureListA(adap);
    model.setFeatureListB(fast);
    model.setMzTolerance(TOLERANCE);
    model.setMinSignalIntensity(MIN_HEIGHT);

    final ChromatogramComparisonUpdateTask task = new ChromatogramComparisonUpdateTask(model);
    Assertions.assertTrue(task.checkPreConditions());
    task.process();
    task.updateGuiModel();

    final List<ChromatogramGroup> groups = model.getGroups();
    Assertions.assertFalse(groups.isEmpty(), model.getStatus());
    Assertions.assertEquals(adap.getNumberOfRows(),
        groups.stream().mapToInt(g -> g.a().chromatograms().size()).sum());
    Assertions.assertEquals(fast.getNumberOfRows(),
        groups.stream().mapToInt(g -> g.b().chromatograms().size()).sum());
    for (final ChromatogramGroup group : groups) {
      Assertions.assertFalse(group.a().isEmpty() && group.b().isEmpty());
      Assertions.assertNotNull(group.signalRtRange());
      group.a().chromatograms().forEach(c -> Assertions.assertNotNull(c.feature()));
      group.b().chromatograms().forEach(c -> Assertions.assertNotNull(c.feature()));
    }
    // both builders find the same ions in most groups
    final long paired = groups.stream().filter(g -> !g.a().isEmpty() && !g.b().isEmpty()).count();
    Assertions.assertTrue(paired > 0.8 * groups.size(),
        "paired " + paired + " of " + groups.size());

    final Map<ChromatogramIssue, Long> issueCounts = Arrays.stream(ChromatogramIssue.values())
        .collect(Collectors.toMap(Function.identity(),
            issue -> groups.stream().filter(g -> g.issues().contains(issue)).count()));
    logger.info(() -> model.getStatus() + ", issues: " + issueCounts);
  }
}
