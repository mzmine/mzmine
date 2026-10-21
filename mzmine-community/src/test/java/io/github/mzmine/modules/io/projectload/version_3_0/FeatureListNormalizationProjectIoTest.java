/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.io.projectload.version_3_0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.dataprocessing.norm_intensity.FeatureRowNormalizationFunction;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizationSummary;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizerParameters;
import io.github.mzmine.modules.dataprocessing.norm_intensity.RawFileNormalizationFunction;
import io.github.mzmine.modules.io.projectsave.FeatureListSaveTask;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeatureListNormalizationProjectIoTest {

  @TempDir
  File tempDir;

  @Test
  void restoresSavedRowIdsBeforeAttachingRowDependentNormalization() throws Exception {
    final RawDataFileImpl savedRaw = new RawDataFileImpl("sample.mzML", null, null);
    final ModularFeatureList savedList = new ModularFeatureList("list", null, savedRaw);
    final SimpleScan scan = new SimpleScan(savedRaw, 1, 1, 0f, null, new double[]{100d},
        new double[]{10d}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
        Range.closed(0d, 2000d));
    savedRaw.addScan(scan);
    savedList.setSelectedScans(savedRaw, List.of(scan));
    addRow(savedList, savedRaw, 1, 10f);
    addRow(savedList, savedRaw, 2, 20f);
    final IntensityNormalizerParameters parameters = new IntensityNormalizerParameters();
    parameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        new IntensityNormalizationSummary(List.of(new RawFileNormalizationFunction(savedRaw,
            new FeatureRowNormalizationFunction(Map.of(1, 2d, 2, 3d), Map.of())))));
    savedList.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class, parameters, Instant.now()));

    final File archive = new File(tempDir, "project.mzmine");
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(archive))) {
      final FeatureListSaveTask saveTask = new FeatureListSaveTask(savedList, out);
      saveTask.run();
      assertEquals(TaskStatus.FINISHED, saveTask.getStatus());
    }

    final MZmineProjectImpl loadedProject = new MZmineProjectImpl();
    final RawDataFileImpl loadedRaw = new RawDataFileImpl("sample.mzML", null, null);
    loadedRaw.addScan(new SimpleScan(loadedRaw, 1, 1, 0f, null, new double[]{100d},
        new double[]{10d}, MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "",
        Range.closed(0d, 2000d)));
    loadedProject.addFile(loadedRaw);
    try (ZipFile zip = new ZipFile(archive)) {
      final FeatureListLoadTask loadTask = new FeatureListLoadTask(null, loadedProject, zip);
      loadTask.run();
      assertEquals(TaskStatus.FINISHED, loadTask.getStatus());
    }

    assertEquals(List.of(1, 2), loadedProject.getCurrentFeatureLists().getFirst().getRows().stream()
        .map(ModularFeatureListRow.class::cast).map(ModularFeatureListRow::getID).toList());
    assertTrue(loadedProject.getCurrentFeatureLists().getFirst().getAppliedMethods().stream()
        .anyMatch(method -> method.getModule() instanceof IntensityNormalizerModule));
  }

  private static void addRow(ModularFeatureList list, RawDataFileImpl raw, int id, float area) {
    final ModularFeatureListRow row = new ModularFeatureListRow(list, id);
    final ModularFeature feature = new ModularFeature(list, raw, FeatureStatus.DETECTED);
    feature.setArea(area);
    feature.setHeight(area);
    feature.setMZ(100d);
    feature.setRT(5f);
    row.addFeature(raw, feature, false);
    row.applyRowBindings();
    list.addRow(row);
  }
}
