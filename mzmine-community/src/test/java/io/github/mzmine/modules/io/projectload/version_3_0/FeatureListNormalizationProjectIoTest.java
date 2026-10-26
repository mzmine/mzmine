/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.io.projectload.version_3_0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FeatureListNormalizationProjectIoTest {

  private static final IntensityNormalizationSummary NO_FUNCTIONS = new IntensityNormalizationSummary(
      List.of(), List.of("diagnostic message"));

  @TempDir
  File tempDir;

  @Test
  void restoresExactIdsAndFunctionsFromSettings() throws Exception {
    final RawDataFileImpl savedRaw = createRaw();
    final ModularFeatureList savedList = createList(savedRaw, NO_FUNCTIONS);
    savedList.setSettings(savedList.getSettings().withNormalization(rowFactors(savedRaw)));

    final File archive = save(savedList, "project.mzmine");
    final RawDataFileImpl loadedRaw = createRaw();
    final ModularFeatureList loadedList = load(archive, loadedRaw);

    assertEquals(List.of(1, 2), loadedList.getRows().stream()
        .map(ModularFeatureListRow.class::cast).map(ModularFeatureListRow::getID).toList());
    assertEquals(Map.of(1, 2d, 2, 3d), ((FeatureRowNormalizationFunction) loadedList.getSettings()
        .normalization().get(0)).factors());
    assertTrue(IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(loadedList,
        loadedRaw).isPresent());
    // the applied method keeps only the diagnostics
    assertEquals(List.of("diagnostic message"),
        IntensityNormalizerModule.getLegacyNormalizationFromAppliedMethods(loadedList).messages());
  }

  @Test
  void migratesFunctionsFromAppliedMethodOfOldProjects() throws Exception {
    final RawDataFileImpl savedRaw = createRaw();
    final ModularFeatureList savedList = createList(savedRaw, rowFactors(savedRaw));

    final File archive = save(savedList, "old-project.mzmine");
    // projects saved before the settings were introduced have no settings element
    final File oldArchive = removeSettingsElement(archive);
    final RawDataFileImpl loadedRaw = createRaw();
    final ModularFeatureList loadedList = load(oldArchive, loadedRaw);

    final IntensityNormalizationSummary migrated = loadedList.getSettings().normalization();
    assertEquals(1, migrated.functions().size());
    assertTrue(migrated.messages().isEmpty());
    assertTrue(IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(loadedList,
        loadedRaw).isPresent());
  }

  @Test
  void keepsClearedNormalizationInsteadOfMigratingAppliedMethod() throws Exception {
    final RawDataFileImpl savedRaw = createRaw();
    // applied method with functions but the settings were cleared
    final ModularFeatureList savedList = createList(savedRaw, rowFactors(savedRaw));

    final File archive = save(savedList, "cleared.mzmine");
    final RawDataFileImpl loadedRaw = createRaw();
    final ModularFeatureList loadedList = load(archive, loadedRaw);

    assertNull(loadedList.getSettings().normalization());
    assertFalse(IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(loadedList,
        loadedRaw).isPresent());
  }

  private static IntensityNormalizationSummary rowFactors(final RawDataFileImpl raw) {
    return new IntensityNormalizationSummary(List.of(new RawFileNormalizationFunction(raw,
        new FeatureRowNormalizationFunction(Map.of(1, 2d, 2, 3d), Map.of()))));
  }

  private static RawDataFileImpl createRaw() {
    final RawDataFileImpl raw = new RawDataFileImpl("sample.mzML", null, null);
    raw.addScan(new SimpleScan(raw, 1, 1, 0f, null, new double[]{100d}, new double[]{10d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(0d, 2000d)));
    return raw;
  }

  private static ModularFeatureList createList(final RawDataFileImpl raw,
      final IntensityNormalizationSummary appliedMethodSummary) {
    final ModularFeatureList list = new ModularFeatureList("list", null, raw);
    list.setSelectedScans(raw, List.of(raw.getScans().getFirst()));
    addRow(list, raw, 1, 10f);
    addRow(list, raw, 2, 20f);
    final IntensityNormalizerParameters parameters = (IntensityNormalizerParameters) new IntensityNormalizerParameters().cloneParameterSet();
    parameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        appliedMethodSummary);
    list.addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class, parameters,
            Instant.now()));
    return list;
  }

  private File save(final ModularFeatureList list, final String name) throws Exception {
    final File archive = new File(tempDir, name);
    try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(archive))) {
      final FeatureListSaveTask saveTask = new FeatureListSaveTask(list, out);
      saveTask.run();
      assertEquals(TaskStatus.FINISHED, saveTask.getStatus());
    }
    return archive;
  }

  private static ModularFeatureList load(final File archive, final RawDataFileImpl loadedRaw)
      throws Exception {
    final MZmineProjectImpl loadedProject = new MZmineProjectImpl();
    loadedProject.addFile(loadedRaw);
    try (ZipFile zip = new ZipFile(archive)) {
      final FeatureListLoadTask loadTask = new FeatureListLoadTask(null, loadedProject, zip);
      loadTask.run();
      assertEquals(TaskStatus.FINISHED, loadTask.getStatus());
    }
    return (ModularFeatureList) loadedProject.getCurrentFeatureLists().getFirst();
  }

  /**
   * Copies the archive without the (empty) settings element to simulate an old project.
   */
  private File removeSettingsElement(final File archive) throws Exception {
    final File oldArchive = new File(tempDir, "without-settings-" + archive.getName());
    final String emptySettings = "<" + CONST.XML_FLIST_SETTINGS_ELEMENT + "/>";
    boolean removed = false;
    try (ZipFile zip = new ZipFile(archive);
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(oldArchive))) {
      final Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();
        byte[] bytes;
        try (InputStream in = zip.getInputStream(entry)) {
          bytes = in.readAllBytes();
        }
        final String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.contains(emptySettings)) {
          bytes = content.replace(emptySettings, "").getBytes(StandardCharsets.UTF_8);
          removed = true;
        }
        out.putNextEntry(new ZipEntry(entry.getName()));
        out.write(bytes);
        out.closeEntry();
      }
    }
    assertTrue(removed);
    return oldArchive;
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
