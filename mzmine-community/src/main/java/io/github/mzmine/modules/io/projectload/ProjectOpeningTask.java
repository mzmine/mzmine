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

package io.github.mzmine.modules.io.projectload;

import com.google.common.io.CountingInputStream;
import com.vdurmont.semver4j.Semver;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.io.projectload.version_3_0.FeatureListLoadTask;
import io.github.mzmine.modules.io.projectsave.ProjectSavingTask;
import io.github.mzmine.modules.io.projectsave.RawDataFileSaveHandler;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.project.ProjectManager;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.project.impl.MZmineProjectImpl;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.GUIUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.StreamCopy;
import io.github.mzmine.util.exceptions.ExceptionUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.io.SemverVersionReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.jetbrains.annotations.NotNull;

public class ProjectOpeningTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ProjectOpeningTask.class.getName());

  private File openFile;
  private MZmineProjectImpl newProject;

  private RawDataFileOpenHandler rawDataFileOpenHandler;
  private PeakListOpenHandler peakListOpenHandler;
  private UserParameterOpenHandler userParameterOpenHandler;
  private StreamCopy copyMachine;

  private CountingInputStream cis;
  private long totalBytes, finishedBytes;
  private String currentLoadedObjectName;

  // This hashtable maps stored IDs to raw data file objects
