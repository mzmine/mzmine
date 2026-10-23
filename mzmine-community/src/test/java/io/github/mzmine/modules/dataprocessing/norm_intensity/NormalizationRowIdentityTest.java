/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.dataprocessing.norm_intensity;

import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.addRow;
import static io.github.mzmine.modules.dataprocessing.norm_intensity.NormIntensityTestUtils.createRawFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
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

class NormalizationRowIdentityTest {

  @Test
  void rejectsRenumberingBeforeMutationForCopiedAndInPlaceFiltering() {
    for (final OriginalFeatureListOption mode : List.of(OriginalFeatureListOption.KEEP,
        OriginalFeatureListOption.PROCESS_IN_PLACE)) {
      final Fixture fixture = fixture();
      final ParameterSet parameters = RowsFilterParameters.createDefaultAllOff();
      parameters.setParameter(RowsFilterParameters.Reset_ID, true);
      parameters.setParameter(RowsFilterParameters.handleOriginal, mode);
      final RowsFilterTask task = new RowsFilterTask(fixture.project, fixture.list, parameters, null,
          Instant.now());

      task.run();

      assertEquals(TaskStatus.ERROR, task.getStatus());
      assertEquals(2, fixture.list.getNumberOfRows());
      assertEquals(1, fixture.project.getCurrentFeatureLists().size());
    }
  }

  @Test
  void removedIdsStayReservedForLaterRowCreation() {
    final Fixture fixture = fixture();
    final ModularFeatureListRow newRow = (ModularFeatureListRow) fixture.list.getRow(1);
    final ModularFeature feature = (ModularFeature) newRow.getFeatures().getFirst();
    feature.set(NormalizedAreaType.class, 123f);

    fixture.list.setRowsApplySort(fixture.list.getRow(0));
    assertThrows(IllegalArgumentException.class, () -> fixture.list.addRow(newRow));

    assertEquals(2, newRow.getID());
    assertEquals(123f, feature.get(NormalizedAreaType.class));
  }

  @Test
  void rejectsOccupiedRowIdWithoutChangingItWhenRowDependentNormalizationIsActive() {
    final RawDataFileImpl raw = createRawFile("sample", LocalDateTime.of(2026, 1, 1, 0, 0));
    final ModularFeatureList list = new ModularFeatureList("list", null, raw);
    addRow(list, 1, raw, 10f, null, null);
    addRow(list, 2, raw, 20f, null, null);
    list.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    list.setPreferences(list.getPreferences().withActiveIntensityNormalization(
        new IntensityNormalizationSummary(List.of(new RawFileNormalizationFunction(raw,
            new FeatureRowNormalizationFunction(Map.of(1, 2d), Map.of()))))));
    final ModularFeatureListRow newRow = new ModularFeatureListRow(list, 2);
    assertThrows(IllegalArgumentException.class, () -> list.addRow(newRow));

    assertEquals(2, newRow.getID());
  }

  @Test
  void rejectsDirectAttachedRowIdMutationBeforeChangingTheSchema() {
    final Fixture fixture = fixture();

    assertThrows(IllegalStateException.class,
        () -> fixture.list.getRow(0).set(IDType.class, 2));
    assertEquals(1, fixture.list.getRow(0).getID());
  }

  @Test
  void permitsDirectIdMutationWithoutRowDependentNormalization() {
    final RawDataFileImpl raw = createRawFile("sample", LocalDateTime.of(2026, 1, 1, 0, 0));
    final ModularFeatureList list = new ModularFeatureList("list", null, raw);
    addRow(list, 1, raw, 10f, null, null);

    list.getRow(0).set(IDType.class, 2);

    assertEquals(2, list.getRow(0).getID());
  }

