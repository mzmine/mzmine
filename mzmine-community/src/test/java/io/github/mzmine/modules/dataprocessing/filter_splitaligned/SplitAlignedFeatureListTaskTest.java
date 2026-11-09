package io.github.mzmine.modules.dataprocessing.filter_splitaligned;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.dataprocessing.norm_intensity.FactorNormalizationFunction;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizationSummary;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizerModule;
import io.github.mzmine.modules.dataprocessing.norm_intensity.RawFileNormalizationFunction;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SplitAlignedFeatureListTaskTest {

  /**
   * Split lists are reprocessed per sample group (e.g., resolve, expand, align again), so the
   * normalization of the aligned list is dropped: no functions and no normalized values.
   */
  @Test
  void splitListsDropNormalization() {
    final RawDataFileImpl a = new RawDataFileImpl("a", null, null);
    final RawDataFileImpl b = new RawDataFileImpl("b", null, null);
    final ModularFeatureList aligned = new ModularFeatureList("aligned", null, a, b);
    aligned.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    aligned.addFeatureType(DataTypes.get(NormalizedHeightType.class));
    addRow(aligned, 1, a, b);
    aligned.setSettings(aligned.getSettings().withNormalization(new IntensityNormalizationSummary(
        List.of(new RawFileNormalizationFunction(a, new FactorNormalizationFunction(2d)),
            new RawFileNormalizationFunction(b, new FactorNormalizationFunction(3d))))));
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(aligned);

    final ParameterSet parameters = new SplitAlignedFeatureListParameters().cloneParameterSet();
    parameters.setParameter(SplitAlignedFeatureListParameters.grouping, false);
    final Task task = new SplitAlignedFeatureListModule().createTask(project, parameters,
        Instant.now(), null, aligned);
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    final List<FeatureList> splitLists = project.getCurrentFeatureLists().stream()
        .filter(flist -> flist != aligned).toList();
    assertEquals(2, splitLists.size());
    for (final FeatureList split : splitLists) {
      assertNull(split.getSettings().normalization());
      split.streamFeatures().forEach(feature -> {
        assertNull(feature.get(NormalizedAreaType.class));
        assertNull(feature.get(NormalizedHeightType.class));
      });
    }
    // the aligned list keeps its normalization
    assertTrue(IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(aligned, a)
        .isPresent());
    aligned.streamFeatures()
        .forEach(feature -> assertEquals(20f, feature.get(NormalizedAreaType.class)));
  }

  private static void addRow(final ModularFeatureList list, final int id,
      final RawDataFileImpl... files) {
    final ModularFeatureListRow row = new ModularFeatureListRow(list, id);
    for (final RawDataFileImpl file : files) {
      final ModularFeature feature = new ModularFeature(list, file, FeatureStatus.DETECTED);
      feature.setArea(10f);
      feature.setHeight(10f);
      feature.setMZ(100d);
      feature.setRT(5f);
      feature.set(NormalizedAreaType.class, 20f);
      feature.set(NormalizedHeightType.class, 20f);
      row.addFeature(file, feature, false);
    }
    row.applyRowBindings();
    list.addRow(row);
  }
}
