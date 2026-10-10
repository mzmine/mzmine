package io.github.mzmine.modules.tools.batchwizard.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.tools.batchwizard.BatchWizardCreateBatchChecker;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.CustomizationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.DataImportWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.FilterWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testutils.MZmineTestUtil;

class WizardSequenceIOUtilsTest {

  @BeforeAll
  static void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  @Test
  void portableSaveExcludesFileSelectionsAndCustomizationFileOverrides(@TempDir Path tempDir)
      throws Exception {
    final File rawFile = tempDir.resolve("private-data.mzML").toFile();
    final File overrideFile = tempDir.resolve("private-library.csv").toFile();
    final var data = DataImportWizardParameterFactory.Data.create();
    data.setParameter(DataImportWizardParameters.fileNames, new File[]{rawFile});
    data.setParameter(DataImportWizardParameters.metadataFile, true, overrideFile);
    final var customization = CustomizationWizardParameters.createDefault();
    final var fileOverride = new ParameterOverride("module", "module", new FileNameParameter(
        "Library", "", FileSelectionType.OPEN), overrideFile, ApplicationScope.ALL);
    final File privateDirectory = tempDir.resolve("private-study-directory").toFile();
    final var directoryOverride = new ParameterOverride("module", "module",
        new io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter("Folder", ""),
        privateDirectory, ApplicationScope.ALL);
    customization.setParameter(CustomizationWizardParameters.overrides, List.of(fileOverride, directoryOverride));

    final File portable = tempDir.resolve("portable.mzmwizard").toFile();
    WizardSequenceIOUtils.saveToFile(List.of(data, customization), portable, true);
    final String portableXml = Files.readString(portable.toPath());

    assertFalse(portableXml.contains(rawFile.getPath()));
    assertFalse(portableXml.contains(overrideFile.getPath()));
    assertFalse(portableXml.contains(privateDirectory.getPath()));
    assertFalse(portableXml.contains("name=\"Metadata file\" selected=\"true\""));
    assertEquals(rawFile, data.getValue(DataImportWizardParameters.fileNames)[0]);
    assertEquals(2, customization.getValue(CustomizationWizardParameters.overrides).size());

    final File complete = tempDir.resolve("complete.mzmwizard").toFile();
    WizardSequenceIOUtils.saveToFile(List.of(data, customization), complete, false);
    final String completeXml = Files.readString(complete.toPath());
    assertTrue(completeXml.contains(rawFile.getPath()));
    assertTrue(completeXml.contains(overrideFile.getPath()));
    assertTrue(completeXml.contains(privateDirectory.getPath()));
  }

  @Test
  void warningsAreCachedAndStableAcrossCalls() {
    final WizardSequence sequence = new WizardSequence();
    sequence.add(DataImportWizardParameterFactory.Data.create());
    sequence.add(FilterWizardParameterFactory.Filters.create());
    final BatchWizardCreateBatchChecker checker = new BatchWizardCreateBatchChecker(sequence);

    final List<String> first = checker.warnings();
    final List<String> second = checker.warnings();

    assertSame(first, second);
  }

  @Test
  void nativeSaveDefaultsToPortablePreset() {
    assertFalse(new WizardSequenceSaveParameters().getValue(
        WizardSequenceSaveParameters.includeFileSelections));
  }
}
