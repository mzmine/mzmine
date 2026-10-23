/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedHeightType;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterParameters;
import io.github.mzmine.modules.dataprocessing.filter_rowsfilter.RowsFilterTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class NormalizationRowIdentityTest {

  @ParameterizedTest
  @EnumSource(value = OriginalFeatureListOption.class, names = {"KEEP", "PROCESS_IN_PLACE"})
  void rowsFilterRemapsFactorsWhenRenumbering(final OriginalFeatureListOption mode) {
    final Fixture fixture = fixture();
    final ParameterSet parameters = RowsFilterParameters.createDefaultAllOff();
    parameters.setParameter(RowsFilterParameters.Reset_ID, true);
    parameters.setParameter(RowsFilterParameters.handleOriginal, mode);
    final RowsFilterTask task = new RowsFilterTask(fixture.project, fixture.list, parameters, null,
        Instant.now());

    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus());
    final ModularFeatureList result = (ModularFeatureList) fixture.project.getCurrentFeatureLists()
        .getLast();
    assertEquals(List.of(1, 2), result.getRows().stream().map(r -> r.getID()).toList());
    // rows 5 and 7 became 1 and 2
    assertEquals(Map.of(1, 2d, 2, 4d), rowFactors(result).factors());
    assertEquals(Map.of(), rowFactors(result).unavailableReasons());
  }

  @Test
  void copyWithRowsPrunesRemovedRows() {
    final Fixture fixture = fixture();
    final ModularFeatureListRow kept = (ModularFeatureListRow) fixture.list.findRowByID(7);

    final ModularFeatureList copy = FeatureListUtils.createCopyWithRows(fixture.list, null, "copy",
        null, List.of(kept), false);

    assertEquals(Map.of(7, 4d), rowFactors(copy).factors());
    // source keeps its own factors
    assertEquals(Map.of(5, 2d, 7, 4d), rowFactors(fixture.list).factors());
  }

  @Test
  void reusedIdOfRemovedRowStaysUnnormalized() {
    final Fixture fixture = fixture();
    final ModularFeatureListRow kept = (ModularFeatureListRow) fixture.list.findRowByID(7);
    final ModularFeatureList copy = FeatureListUtils.createCopyWithRows(fixture.list, null, "copy",
        null, List.of(kept), false);

    // a later task adds a new row with the ID of the removed row 5
    final ModularFeatureListRow reused = addRow(copy, 5, fixture.raw, 30f, null, null);
    FeatureDataUtils.applyRowDependentIntensityNormalization(copy);

    assertNull(((ModularFeature) reused.getFeature(fixture.raw)).get(NormalizedAreaType.class));
  }

  @Test
  void emptyCopyDropsAllFactorsAndReusedIdStaysUnnormalized() {
    final Fixture fixture = fixture();
    final ModularFeatureList copy = FeatureListUtils.createCopyWithRows(fixture.list, null, "copy",
        null, List.of(), false);

    assertNull(copy.getSettings().normalization());

    // a later task adds a new row with the ID of the removed row 5
    final ModularFeatureListRow reused = addRow(copy, 5, fixture.raw, 30f, null, null);
    FeatureDataUtils.applyRowDependentIntensityNormalization(copy);

    assertNull(((ModularFeature) reused.getFeature(fixture.raw)).get(NormalizedAreaType.class));
    // source keeps its own factors
    assertEquals(Map.of(5, 2d, 7, 4d), rowFactors(fixture.list).factors());
  }

  @Test
  void mergingRowDependentNormalizationIsBlocked() {
    final Fixture fixture = fixture();
    final RawDataFileImpl otherRaw = createRawFile("other", LocalDateTime.of(2026, 1, 1, 0, 1));
    final ModularFeatureList other = new ModularFeatureList("other", null, otherRaw);
    addRow(other, 1, otherRaw, 10f, null, null);

    assertThrows(IllegalStateException.class,
        () -> FeatureListUtils.transferMetadata(List.of(other, fixture.list),
            new ModularFeatureList("merged", null, fixture.raw, otherRaw), true));
    assertTrue(NormalizationRowIdentity.hasRowDependentNormalization(fixture.list));
    assertFalse(NormalizationRowIdentity.hasRowDependentNormalization(other));
  }

  @Test
  void newFeatureWithoutRowIsNormalizedOnceItIsInTheRow() {
    final Fixture fixture = fixture();
    final ModularFeatureListRow row = (ModularFeatureListRow) fixture.list.findRowByID(7);
    final RawDataFileImpl raw = fixture.raw;
    final ModularFeature feature = (ModularFeature) row.getFeature(raw);
    FeatureDataUtils.clearIntensityNormalization(feature);

    // like gap filling: the new feature is normalized before it is added to its row
    final ModularFeature newFeature = new ModularFeature(fixture.list, feature);
    newFeature.setRow(null);
    FeatureDataUtils.normalizeAbundances(newFeature,
        IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(fixture.list, raw)
            .orElseThrow());
    assertNull(newFeature.get(NormalizedAreaType.class));

    row.addFeature(raw, newFeature, false);
    assertTrue(FeatureDataUtils.applyRowDependentIntensityNormalization(fixture.list));

    assertEquals(20f * 4f, newFeature.get(NormalizedAreaType.class), 1e-4);
    assertEquals(20f * 4f, newFeature.get(NormalizedHeightType.class), 1e-4);
  }

  @Test
  void rowIndependentNormalizationDoesNotNeedTheRow() {
    final RawDataFileImpl raw = createRawFile("sample", LocalDateTime.of(2026, 1, 1, 0, 0));
    final ModularFeatureList list = new ModularFeatureList("list", null, raw);
    final ModularFeatureListRow row = addRow(list, 1, raw, 10f, null, null);
    list.setSettings(list.getSettings().withNormalization(new IntensityNormalizationSummary(
        List.of(new RawFileNormalizationFunction(raw, new FactorNormalizationFunction(3d))))));
    final ModularFeature newFeature = new ModularFeature(list, row.getFeature(raw));
    newFeature.setRow(null);

    FeatureDataUtils.normalizeAbundances(newFeature,
        IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(list, raw)
            .orElseThrow());

    assertEquals(30f, newFeature.get(NormalizedAreaType.class), 1e-4);
    assertFalse(FeatureDataUtils.applyRowDependentIntensityNormalization(list));
  }

  /**
   * Rows 5 (RT 1) and 7 (RT 2): ID order matches the default RT sorting, so renumbering maps 5->1
   * and 7->2. Row 6 was removed earlier and only has an unavailable outcome.
   */
  private static Fixture fixture() {
    final RawDataFileImpl raw = createRawFile("sample", LocalDateTime.of(2026, 1, 1, 0, 0));
    final ModularFeatureList list = new ModularFeatureList("list", null, raw);
    addRow(list, 5, raw, 10f, 10f, null, null, null, 100d, 1f, null);
    addRow(list, 7, raw, 20f, 20f, null, null, null, 200d, 2f, null);
    list.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    list.addFeatureType(DataTypes.get(NormalizedHeightType.class));
    list.setSettings(list.getSettings().withNormalization(new IntensityNormalizationSummary(
        List.of(new RawFileNormalizationFunction(raw,
            new FeatureRowNormalizationFunction(Map.of(5, 2d, 7, 4d), Map.of(6, "removed")))))));
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(list);
    return new Fixture(project, list, raw);
  }

  private static FeatureRowNormalizationFunction rowFactors(final ModularFeatureList list) {
    return (FeatureRowNormalizationFunction) list.getSettings().normalization().get(0);
  }

  private record Fixture(MZmineProjectImpl project, ModularFeatureList list, RawDataFileImpl raw) {

  }
}
