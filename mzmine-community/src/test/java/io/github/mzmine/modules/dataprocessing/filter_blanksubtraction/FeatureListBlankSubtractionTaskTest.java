package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.dataprocessing.filter_blanksubtraction.FeatureListBlankSubtractionTask.BlankSubtractionOptions;
import io.github.mzmine.modules.dataprocessing.norm_intensity.FactorNormalizationFunction;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizationSummary;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_intensity.RawFileNormalizationFunction;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class FeatureListBlankSubtractionTaskTest {

  @AfterEach
  void resetProject() {
    ProjectService.getProjectManager().setCurrentProject(new MZmineProjectImpl());
  }

  /**
   * Blank subtraction removes features, which changes the basis of all normalization factors. The
   * results have no normalization, the original list keeps it.
   */
  @Test
  void resultsDropNormalization() {
    final RawDataFileImpl blank = new RawDataFileImpl("blank", null, null);
    final RawDataFileImpl sample = new RawDataFileImpl("sample", null, null);
    final MZmineProjectImpl project = new MZmineProjectImpl();
    ProjectService.getProjectManager().setCurrentProject(project);
    project.addFile(blank);
    project.addFile(sample);

    final ModularFeatureList aligned = new ModularFeatureList("aligned", null, blank, sample);
    aligned.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    aligned.addFeatureType(DataTypes.get(NormalizedHeightType.class));
    // row 1 is kept in the sample, row 2 is background and only ends up in the deleted list
    addRow(aligned, 1, blank, 10f, sample, 100f);
    addRow(aligned, 2, blank, 100f, sample, 10f);
    aligned.setSettings(aligned.getSettings().withNormalization(new IntensityNormalizationSummary(
        List.of(new RawFileNormalizationFunction(blank, new FactorNormalizationFunction(2d)),
            new RawFileNormalizationFunction(sample, new FactorNormalizationFunction(2d))))));
    project.addFeatureList(aligned);

    final FeatureListBlankSubtractionParameters parameters = (FeatureListBlankSubtractionParameters) new FeatureListBlankSubtractionParameters().cloneParameterSet();
    parameters.setParameter(FeatureListBlankSubtractionParameters.alignedPeakList,
        new FeatureListsSelection(aligned));
    parameters.setParameter(FeatureListBlankSubtractionParameters.blankRawDataFiles,
        new RawDataFilesSelection(new RawDataFile[]{blank}));
    parameters.setParameter(FeatureListBlankSubtractionParameters.minBlanks, 1);
    parameters.getParameter(FeatureListBlankSubtractionParameters.foldChange).setValue(false);
    parameters.setParameter(FeatureListBlankSubtractionParameters.subtractionOption,
        BlankSubtractionOptions.EACH_FEATURE);
    parameters.setParameter(FeatureListBlankSubtractionParameters.suffix, "subtracted");
    parameters.setParameter(FeatureListBlankSubtractionParameters.handleOriginal,
        OriginalFeatureListOption.KEEP);
    parameters.setParameter(FeatureListBlankSubtractionParameters.createDeleted, true);

    final FeatureListBlankSubtractionTask task = new FeatureListBlankSubtractionTask(project,
        parameters, null, Instant.now());
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    final List<FeatureList> results = project.getCurrentFeatureLists().stream()
        .filter(flist -> flist != aligned).toList();
    assertEquals(2, results.size());
    for (final FeatureList result : results) {
      assertTrue(result.getNumberOfRows() > 0);
      assertNull(result.getSettings().normalization());
      result.streamFeatures().forEach(feature -> {
        assertNull(feature.get(NormalizedAreaType.class));
        assertNull(feature.get(NormalizedHeightType.class));
      });
    }
    // the original list keeps its normalization
    assertTrue(
        IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(aligned, sample)
            .isPresent());
    aligned.streamFeatures()
        .forEach(feature -> assertEquals(99f, feature.get(NormalizedAreaType.class)));
  }

  private static void addRow(final ModularFeatureList list, final int id,
      final RawDataFileImpl blank, final float blankAbundance, final RawDataFileImpl sample,
      final float sampleAbundance) {
    final ModularFeatureListRow row = new ModularFeatureListRow(list, id);
    row.addFeature(blank, createFeature(list, blank, blankAbundance), false);
    row.addFeature(sample, createFeature(list, sample, sampleAbundance), false);
    row.applyRowBindings();
    list.addRow(row);
  }

  private static ModularFeature createFeature(final ModularFeatureList list,
      final RawDataFileImpl file, final float abundance) {
    final ModularFeature feature = new ModularFeature(list, file, FeatureStatus.DETECTED);
    feature.setArea(abundance);
    feature.setHeight(abundance);
    feature.setMZ(100d);
    feature.setRT(5f);
    feature.set(NormalizedAreaType.class, 99f);
    feature.set(NormalizedHeightType.class, 99f);
    return feature;
  }
}
