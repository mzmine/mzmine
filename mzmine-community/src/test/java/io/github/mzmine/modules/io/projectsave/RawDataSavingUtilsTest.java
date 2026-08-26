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

package io.github.mzmine.modules.io.projectsave;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectionParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetectors;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.exactmass.ExactMassDetectorParameters;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import testutils.MZmineTestUtil;

class RawDataSavingUtilsTest {

  private static final double MS1_NOISE = 100d;
  private static final double MS2_NOISE = 10d;

  private RawDataFile raw1;
  private RawDataFile raw2;
  private RawDataFile raw3;
  private File file1;
  private File file2;
  private File file3;

  @BeforeAll
  static void initMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @BeforeEach
  void setUp() {
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
    file1 = new File("raw-saving-test-1.mzML").getAbsoluteFile();
    file2 = new File("raw-saving-test-2.mzML").getAbsoluteFile();
    file3 = new File("raw-saving-test-3.mzML").getAbsoluteFile();
    raw1 = createRawDataFile(file1);
    raw2 = createRawDataFile(file2);
    raw3 = createRawDataFile(file3);
    ProjectService.getProject().addFile(raw1);
    ProjectService.getProject().addFile(raw2);
    ProjectService.getProject().addFile(raw3);
  }

  @Test
  void mergesEquivalentAppliedMethodBatches() {
    final List<BatchQueue> batches = createAppliedMethodBatches(MS1_NOISE);

    // decision: file selections differ between imports and must be combined during the merge.
    final BatchQueue merged = RawDataSavingUtils.mergeQueues(batches.getFirst(), batches.getLast(),
        true);

    Assertions.assertNotNull(merged);
    Assertions.assertEquals(3, merged.size());
    Assertions.assertInstanceOf(AllSpectralDataImportModule.class, merged.get(0).getModule());
    Assertions.assertInstanceOf(MassDetectionModule.class, merged.get(1).getModule());
    Assertions.assertInstanceOf(MassDetectionModule.class, merged.get(2).getModule());
    Assertions.assertEquals(Set.of(file1, file2, file3), Set.of(
        merged.get(0).getParameterSet().getValue(AllSpectralDataImportParameters.fileNames)));
    // steps are ordered by module call date, so the MS1 detection must precede the MS2 detection.
    assertSelectedRawFiles(merged.get(1).getParameterSet());
    assertMsLevel(merged.get(1).getParameterSet(), 1);
    assertSelectedRawFiles(merged.get(2).getParameterSet());
    assertMsLevel(merged.get(2).getParameterSet(), 2);
  }

  @Test
  void doesNotMergeAppliedMethodBatchesWithDifferentMs1Noise() {
    final List<BatchQueue> batches = createAppliedMethodBatches(MS1_NOISE + 1d);

    final BatchQueue merged = RawDataSavingUtils.mergeQueues(batches.getFirst(), batches.getLast(),
        true);

    Assertions.assertNull(merged);
  }

  private List<BatchQueue> createAppliedMethodBatches(final double secondMs1Noise) {
    final Instant firstImport = Instant.parse("2026-01-01T10:00:00Z");
    final Instant firstMs1 = Instant.parse("2026-01-01T10:01:00Z");
    final Instant firstMs2 = Instant.parse("2026-01-01T10:02:00Z");
    final File[] firstFiles = {file1, file2};
    final RawDataFile[] firstRaws = {raw1, raw2};
    addAppliedMethods(raw1, firstFiles, firstRaws, firstImport, firstMs1, firstMs2, MS1_NOISE);
    addAppliedMethods(raw2, firstFiles, firstRaws, firstImport, firstMs1, firstMs2, MS1_NOISE);

    addAppliedMethods(raw3, new File[]{file3}, new RawDataFile[]{raw3},
        Instant.parse("2026-01-01T11:00:00Z"), Instant.parse("2026-01-01T11:01:00Z"),
        Instant.parse("2026-01-01T11:02:00Z"), secondMs1Noise);

    return List.of(RawDataSavingUtils.makeBatchQueue(List.of(raw1, raw2)),
        RawDataSavingUtils.makeBatchQueue(List.of(raw3)));
  }

  private void addAppliedMethods(final RawDataFile raw, final File[] importFiles,
      final RawDataFile[] massDetectionFiles, final Instant importTime, final Instant ms1Time,
      final Instant ms2Time, final double ms1Noise) {
    final ObservableList<FeatureListAppliedMethod> methods = raw.getAppliedMethods();
    methods.add(new SimpleFeatureListAppliedMethod(importModule(), importParameters(importFiles),
        importTime));
    methods.add(new SimpleFeatureListAppliedMethod(massDetectionModule(),
        massDetectionParameters(massDetectionFiles, 1, ms1Noise), ms1Time));
    methods.add(new SimpleFeatureListAppliedMethod(massDetectionModule(),
        massDetectionParameters(massDetectionFiles, 2, MS2_NOISE), ms2Time));
  }

  private static ParameterSet importParameters(final File[] files) {
    final ParameterSet parameters = new AllSpectralDataImportParameters().cloneParameterSet();
    parameters.setParameter(AllSpectralDataImportParameters.fileNames, files);
    return parameters;
  }

  private static ParameterSet massDetectionParameters(final RawDataFile[] files, final int msLevel,
      final double noiseLevel) {
    final ParameterSet detectorParameters = new ExactMassDetectorParameters().cloneParameterSet();
    detectorParameters.setParameter(ExactMassDetectorParameters.noiseLevel, noiseLevel);

    final ParameterSet parameters = new MassDetectionParameters().cloneParameterSet();
    parameters.setParameter(MassDetectionParameters.dataFiles, new RawDataFilesSelection(files));
    parameters.setParameter(MassDetectionParameters.scanSelection, new ScanSelection(msLevel));
    parameters.getParameter(MassDetectionParameters.massDetector)
        .setValue(MassDetectors.EXACT, detectorParameters);
    return parameters;
  }

  private void assertSelectedRawFiles(final ParameterSet parameters) {
    final Set<RawDataFile> expected = Set.of(raw1, raw2, raw3);
    final Set<RawDataFile> actual = Arrays.stream(
            parameters.getValue(MassDetectionParameters.dataFiles).getMatchingRawDataFiles())
        .collect(Collectors.toSet());
    Assertions.assertEquals(expected, actual);
  }

  private void assertMsLevel(final ParameterSet parameters, final Integer expectedMsLevel) {
    final Integer actualMsLevel = parameters.getValue(MassDetectionParameters.scanSelection)
        .getMsLevelFilter().getSingleMsLevelOrNull();
    Assertions.assertEquals(expectedMsLevel, actualMsLevel);
  }

  private static RawDataFile createRawDataFile(final File file) {
    final RawDataFile raw = Mockito.mock(RawDataFile.class);
    Mockito.when(raw.getName()).thenReturn(file.getName());
    Mockito.when(raw.getAbsolutePath()).thenReturn(file.getAbsolutePath());
    Mockito.when(raw.getAbsoluteFilePath()).thenReturn(file);
    Mockito.when(raw.getAppliedMethods()).thenReturn(FXCollections.observableArrayList());
    return raw;
  }

  private static MZmineProcessingModule importModule() {
    return MZmineCore.getModuleInstance(AllSpectralDataImportModule.class);
  }

  private static MZmineProcessingModule massDetectionModule() {
    return MZmineCore.getModuleInstance(MassDetectionModule.class);
  }
}