  @Test
  void rejectsDuplicateTargetIdsInIdentityPreservingBulkCopy() {
    final Fixture fixture = fixture();
    final ModularFeatureList target = FeatureListUtils.createCopyWithoutRows(fixture.list, "copy",
        null, 2, 2);
    final ModularFeatureListRow firstSource = (ModularFeatureListRow) fixture.list.getRow(0);
    final ModularFeatureListRow secondSource = (ModularFeatureListRow) fixture.list.getRow(1);
    final ModularFeatureListRow first = new ModularFeatureListRow(target, firstSource, true);
    final ModularFeatureListRow duplicate = new ModularFeatureListRow(target, secondSource, true);
    duplicate.set(IDType.class, firstSource.getID());

    assertThrows(IllegalArgumentException.class, () ->
        FeatureListUtils.insertCopiedRowsPreservingIdentity(List.of(firstSource, secondSource),
            Map.of(firstSource, first, secondSource, duplicate), target));
    assertEquals(0, target.getNumberOfRows());
  }

  @Test
  void scopesCopiedActiveNormalizationToSelectedRawFiles() {
    final RawDataFileImpl protectedRaw = createRawFile("protected",
        LocalDateTime.of(2026, 1, 1, 0, 0));
    final RawDataFileImpl selectedRaw = createRawFile("selected",
        LocalDateTime.of(2026, 1, 1, 0, 1));
    final ModularFeatureList source = new ModularFeatureList("source", null, protectedRaw,
        selectedRaw);
    source.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    final ModularFeatureListRow sourceRow = new ModularFeatureListRow(source, 1);
    source.addRow(sourceRow);
    source.setPreferences(source.getPreferences().withActiveIntensityNormalization(
        new IntensityNormalizationSummary(List.of(
            new RawFileNormalizationFunction(protectedRaw,
                new FeatureRowNormalizationFunction(Map.of(1, 2d), Map.of())),
            new RawFileNormalizationFunction(selectedRaw, new FactorNormalizationFunction(2d))))));

    final ModularFeatureList target = FeatureListUtils.createCopy(source, null, "split", null,
        false, List.of(selectedRaw), false, null, null);

    assertFalse(NormalizationRowIdentity.hasActiveRowDependentNormalization(target));
    assertTrue(IntensityNormalizerModule.getNormalizationFunctionsOfLatestCallForFile(target,
        selectedRaw).isPresent());
    final ModularFeatureListRow copied = new ModularFeatureListRow(target, sourceRow, true);
    FeatureListUtils.insertCopiedRowsPreservingIdentity(List.of(sourceRow),
        Map.of(sourceRow, copied), target);
    assertEquals(1, target.getNumberOfRows());
  }

  private static Fixture fixture() {
    final RawDataFileImpl raw = createRawFile("sample", LocalDateTime.of(2026, 1, 1, 0, 0));
    final ModularFeatureList list = new ModularFeatureList("list", null, raw);
    addRow(list, 1, raw, 10f, null, null);
    addRow(list, 2, raw, 20f, null, null);
    final IntensityNormalizerParameters parameters = new IntensityNormalizerParameters();
    parameters.setParameter(IntensityNormalizerParameters.hiddenNormalizationSummary,
        new IntensityNormalizationSummary(List.of(new RawFileNormalizationFunction(raw,
            new FeatureRowNormalizationFunction(Map.of(1, 2d), Map.of(2, "removed"))))));
    list.addDescriptionOfAppliedTask(new SimpleFeatureListAppliedMethod(IntensityNormalizerModule.class,
        parameters, Instant.now()));
    list.addFeatureType(DataTypes.get(NormalizedAreaType.class));
    list.setPreferences(list.getPreferences().withActiveIntensityNormalization(
        parameters.getValue(IntensityNormalizerParameters.hiddenNormalizationSummary)));
    final MZmineProjectImpl project = new MZmineProjectImpl();
    project.addFeatureList(list);
    return new Fixture(project, list);
  }

  private record Fixture(MZmineProjectImpl project, ModularFeatureList list) {}
}
