package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.DoubleMetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lifstools.mztab2.model.Assay;
import org.lifstools.mztab2.model.StudyVariable;
import org.mockito.Mockito;

/**
 * Verifies selected metadata grouping independently of active project or UI state.
 */
class MzTabmStudyVariablesTest {

  /**
   * Group order follows numbers rather than labels and excludes files outside the exported assays.
   */
  @Test
  void groupsExportedFilesInNumericFactorOrderAndKeepsZero() {
    final MetadataTable metadata = new MetadataTable(false);
    final DoubleMetadataColumn factor = new DoubleMetadataColumn("factor");
    final Map<RawDataFile, Assay> assays = createAssays(4);
    final List<RawDataFile> files = List.copyOf(assays.keySet());
    metadata.setValue(factor, files.get(0), 10d);
    metadata.setValue(factor, files.get(1), 0d);
    metadata.setValue(factor, files.get(2), 2d);
    metadata.setValue(factor, files.get(3), 2d);
    metadata.setValue(factor, Mockito.mock(RawDataFile.class), 1d);
    final long originalVersion = metadata.getVersion();

    final List<StudyVariable> variables = MzTabmStudyVariables.create(metadata, assays, factor);

    Assertions.assertEquals(List.of("0.0", "2.0", "10.0"),
        variables.stream().map(StudyVariable::getName).toList());
    Assertions.assertEquals(List.of(1, 2, 3),
        variables.stream().map(StudyVariable::getId).toList());
    Assertions.assertEquals(List.of(2), assayIds(variables.get(0)));
    Assertions.assertEquals(List.of(3, 4), assayIds(variables.get(1)));
    Assertions.assertEquals(List.of(1), assayIds(variables.get(2)));
    Assertions.assertEquals("factor", variables.get(0).getFactors().getFirst().getName());
    Assertions.assertEquals("0.0", variables.get(0).getFactors().getFirst().getValue());
    Assertions.assertEquals(originalVersion, metadata.getVersion());
  }

  /**
   * An explicit factor parameter allows a single real group to retain its actual value as its name.
   */
  @Test
  void retainsTheActualValueForASingleKnownFactor() {
    final MetadataTable metadata = new MetadataTable(false);
    final DoubleMetadataColumn factor = new DoubleMetadataColumn("factor");
    final Map<RawDataFile, Assay> assays = createAssays(2);
    for (final RawDataFile file : assays.keySet()) {
      metadata.setValue(factor, file, 1.5d);
    }

    final List<StudyVariable> variables = MzTabmStudyVariables.create(metadata, assays, factor);

    Assertions.assertEquals(1, variables.size());
    final StudyVariable variable = variables.getFirst();
    Assertions.assertEquals("1.5", variable.getName());
    Assertions.assertEquals("factor = 1.5", variable.getDescription());
    Assertions.assertEquals("factor", variable.getFactors().getFirst().getName());
    Assertions.assertEquals("1.5", variable.getFactors().getFirst().getValue());
    Assertions.assertEquals(List.of(1, 2), assayIds(variable));
    Assertions.assertEquals("MS:1002962", variable.getAverageFunction().getCvAccession());
    Assertions.assertEquals("MS:1002963", variable.getVariationFunction().getCvAccession());
  }

  /**
   * Missing factor values form an explicit final group rather than removing their assays.
   */
  @Test
  void preservesPartiallyMissingFactorsInUndefinedGroup() {
    final MetadataTable metadata = new MetadataTable(false);
    final DoubleMetadataColumn factor = new DoubleMetadataColumn("factor");
    final Map<RawDataFile, Assay> assays = createAssays(3);
    final List<RawDataFile> files = List.copyOf(assays.keySet());
    metadata.setValue(factor, files.get(1), 2d);

    final List<StudyVariable> variables = MzTabmStudyVariables.create(metadata, assays, factor);

    Assertions.assertEquals(List.of("2.0", "undefined"),
        variables.stream().map(StudyVariable::getName).toList());
    Assertions.assertEquals(List.of(2), assayIds(variables.get(0)));
    Assertions.assertEquals(List.of(1, 3), assayIds(variables.get(1)));
    Assertions.assertTrue(variables.get(1).getFactors().isEmpty());
  }

