package io.github.mzmine.modules.io.rawdataimport;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFileWriter;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.io.rawdataimport.fileformats.NativeFileReadTask;
import io.github.mzmine.modules.io.rawdataimport.fileformats.bruker.BrukerTimsTofReadTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class RawDataImportFolderModule implements MZmineProcessingModule {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private static final String MODULE_NAME = "Raw data folder import";
  private static final String MODULE_DESCRIPTION = "This module imports raw data from folders into the project.";

  @Override
  public @Nonnull
  String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull
  String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(final @Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    File fileNames[] = parameters.getParameter(RawDataImportFolderParameters.fileNames).getValue();

    for (int i = 0; i < fileNames.length; i++) {
      if (fileNames[i] == null) {
        return ExitCode.OK;
      }

      if ((!fileNames[i].exists()) || (!fileNames[i].canRead())) {
        MZmineCore.getDesktop().displayErrorMessage("Cannot read file " + fileNames[i]);
        logger.warning("Cannot read file " + fileNames[i]);
        return ExitCode.ERROR;
      }

      RawDataFileWriter newMZmineFile;
      try {
        newMZmineFile = MZmineCore.createNewFile(fileNames[i].getName());
      } catch (IOException e) {
        MZmineCore.getDesktop().displayErrorMessage("Could not create a new temporary file " + e);
        logger.log(Level.SEVERE, "Could not create a new temporary file ", e);
        return ExitCode.ERROR;
      }

      RawDataFileType fileType = RawDataFileTypeDetector.detectDataFileType(fileNames[i]);
      logger.finest("File " + fileNames[i] + " type detected as " + fileType);

      if (fileType == null) {
        MZmineCore.getDesktop()
            .displayErrorMessage("Could not determine the file type of file " + fileNames[i]);
        continue;
      }

      Task newTask = createOpeningTask(fileType, project, fileNames[i], newMZmineFile);

      if (newTask == null) {
        logger.warning("File type " + fileType + " of file " + fileNames[i] + " is not supported.");
        return ExitCode.ERROR;
      }

      tasks.add(newTask);

    }

    return ExitCode.OK;
  }

  @Override
  public @Nonnull
  MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.RAWDATA;
  }

  @Override
  public @Nonnull
  Class<? extends ParameterSet> getParameterSetClass() {
    return RawDataImportFolderParameters.class;
  }

  public static Task createOpeningTask(RawDataFileType fileType, MZmineProject project,
      File fileName, RawDataFileWriter newMZmineFile) {
    Task newTask = null;
    switch (fileType) {
      case BRUKER_TIMS_TOF:
        System.out.println("Bruker Import starts here!");
        newTask = new BrukerTimsTofReadTask(project, fileName, fileType, newMZmineFile);
        break;
      case WATERS_RAW:
        newTask = new NativeFileReadTask(project, fileName, fileType, newMZmineFile);
        break;

    }
    return newTask;
  }

}