package io.github.mzmine.modules.io.export_features_mztabm;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies that interactive exports start beside the current project and require a destination.
 */
class MZTabmExportParametersTest {

  @TempDir
  Path tempDir;

  /**
   * Switching saved projects must not retain a destination from the previously opened project.
   */
  @Test
  void startsEachDialogInTheCurrentProjectDirectory() {
    final MZTabmExportParameters parameters = (MZTabmExportParameters) new MZTabmExportParameters()
        .cloneParameterSet();
    final Path firstProject = tempDir.resolve("first/project.mzmine");
    final Path secondProject = tempDir.resolve("second/project.mzmine");

    parameters.initializeOutputFile(firstProject.toFile());

    Assertions.assertEquals(firstProject.getParent().resolve("{}_quant.mzTab").toFile(),
        parameters.getValue(MZTabmExportParameters.filename));

    parameters.initializeOutputFile(secondProject.toFile());

    Assertions.assertEquals(secondProject.getParent().resolve("{}_quant.mzTab").toFile(),
        parameters.getValue(MZTabmExportParameters.filename));
  }

  /**
   * A project supplied as a relative filename still has an absolute export destination.
   */
  @Test
  void resolvesRelativeProjectFilesToAnAbsoluteDirectory() {
    final MZTabmExportParameters parameters = (MZTabmExportParameters) new MZTabmExportParameters()
        .cloneParameterSet();
    final File projectFile = new File("project.mzmine");

    parameters.initializeOutputFile(projectFile);

    Assertions.assertEquals(new File(projectFile.getAbsoluteFile().getParentFile(), "{}_quant.mzTab"),
        parameters.getValue(MZTabmExportParameters.filename));
    Assertions.assertTrue(parameters.getValue(MZTabmExportParameters.filename).isAbsolute());
  }

  /**
   * An unsaved project requires a user choice instead of exporting to a remembered destination.
   */
  @Test
  void requiresAnOutputFileForUnsavedProjects() {
    final MZTabmExportParameters parameters = (MZTabmExportParameters) new MZTabmExportParameters()
        .cloneParameterSet();
    parameters.setParameter(MZTabmExportParameters.filename, tempDir.resolve("previous.mzTab").toFile());
    final List<String> errors = new ArrayList<>();

    parameters.initializeOutputFile(null);

    Assertions.assertNull(parameters.getValue(MZTabmExportParameters.filename));
    Assertions.assertFalse(parameters.getParameter(MZTabmExportParameters.filename).checkValue(errors));
    Assertions.assertFalse(errors.isEmpty());

    parameters.setParameter(MZTabmExportParameters.filename, tempDir.resolve("chosen.mzTab").toFile());

    Assertions.assertTrue(parameters.getParameter(MZTabmExportParameters.filename)
        .checkValue(new ArrayList<>()));
  }
}