  /**
   * No grouping uses every assay without falling back to an existing factor column.
   */
  @Test
  void fallsBackToAllAssaysWhenNoColumnIsSelected() {
    final MetadataTable metadata = new MetadataTable(false);
    final DoubleMetadataColumn factor = new DoubleMetadataColumn("factor");
    final Map<RawDataFile, Assay> assays = createAssays(2);
    final List<RawDataFile> files = List.copyOf(assays.keySet());
    metadata.setValue(factor, files.get(0), 1.5d);
    metadata.setValue(factor, files.get(1), 2d);

    final List<StudyVariable> variables = MzTabmStudyVariables.create(metadata, assays, null);

    Assertions.assertEquals(1, variables.size());
    Assertions.assertEquals("undefined", variables.getFirst().getName());
    Assertions.assertEquals(List.of(1, 2), assayIds(variables.getFirst()));
    Assertions.assertTrue(variables.getFirst().getFactors().isEmpty());
    Assertions.assertTrue(variables.getFirst().getDescription().contains("no metadata grouping column"));
  }

  /**
   * The selected condition column takes precedence over an unrelated factor column.
   */
  @Test
  void groupsBySelectedConditionInsteadOfFactor() {
    final MetadataTable metadata = new MetadataTable(false);
    final StringMetadataColumn condition = new StringMetadataColumn("condition");
    final DoubleMetadataColumn factor = new DoubleMetadataColumn("factor");
    final Map<RawDataFile, Assay> assays = createAssays(4);
    final List<RawDataFile> files = List.copyOf(assays.keySet());
    for (int index = 0; index < files.size(); index++) {
      metadata.setValue(condition, files.get(index), index % 2 == 0 ? "control" : "treated");
      metadata.setValue(factor, files.get(index), 1d);
    }

    final List<StudyVariable> variables = MzTabmStudyVariables.create(metadata, assays, condition);

    Assertions.assertEquals(List.of("control", "treated"),
        variables.stream().map(StudyVariable::getName).toList());
    Assertions.assertEquals(List.of(1, 3), assayIds(variables.get(0)));
    Assertions.assertEquals(List.of(2, 4), assayIds(variables.get(1)));
    Assertions.assertEquals("condition = control", variables.get(0).getDescription());
    Assertions.assertEquals("condition", variables.get(0).getFactors().getFirst().getName());
    Assertions.assertEquals("control", variables.get(0).getFactors().getFirst().getValue());
  }

  /**
   * A captured selection is resolved against the supplied project's actual column and values.
   */
  @Test
  void resolvesTheSelectionAgainstTheSuppliedProjectMetadata() {
    final MetadataTable metadata = new MetadataTable(false);
    final StringMetadataColumn condition = new StringMetadataColumn("condition");
    final Map<RawDataFile, Assay> assays = createAssays(2);
    final List<RawDataFile> files = List.copyOf(assays.keySet());
    metadata.setValue(condition, files.get(0), "control");
    metadata.setValue(condition, files.get(1), "treated");
    final DoubleMetadataColumn capturedColumn = new DoubleMetadataColumn("condition");

    final List<StudyVariable> variables = MzTabmStudyVariables.create(metadata, assays, capturedColumn);

    Assertions.assertEquals(List.of("control", "treated"),
        variables.stream().map(StudyVariable::getName).toList());
    Assertions.assertEquals(List.of(1), assayIds(variables.get(0)));
    Assertions.assertEquals(List.of(2), assayIds(variables.get(1)));
    Assertions.assertEquals("condition", variables.getFirst().getFactors().getFirst().getName());
  }

