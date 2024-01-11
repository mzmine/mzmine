/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.io.projectsave;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.ExceptionUtils;
import io.github.mzmine.util.StreamCopy;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

public class ProjectSavingTask extends AbstractTask {

  public static final String VERSION_FILENAME = "MZMINE_VERSION";
  public static final String STANDALONE_FILENAME = "STANDALONE"; // only exists if it's a standalone project.
  public static final String CONFIG_FILENAME = "configuration.xml";
  public static final String PARAMETERS_FILENAME = "User parameters.xml";
  private static final Logger logger = Logger.getLogger(ProjectSavingTask.class.getName());
  private final ProjectSaveOption projectType;

  private final File saveFile;
  private final MZmineProjectImpl savedProject;
  private final int totalSaveItems;
  private final int finishedSaveItems = 0;
  // This hashtable maps raw data files to their ID within the saved project
  private final Hashtable<RawDataFile, String> dataFilesIDMap;
  private RawDataFileSaveHandler rawDataFileSaveHandler;
  private PeakListSaveHandler peakListSaveHandler;
  private UserParameterSaveHandler userParameterSaveHandler;
  private int currentStage;
  private String currentSavedObjectName;

  public ProjectSavingTask(MZmineProject project, ParameterSet parameters,
      @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.savedProject = (MZmineProjectImpl) project;
    this.saveFile = parameters.getValue(ProjectSaveAsParameters.projectFile);
    this.projectType = parameters.getValue(ProjectSaveAsParameters.option);
    dataFilesIDMap = new Hashtable<>();
    this.totalSaveItems = project.getDataFiles().length + project.getCurrentFeatureLists().size();
  }

  @Override
  public String getTaskDescription() {
    if (currentSavedObjectName == null) {
      return "Saving project";
    }
    return "Saving project (" + currentSavedObjectName + ")";
  }

  @Override
  public double getFinishedPercentage() {

    if (totalSaveItems == 0) {
      return 0.0;
    }

    double currentItemProgress = 0.0;

    switch (currentStage) {
      case 2:
        if (rawDataFileSaveHandler != null) {
          currentItemProgress = rawDataFileSaveHandler.getProgress();
        }
        break;
      case 3:
        if (peakListSaveHandler != null) {
          currentItemProgress = peakListSaveHandler.getProgress();
        }
        break;
      case 4:
      case 5:
        return 1.0;
      default:
        return 0;
    }

    return (finishedSaveItems + currentItemProgress) / totalSaveItems;
  }

  @Override
  public void cancel() {

    logger.info("Canceling saving of project to " + saveFile);

    setStatus(TaskStatus.CANCELED);

    if (rawDataFileSaveHandler != null) {
      rawDataFileSaveHandler.cancel();
    }

    if (peakListSaveHandler != null) {
      peakListSaveHandler.cancel();
    }

    if (userParameterSaveHandler != null) {
      userParameterSaveHandler.cancel();
    }

  }

  @Override
  public void run() {
    try {
      logger.info("Saving project to " + saveFile);
      setStatus(TaskStatus.PROCESSING);

      switch (projectType) {
        case STANDALONE -> savedProject.setStandalone(true);
        case REFERENCING -> savedProject.setStandalone(false);
      }

      // Prepare a temporary ZIP file. We create this file in the same
      // directory as the final saveFile to avoid moving between
      // filesystems in the last stage (renameTo)

      // checking if directory exists first
      if (!FileAndPathUtil.createDirectory(saveFile.getParentFile())) {
        setErrorMessage("Could not create directories for file " + saveFile + " for writing.");
        setStatus(TaskStatus.ERROR);
        return;
      }

      File tempFile = File.createTempFile(saveFile.getName(), ".tmp", saveFile.getParentFile());
      tempFile.deleteOnExit();

      // Create a ZIP stream writing to the temporary file
      FileOutputStream tempStream = new FileOutputStream(tempFile);
      ZipOutputStream zipStream = new ZipOutputStream(tempStream);

      // Stage 1 - save version and configuration
      currentStage++;
      saveVersion(zipStream);
      saveStandalone(zipStream);
      saveConfiguration(zipStream);
      if (isCanceled()) {
        zipStream.close();
        tempFile.delete();
        return;
      }

      // Stage 2 - save RawDataFile objects
      currentStage++;
      saveRawDataFiles(zipStream);
      if (isCanceled()) {
        zipStream.close();
        tempFile.delete();
        return;
      }

      // Stage 3 - save PeakList objects
      currentStage++;
      savePeakLists(zipStream);
      if (isCanceled()) {
        zipStream.close();
        tempFile.delete();
        return;
      }

      // Stage 4 - save user parameters
      currentStage++;
      saveUserParameters(zipStream);
      if (isCanceled()) {
        zipStream.close();
        tempFile.delete();
        return;
      }

      // Stage 5 - finish and close the temporary ZIP file
      currentStage++;
      currentSavedObjectName = null;
      zipStream.close();

      // Final check for cancel
      if (isCanceled()) {
        tempFile.delete();
        return;
      }

      // Move the temporary ZIP file to the final location
      if (saveFile.exists() && !saveFile.delete()) {
        throw new IOException("Could not delete old file " + saveFile);
      }

      boolean renameOK = tempFile.renameTo(saveFile);
      if (!renameOK) {
        throw new IOException(
            "Could not move the temporary file " + tempFile + " to the final location " + saveFile);
      }

      // Update the location of the project
      savedProject.setProjectFile(saveFile);

      // Update the window title to reflect the new name of the project
      // if (MZmineCore.getDesktop() instanceof MainWindow) {
      // MainWindow mainWindow = (MainWindow) MZmineCore.getDesktop();
      // mainWindow.updateTitle();
      // }

      logger.info("Finished saving the project to " + saveFile);
      setStatus(TaskStatus.FINISHED);

      // add to last loaded projects
      MZmineCore.getConfiguration().getLastProjectsParameter().addFile(saveFile);

    } catch (Throwable e) {

      logger.log(Level.SEVERE, e.getMessage(), e);
      setStatus(TaskStatus.ERROR);

      if (currentSavedObjectName == null) {
        setErrorMessage("Failed saving the project: " + ExceptionUtils.exceptionToString(e));
      } else {
        setErrorMessage(
            "Failed saving the project. Error while saving " + currentSavedObjectName + ": "
                + ExceptionUtils.exceptionToString(e));
      }

    }
  }