//  private final Hashtable<String, RawDataFile> dataFilesIDMap = new Hashtable<>();
//  private final Hashtable<String, File> scanFilesIDMap = new Hashtable<>();

  public ProjectOpeningTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.openFile = parameters.getParameter(ProjectLoaderParameters.projectFile).getValue();
  }

  public ProjectOpeningTask(File openFile, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.openFile = openFile;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    if (currentLoadedObjectName == null) {
      return "Opening project " + openFile;
    }
    return "Opening project " + openFile + " (" + currentLoadedObjectName + ")";
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {

    if (totalBytes == 0) {
      return 0;
    }

    long totalReadBytes = this.finishedBytes;

    // Add the current ZIP entry progress to totalReadBytes
    synchronized (this) {
      if (cis != null) {
        totalReadBytes += cis.getCount();
      }
    }

    return (double) totalReadBytes / totalBytes;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    try {
      // Check if existing raw data files are present
      ProjectManager projectManager = ProjectService.getProjectManager();
      if (projectManager.getCurrentProject().getDataFiles().length > 0) {
        boolean confirm = DialogLoggerUtil.showDialogYesNo("Replace existing project?",
            "Loading the project will replace the existing raw data files and feature lists. Do you want to proceed?");

        if (confirm) {
          cancel();
          return;
        }
      }

      logger.info("Started opening project " + openFile);
      setStatus(TaskStatus.PROCESSING);

      newProject = new MZmineProjectImpl();
      newProject.setProjectFile(openFile);
      newProject.setStandalone(false); // set to false by default, we check for existing files later
      GUIUtils.closeAllWindows();
      projectManager.setCurrentProject(newProject);

      ZipFile zipFile = new ZipFile(openFile);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        totalBytes += entry.getSize();
      }

      final Pattern peakListPattern = Pattern.compile("Peak list #([\\d]+) (.*)\\.xml$");
      boolean versionInformationLoaded = false;

      // Iterate over the entries and read them
      entries = zipFile.entries();

      while (entries.hasMoreElements()) {

        if (isCanceled()) {
          zipFile.close();
          return;
        }

        ZipEntry entry = entries.nextElement();
        String entryName = entry.getName();
        cis = new CountingInputStream(zipFile.getInputStream(entry));

        if (entryName.equals(ProjectSavingTask.VERSION_FILENAME)) {
          loadVersion(cis);
          versionInformationLoaded = true;
        } else if (entryName.equals(ProjectSavingTask.CONFIG_FILENAME)) {
          loadConfiguration(cis);
        } else if (entryName.equals(RawDataFileSaveHandler.RAW_DATA_IMPORT_BATCH_FILENAME)) {
          loadRawDataFiles(cis, zipFile);
        } else if (entryName.equals(ProjectSavingTask.STANDALONE_FILENAME)) {
          newProject.setStandalone(true);
        }

        // Close the ZIP entry
        cis.close();

        // Add the uncompressed entry size finishedBytes
        synchronized (this) {
          finishedBytes += entry.getSize();
          cis = null;
        }

      }

      loadFeatureList(zipFile);

      // Finish and close the project ZIP file
      zipFile.close();

      if (!versionInformationLoaded) {
        throw new IOException(
            "This file is not valid MZmine project. It does not contain version information.");
      }

      // Final check for cancel
      if (isCanceled()) {
        return;
      }

      logger.info("Finished opening project " + openFile);
      setStatus(TaskStatus.FINISHED);

      // add to last loaded projects
      MZmineCore.getConfiguration().getLastProjectsParameter().addFile(openFile);

    } catch (Throwable e) {

      // If project opening was canceled, parser was stopped by a
      // SAXException which can be safely ignored
      if (isCanceled()) {
        setStatus(TaskStatus.FINISHED);
        return;
      }

      setStatus(TaskStatus.ERROR);
      e.printStackTrace();
      setErrorMessage("Failed opening project: " + ExceptionUtils.exceptionToString(e));
    }

  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#cancel()
   */
  @Override
  public void cancel() {

    logger.info("Canceling opening of project " + openFile);

    setStatus(TaskStatus.CANCELED);

    if (rawDataFileOpenHandler != null) {
      rawDataFileOpenHandler.cancel();
    }

    if (peakListOpenHandler != null) {
      peakListOpenHandler.cancel();
    }

    if (userParameterOpenHandler != null) {
      userParameterOpenHandler.cancel();
    }

    if (copyMachine != null) {
      copyMachine.cancel();
    }

  }

  /**
   * Load the version info from the ZIP file and checks whether such version can be opened with this
   * MZmine
   */
  private void loadVersion(InputStream is) throws IOException {

    logger.info("Checking project version");

    currentLoadedObjectName = "Version";

    String projectVersionString = null;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
      projectVersionString = reader.readLine();
    }
    if(projectVersionString == null) {
      throw new IOException("Cannot open mzmine project due to a missing version specification.");
    }

    final Semver mzmineVersion = SemverVersionReader.getMZmineVersion();
    final Semver projectVersion = new Semver(projectVersionString);

    final int mzmineMajorVersion = mzmineVersion.getMajor();
    final int mzmineMinorVersion = mzmineVersion.getMinor();

    final int projectMajorVersion = projectVersion.getMajor();
    final int projectMinorVersion = projectVersion.getMinor();

    // Check if project was saved with an old version
    if (projectMajorVersion < 3) {
      final String message = "The requested project was processed with an old version of mzmine "
          + projectVersionString + ".\nIt cannot be opened with mzmine " + mzmineVersion;
      MZmineCore.getDesktop().displayErrorMessage(message);
      setStatus(TaskStatus.FINISHED);
      return;
    }

    // Check if project was saved with a newer version
    if (mzmineMajorVersion > 0) {
      if ((projectMajorVersion > mzmineMajorVersion) || ((projectMajorVersion == mzmineMajorVersion)
          && (projectMinorVersion > mzmineMinorVersion))) {
        String warning = "Warning: this project was saved with a newer version of MZmine ("
            + projectVersionString + "). Opening this project in mzmine " + mzmineVersion
            + " may result in errors or loss of information.";
        MZmineCore.getDesktop().displayMessage(warning);
      }
    }

    // Default opening handler for MZmine 3 and higher
//    peakListOpenHandler = new PeakListOpenHandler_3_0_old(dataFilesIDMap);
//    userParameterOpenHandler = new UserParameterOpenHandler_3_0(newProject, dataFilesIDMap);

    rawDataFileOpenHandler = RawDataFileOpenHandler.forVersion(projectVersion,
        getModuleCallDate());
  }

  /**
   * Load the configuration file from the project zip file
   */
  private void loadConfiguration(InputStream is) throws IOException {

    logger.info("Loading configuration file");

    currentLoadedObjectName = "Configuration";

    File tempConfigFile = FileAndPathUtil.createTempFile("mzmineconfig", ".tmp");
    FileOutputStream fileStream = new FileOutputStream(tempConfigFile);
    copyMachine = new StreamCopy();
    copyMachine.copy(is, fileStream);
    fileStream.close();

    try {
      MZmineCore.getConfiguration().loadConfiguration(tempConfigFile, false);
    } catch (Exception e) {
      logger.warning(
          "Could not load configuration from the project: " + ExceptionUtils.exceptionToString(e));
    }

    tempConfigFile.delete();
  }

  private void loadFeatureList(ZipFile zipFile) {

    FeatureListLoadTask task = new FeatureListLoadTask(MemoryMapStorage.forFeatureList(),
        newProject, zipFile);
    MZmineCore.getTaskController().addTask(task);
    currentLoadedObjectName = "Feature lists";
    while (task.getStatus() != TaskStatus.FINISHED && !task.isCanceled() && !isCanceled()) {
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private boolean loadRawDataFiles(InputStream is, ZipFile zipFile) {
    currentLoadedObjectName = ("MS data files");
    rawDataFileOpenHandler.setBatchFileStream(is);
    rawDataFileOpenHandler.setProject(newProject);
    rawDataFileOpenHandler.setZipFile(zipFile);

    AtomicBoolean finished = new AtomicBoolean(false);
    ((AbstractTask) rawDataFileOpenHandler).addTaskStatusListener((task, newStatus, oldStatus) -> {
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
          setErrorMessage("Error while opening raw data files.");
          setStatus(TaskStatus.ERROR);
        }
      }
    });
    MZmineCore.getTaskController().addTask(rawDataFileOpenHandler);

    while (!finished.get() && !isCanceled()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    return rawDataFileOpenHandler.getStatus() == TaskStatus.FINISHED;
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
  }
}