  /**
   * Removing the selected column produces an undefined group rather than a factor fallback.
   */
  @Test
  void fallsBackToAllAssaysWhenTheSelectedColumnIsRemoved() {
    final MetadataTable metadata = new MetadataTable(false);
    final StringMetadataColumn condition = new StringMetadataColumn("condition");
    final DoubleMetadataColumn factor = new DoubleMetadataColumn("factor");
    final Map<RawDataFile, Assay> assays = createAssays(2);
    final List<RawDataFile> files = List.copyOf(assays.keySet());
    metadata.setValue(condition, files.get(0), "control");
    metadata.setValue(condition, files.get(1), "treated");
    metadata.setValue(factor, files.get(0), 1.5d);
    metadata.setValue(factor, files.get(1), 2d);
    metadata.removeColumn(condition);

    final List<StudyVariable> variables = MzTabmStudyVariables.create(metadata, assays, condition);

    Assertions.assertEquals(1, variables.size());
    Assertions.assertEquals("undefined", variables.getFirst().getName());
    Assertions.assertEquals(List.of(1, 2), assayIds(variables.getFirst()));
    Assertions.assertTrue(variables.getFirst().getFactors().isEmpty());
    Assertions.assertTrue(variables.getFirst().getDescription().contains("condition"));
    Assertions.assertTrue(variables.getFirst().getDescription().contains("unavailable"));
  }

  /**
   * An existing column with no assigned values remains an undefined group containing every assay.
   */
  @Test
  void preservesAllAssaysWhenEveryFactorIsMissing() {
    final MetadataTable metadata = new MetadataTable(false);
    final DoubleMetadataColumn factor = new DoubleMetadataColumn("factor");
    metadata.addColumn(factor);
    final Map<RawDataFile, Assay> assays = createAssays(2);

    final List<StudyVariable> variables = MzTabmStudyVariables.create(metadata, assays, factor);

    Assertions.assertEquals(1, variables.size());
    Assertions.assertEquals("undefined", variables.getFirst().getName());
    Assertions.assertEquals(List.of(1, 2), assayIds(variables.getFirst()));
    Assertions.assertTrue(variables.getFirst().getFactors().isEmpty());
  }

  /**
   * Numeric text sorts naturally while blank strings remain missing and nonnumeric values survive.
   */
  @Test
  void preservesTextFactorsAndGroupsBlankValuesAsMissing() {
    final MetadataTable metadata = new MetadataTable(false);
    final StringMetadataColumn factor = new StringMetadataColumn("treatment");
    final Map<RawDataFile, Assay> assays = createAssays(4);
    final List<RawDataFile> files = List.copyOf(assays.keySet());
    metadata.setValue(factor, files.get(0), "10");
    metadata.setValue(factor, files.get(1), "2");
    metadata.setValue(factor, files.get(2), "control");
    metadata.setValue(factor, files.get(3), " ");

    final List<StudyVariable> variables = MzTabmStudyVariables.create(metadata, assays, factor);

    Assertions.assertEquals(List.of("2", "10", "control", "undefined"),
        variables.stream().map(StudyVariable::getName).toList());
    Assertions.assertEquals("control", variables.get(2).getFactors().getFirst().getValue());
    Assertions.assertEquals("treatment", variables.get(2).getFactors().getFirst().getName());
    Assertions.assertEquals(List.of(4), assayIds(variables.get(3)));
  }

  /**
   * No study-variable group is invented when no assays are exported.
   */
  @Test
  void returnsNoGroupsForAnEmptyExport() {
    Assertions.assertTrue(MzTabmStudyVariables.create(new MetadataTable(false), Map.of(), null).isEmpty());
  }

  /**
   * Creates isolated files with a stable assay order.
   *
   * @param count number of exported assays
   * @return raw file to assay mapping in identifier order
   */
  private static @NotNull Map<RawDataFile, Assay> createAssays(final int count) {
    final Map<RawDataFile, Assay> assays = new LinkedHashMap<>();
    for (int id = 1; id <= count; id++) {
      assays.put(Mockito.mock(RawDataFile.class), new Assay().id(id));
    }
    return assays;
  }

  /**
   * Extracts group membership without comparing the mutable mzTab assay model itself.
   *
   * @param variable exported study variable
   * @return assay identifiers in their exported order
   */
  private static @NotNull List<Integer> assayIds(@NotNull final StudyVariable variable) {
    return variable.getAssayRefs().stream().map(Assay::getId).toList();
  }
}