  /**
   * Save the version info
   *
   * @throws java.io.IOException
   */
  private void saveVersion(ZipOutputStream zipStream) throws IOException {

    zipStream.putNextEntry(new ZipEntry(VERSION_FILENAME));

    String MZmineVersion = String.valueOf(MZmineCore.getMZmineVersion());

    zipStream.write(MZmineVersion.getBytes());

  }

  private void saveStandalone(ZipOutputStream zipStream) throws IOException {
    if (savedProject.isStandalone()) {
      zipStream.putNextEntry(new ZipEntry(STANDALONE_FILENAME));
      String MZmineVersion = STANDALONE_FILENAME;
      zipStream.write(MZmineVersion.getBytes());
    }
  }

  /**
   * Save the configuration file.
   *
   * @throws java.io.IOException
   */
  private void saveConfiguration(ZipOutputStream zipStream) throws IOException {

    logger.info("Saving configuration file");

    currentSavedObjectName = "configuration";

    zipStream.putNextEntry(new ZipEntry(CONFIG_FILENAME));

    try {
      File tempConfigFile = FileAndPathUtil.createTempFile("mzmineconfig", ".tmp");
      MZmineCore.getConfiguration().saveConfiguration(tempConfigFile);
      FileInputStream fileStream = new FileInputStream(tempConfigFile);

      StreamCopy copyMachine = new StreamCopy();
      copyMachine.copy(fileStream, zipStream);

      fileStream.close();
      tempConfigFile.delete();
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      logger.warning("Could not save configuration" + ExceptionUtils.exceptionToString(e));
    }
  }

  /**
   * Save the raw data files
   */
  private void saveRawDataFiles(ZipOutputStream zipStream)
      throws IOException, ParserConfigurationException {

    AtomicBoolean finished = new AtomicBoolean(false);
    rawDataFileSaveHandler = new RawDataFileSaveHandler(savedProject, zipStream,
        Objects.requireNonNullElse(savedProject.isStandalone(), true), getModuleCallDate());
    rawDataFileSaveHandler.addTaskStatusListener((task, newStatus, oldStatus) -> {
      switch (newStatus) {
        case WAITING, PROCESSING -> {
        }
        case FINISHED -> {
          finished.set(true);
        }
        case CANCELED -> {
          finished.set(true);
          setStatus(TaskStatus.CANCELED);
        }
        case ERROR -> {
          finished.set(true);
          setErrorMessage("Error while saving raw data files.");
          setStatus(TaskStatus.ERROR);
        }
      }
    });
    MZmineCore.getTaskController().addTask(rawDataFileSaveHandler);

    while (!finished.get() && !isCanceled()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        logger.log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }

  /**
   * Save the feature lists
   *
   * @throws SAXException
   * @throws TransformerConfigurationException
   */
  private void savePeakLists(ZipOutputStream zipStream)
      throws IOException, TransformerConfigurationException, SAXException {

    final List<FeatureList> currentFeatureLists = savedProject.getCurrentFeatureLists();
    for (FeatureList featureList : currentFeatureLists) {
      FeatureListSaveTask saveTask = new FeatureListSaveTask((ModularFeatureList) featureList,
          zipStream);

      AtomicBoolean finished = new AtomicBoolean(false);
      saveTask.addTaskStatusListener((task, newStatus, oldStatus) -> {
        switch (newStatus) {
          case WAITING, PROCESSING -> {
          }
          case FINISHED, ERROR, CANCELED -> {
            finished.set(true);
          }
        }
      });
      MZmineCore.getTaskController().addTask(saveTask);

      while (!finished.get()) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          logger.log(Level.SEVERE, e.getMessage(), e);
        }
      }

      if (isCanceled()) {
        break;
      }
    }
  }

  /**
   * Save the feature lists
   *
   * @throws SAXException
   * @throws TransformerConfigurationException
   */
  private void saveUserParameters(ZipOutputStream zipStream)
      throws IOException, TransformerConfigurationException, SAXException {

    if (isCanceled()) {
      return;
    }

    logger.info("Saving user parameters");

    zipStream.putNextEntry(new ZipEntry(PARAMETERS_FILENAME));

    userParameterSaveHandler = new UserParameterSaveHandler(zipStream, savedProject,
        dataFilesIDMap);

    currentSavedObjectName = "User parameters";
    userParameterSaveHandler.saveParameters();

  }

}
