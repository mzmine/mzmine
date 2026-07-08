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

package io.github.mzmine.modules.io.export_features_massdynamics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.PreferredAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesIsomericStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.numbers.NormalizedAreaType;
import io.github.mzmine.modules.dataanalysis.utils.imputation.ImputationFunctions;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.StringMetadataColumn;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.RawDataFileImpl;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.FeatureListTestUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testutils.MZmineTestUtil;

class MassDynamicsExportTaskTest {

  private static final String[] HEADER = {"MetaboliteId", "MetaboliteName", "Smiles",
      "IsomericSmiles", "InChI", "InChIKey", "HMDB", "KEGG", "mz", "RetentionTime", "SampleName",
      "MetaboliteIntensity", "Imputed"};

  @BeforeAll
  static void startMzmine() {
    MZmineTestUtil.startMzmineCore();
  }

  @BeforeEach
  void cleanProject() {
    MZmineTestUtil.cleanProject();
  }

  @Test
  void exportsMassDynamicsFiles(@TempDir final Path tempDir) throws Exception {
    final RawDataFileImpl fileA = new RawDataFileImpl("sample_A.mzML", null, null);
    final RawDataFileImpl fileB = new RawDataFileImpl("sample_B.mzML", null, null);
    final List<RawDataFileImpl> files = List.of(fileA, fileB);
    final MZmineProject project = ProjectService.getProject();
    project.addFile(fileA);
    project.addFile(fileB);

    final MetadataTable metadata = project.getProjectMetadata();
    final MetadataColumn<String> treatment = new StringMetadataColumn("treatment");
    metadata.setValue(treatment, fileA, "Tumor");

    final ModularFeatureList featureList = new ModularFeatureList("Feature/List 1", null, fileA,
        fileB);
    project.addFeatureList(featureList);
    final ModularFeatureListRow annotatedRow = FeatureListTestUtils.addRow(featureList, 1, files,
        List.of(10f, 20f), 101.1234, 5.5f);
    setNormalizedArea(annotatedRow, fileA, 11f);
    setNormalizedArea(annotatedRow, fileB, 22f);
    final CompoundDBAnnotation annotation = createAnnotation();
    annotatedRow.set(PreferredAnnotationType.class, annotation);

    final ModularFeatureListRow missingRow = FeatureListTestUtils.addRow(featureList, 2, files,
        Arrays.asList(5f, null), 202.2, 6.5f);
    setNormalizedArea(missingRow, fileA, 55f);

    final File baseFile = tempDir.resolve("massdynamics.tsv").toFile();
    final File metaboliteFile = FileAndPathUtil.getRealFilePathWithSuffix(baseFile,
        "_massdynamics_metabolite.tsv");
    final File metadataFile = MassDynamicsExportTask.getExperimentMetadataFile(baseFile);

    final ParameterSet parameters = new MassDynamicsExportParameters().cloneParameterSet();
    parameters.setParameter(MassDynamicsExportParameters.featureLists,
        new FeatureListsSelection(featureList));
    parameters.setParameter(MassDynamicsExportParameters.filename, baseFile);
    parameters.setParameter(MassDynamicsExportParameters.abundanceMeasure,
        AbundanceMeasure.NORMALIZED_AREA);
    parameters.setParameter(MassDynamicsExportParameters.missingValueImputation,
        ImputationFunctions.OneFifthOfMinimum);
    parameters.setParameter(MassDynamicsExportParameters.conditionColumn, "treatment");
    parameters.setParameter(MassDynamicsExportParameters.defaultCondition, true, "Unknown");

    final MassDynamicsExportTask task = new MassDynamicsExportTask(null, Instant.now(), parameters,
        MassDynamicsExportModule.class, featureList);
    task.run();

    assertEquals(TaskStatus.FINISHED, task.getStatus(), task.getErrorMessage());
    final List<String[]> metaboliteRows = CSVParsingUtils.readData(metaboliteFile, "\t");
    assertEquals(5, metaboliteRows.size());
    assertArrayEquals(HEADER, metaboliteRows.getFirst());

    assertArrayEquals(
        new String[]{"row_1", "Glucose", annotation.getSmiles(), annotation.getIsomericSmiles(),
            annotation.getInChI(), annotation.getInChIKey(), "", "", "101.1234", "5.5", "sample_A",
            "11.0", "0"}, metaboliteRows.get(1));
    assertEquals("22.0", metaboliteRows.get(2)[11]);
    assertEquals("0", metaboliteRows.get(2)[12]);
    assertEquals("sample_B", metaboliteRows.get(4)[10]);
    assertEquals("11.0", metaboliteRows.get(4)[11]);
    assertEquals("1", metaboliteRows.get(4)[12]);

    assertMetadataCsv(metadataFile);
  }

  private static void setNormalizedArea(@NotNull final ModularFeatureListRow row,
      @NotNull final RawDataFile file, final float value) {
    final ModularFeature feature = (ModularFeature) row.getFeature(file);
    assertNotNull(feature);
    feature.set(NormalizedAreaType.class, value);
  }

  private static @NotNull CompoundDBAnnotation createAnnotation() {
    final CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();
    annotation.put(CompoundNameType.class, "Glucose");
    annotation.put(InChIKeyStructureType.class, "WQZGKKKJIJFFOK-UHFFFAOYSA-N");
    annotation.put(SmilesStructureType.class, "C(C1C(C(C(C(O1)O)O)O)O)O");
    annotation.put(SmilesIsomericStructureType.class, "C(C1C(C(C(C(O1)O)O)O)O)O");
    annotation.put(InChIStructureType.class, "InChI=1S/C6H12O6");
    return annotation;
  }

  private static void assertMetadataCsv(@NotNull final File metadataFile) throws Exception {
    assertTrue(metadataFile.exists(), metadataFile::getAbsolutePath);
    final List<String[]> rows = CSVParsingUtils.readData(metadataFile, ",");
    assertEquals(3, rows.size());
    final List<String> header = Arrays.asList(rows.getFirst());
    final int filenameIndex = header.indexOf("filename");
    final int sampleNameIndex = header.indexOf("sample_name");
    final int conditionIndex = header.indexOf("condition");
    assertTrue(filenameIndex >= 0, header::toString);
    assertTrue(sampleNameIndex >= 0, header::toString);
    assertTrue(conditionIndex >= 0, header::toString);

    assertEquals("sample_A.mzML", rows.get(1)[filenameIndex]);
    assertEquals("sample_A", rows.get(1)[sampleNameIndex]);
    assertEquals("Tumor", rows.get(1)[conditionIndex]);
    assertEquals("sample_B.mzML", rows.get(2)[filenameIndex]);
    assertEquals("sample_B", rows.get(2)[sampleNameIndex]);
    assertEquals("Unknown", rows.get(2)[conditionIndex]);
  }
}
