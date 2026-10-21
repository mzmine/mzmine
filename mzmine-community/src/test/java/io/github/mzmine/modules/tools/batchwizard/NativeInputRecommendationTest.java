package io.github.mzmine.modules.tools.batchwizard;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.DataImportWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation.AnalysisInputSelectionMode;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataPreparation.WizardAnalysisInput;
import java.io.File;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class NativeInputRecommendationTest {
  @Test void reusesNativeQcRecommendationWithoutRestrictingInputArray() {
    final File[] dataset = IntStream.range(0, 25).mapToObj(i -> new File(
        (i < 5 ? "QC_" : "sample_") + i + ".mzML")).toArray(File[]::new);
    final File[] selected = RawDataPreparation.selectOptimizerInputFiles(dataset);
    assertEquals(5, selected.length);
    assertTrue(Arrays.stream(selected).allMatch(file -> file.getName().startsWith("QC_")));
    assertEquals(25, dataset.length);
  }
  @Test void emptyAndSmallDataSetsNeedNoFallbackLimits() {
    assertEquals(0, RawDataPreparation.selectOptimizerInputFiles(new File[0]).length);
    assertEquals(2, RawDataPreparation.selectOptimizerInputFiles(new File[]{new File("sample1"), new File("sample2")}).length);
  }

  @Test void wizardAnalysisInputKeepsCompleteDataSelectionAndUsesRepresentativeSubset() {
    final File[] dataFiles = IntStream.range(0, 25).mapToObj(i -> new File(
        (i < 5 ? "QC_" : "sample_") + i + ".mzML")).toArray(File[]::new);
    final File metadataFile = new File("metadata.csv");
    final WizardSequence sequence = sequenceWithDataFiles(dataFiles, metadataFile);

    final WizardAnalysisInput input = RawDataPreparation.selectWizardAnalysisInput(sequence, 3,
        AnalysisInputSelectionMode.REPRESENTATIVE);

    assertArrayEquals(dataFiles, input.dataFiles());
    assertArrayEquals(dataFiles, sequence.get(WizardPart.DATA_IMPORT).orElseThrow()
        .getValue(DataImportWizardParameters.fileNames));
    assertEquals(5, input.analysisFiles().length);
    assertTrue(Arrays.stream(input.analysisFiles())
        .allMatch(file -> file.getName().startsWith("QC_")));
    assertEquals(metadataFile, input.metadataFile());

    final File[] copiedDataFiles = input.dataFiles();
    copiedDataFiles[0] = new File("changed.mzML");
    assertEquals(dataFiles[0], input.dataFiles()[0]);
    final File[] copiedAnalysisFiles = input.analysisFiles();
    copiedAnalysisFiles[0] = new File("changed.mzML");
    assertTrue(input.analysisFiles()[0].getName().startsWith("QC_"));
  }

  @Test void wizardAnalysisInputAllowsExplicitAllFilesMode() {
    final File[] dataFiles = IntStream.range(0, 12)
        .mapToObj(i -> new File("sample_" + i + ".mzML")).toArray(File[]::new);

    final WizardAnalysisInput input = RawDataPreparation.selectWizardAnalysisInput(
        sequenceWithDataFiles(dataFiles, null), 3, AnalysisInputSelectionMode.ALL);

    assertArrayEquals(dataFiles, input.analysisFiles());
  }

  @Test void wizardAnalysisInputReportsUnavailableOrInsufficientData() {
    final IllegalStateException noDataStep = assertThrows(IllegalStateException.class,
        () -> RawDataPreparation.selectWizardAnalysisInput(new WizardSequence(), 1,
            AnalysisInputSelectionMode.REPRESENTATIVE));
    assertEquals("The wizard has no data import step.", noDataStep.getMessage());

    final IllegalStateException tooFewFiles = assertThrows(IllegalStateException.class,
        () -> RawDataPreparation.selectWizardAnalysisInput(
            sequenceWithDataFiles(new File[]{new File("sample.mzML")}, null), 3,
            AnalysisInputSelectionMode.REPRESENTATIVE));
    assertTrue(tooFewFiles.getMessage().startsWith("This requires at least 3 raw data files."));

    final IllegalStateException invalidFile = assertThrows(IllegalStateException.class,
        () -> RawDataPreparation.selectWizardAnalysisInput(
            sequenceWithDataFiles(new File[]{new File("sample.mzML"), null}, null), 1,
            AnalysisInputSelectionMode.REPRESENTATIVE));
    assertEquals("The Data Import step contains an invalid raw data file.", invalidFile.getMessage());
  }

  private static WizardSequence sequenceWithDataFiles(File[] dataFiles, File metadataFile) {
    final var data = DataImportWizardParameterFactory.Data.create();
    data.setParameter(DataImportWizardParameters.fileNames, dataFiles);
    if (metadataFile != null) {
      data.setParameter(DataImportWizardParameters.metadataFile, true, metadataFile);
    }
    final WizardSequence sequence = new WizardSequence();
    sequence.add(data);
    return sequence;
  }
}
