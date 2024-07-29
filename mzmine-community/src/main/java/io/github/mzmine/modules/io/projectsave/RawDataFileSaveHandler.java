/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilePlaceholder;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.StreamCopy;
import io.github.mzmine.util.XMLUtils;
import io.github.mzmine.util.ZipUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RawDataFileSaveHandler extends AbstractTask {

  public static final String RAW_DATA_IMPORT_BATCH_FILENAME = "raw_data_import_batch.xml";
  public static final String ROOT_ELEMENT = "root";
  public static final String BATCH_QUEUES_ROOT = "batch-queue-list";
  public static final String BATCH_QUEUE_ELEMENT = "batch-queue";
  public static final String TEMP_FILE_NAME = "mzmine_project_rawimportbatch";
  public static final String DATA_FILES_FOLDER = "msdatafiles/";
  public static final String DATA_FILES_PREFIX = "$$";
  public static final String DATA_FILES_SUFFIX = DATA_FILES_PREFIX;
  public static final Pattern DATA_FILE_PATTERN = Pattern.compile("(\\$\\$)([^\\n]+)(\\$\\$)");

  private final MZmineProject project;
  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private final ZipOutputStream zipStream;
  private final List<RawDataFile> files;
  private final boolean saveFilesInProject;
  private final String prefix = "Saving raw data files: ";
  private final int numSteps;
  private final double stepProgress;
  private double progress = 0;
  private String description;

  public RawDataFileSaveHandler(MZmineProject project, ZipOutputStream zipOutputStream,
      boolean saveFilesInProject, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.project = project;
    this.zipStream = zipOutputStream;
    this.saveFilesInProject = saveFilesInProject;
    files = List.of(project.getDataFiles());
    numSteps = 1 /*dissect + merge */ + (saveFilesInProject ? files.size() : 0) /*save files*/
        + 1 /*save batch file*/;
    stepProgress = 1 / (double) numSteps;
  }

  public static String getZipPath(@NotNull RawDataFile file, @Nullable String prefix,
      @Nullable String suffix) {
    return getZipPath(file.getAbsoluteFilePath(), prefix, suffix);
  }

  public static String getZipPath(@NotNull File file, @Nullable String prefix,
      @Nullable String suffix) {
    StringBuilder path = new StringBuilder();
    if (prefix != null) {
      path.append(prefix);
    }
    path.append(DATA_FILES_FOLDER);
    final String separator = System.getProperty("file.separator");
    path.append(
        file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf(separator) + 1));
    if (suffix != null) {
      path.append(suffix);
    }

    return path.toString();
  }

  public static String getZipPath(RawDataFile file) {
    return getZipPath(file, null, null);
  }

  public static String getZipPath(File file) {
    return getZipPath(file, null, null);
  }

  @Override
  public TaskPriority getTaskPriority() {
    return TaskPriority.HIGH;
  }

  public boolean saveRawDataFilesAsBatch() throws IOException, ParserConfigurationException {

    List<BatchQueue> cleanedBatchQueues = List.of(RawDataSavingUtils.makeBatchQueue(files));
    progress += stepProgress;

    if (saveFilesInProject) {
      description = prefix + "Zipping raw data files.";
      replaceRawFilePaths(cleanedBatchQueues);
      copyRawDataFilesToZip();
    }

    zipStream.putNextEntry(new ZipEntry(RAW_DATA_IMPORT_BATCH_FILENAME));

    try {
      final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      final DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();

      final Document batchQueueFile = dbBuilder.newDocument();
      final Element root = batchQueueFile.createElement(ROOT_ELEMENT);
      final Element batchRoot = batchQueueFile.createElement(BATCH_QUEUES_ROOT);

      root.appendChild(batchRoot);
      batchQueueFile.appendChild(root);

      for (final BatchQueue mergedBatchQueue : cleanedBatchQueues) {
        final Element batchQueueEntry = batchQueueFile.createElement(BATCH_QUEUE_ELEMENT);
        mergedBatchQueue.saveToXml(batchQueueEntry);
        batchRoot.appendChild(batchQueueEntry);
      }

      final File tmpFile = FileAndPathUtil.createTempFile(TEMP_FILE_NAME, ".tmp");
      XMLUtils.saveToFile(tmpFile, batchQueueFile);

      final StreamCopy copyMachine = new StreamCopy();
      final FileInputStream fileInputStream = new FileInputStream(tmpFile);
      copyMachine.copy(fileInputStream, zipStream);

      fileInputStream.close();
      tmpFile.delete();
    } catch (ParserConfigurationException | TransformerException e) {
      e.printStackTrace();
      logger.log(Level.WARNING, "Could not save batch import step.\n" + e.getMessage(), e);
      return false;
    }
    progress += stepProgress;

    return true;
  }

  /**
   * Copies the raw data files to the zip folder (MZmine project file).
   *
   * @throws IOException
   */
  private void copyRawDataFilesToZip() throws IOException {

    for (final RawDataFile file : files) {
      if (file.getAbsolutePath() == null || !Files.exists(Paths.get(file.getAbsolutePath()))) {
        progress += stepProgress;
        continue;
      }

      description = prefix + "Copying data file " + file.getAbsolutePath() + " to project file.";
      logger.finest(() -> "Copying data file " + file.getAbsolutePath() + " to project file.");

      final File f = new File(file.getAbsolutePath());
      if (f.isDirectory()) {
        ZipUtils.zipDirectory(zipStream, f, getZipPath(file));
      } else {
        try {
          String zipPath = getZipPath(file);
          copyToZip(file.getAbsoluteFilePath(), zipPath);

          for (File additional : RawDataFileType.getAdditionalRequiredFiles(file)) {
            if (!additional.exists() || !additional.canRead()) {
              throw new RuntimeException(
                  "Required file %s for raw file %s does not exist.".formatted(
                      additional.getAbsolutePath(), file.getAbsolutePath()));
            }
            copyToZip(additional, getZipPath(additional));
          }
        } catch (ZipException e) {
          // this might happen in case fo duplicate files
          logger.info(e::getMessage);
          continue;
        }
      }

      progress += stepProgress;
    }
  }

  private void copyToZip(File actualFile, String zipPath) throws IOException {
    zipStream.putNextEntry(new ZipEntry(zipPath));
    FileInputStream inputStream = new FileInputStream(actualFile);
    StreamCopy cpy = new StreamCopy();
    cpy.copy(inputStream, zipStream);
    inputStream.close();
  }

  /**
   * @return the progress of these functions saving the raw data information to the zip file.
   */
  double getProgress() {
    return progress;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return progress;
  }

  /**
   * Replaces the raw data file paths in case an independent project is saved to an MZmine project
   * file.
   *
   * @param queues The batch queues.
   */
  private void replaceRawFilePaths(List<BatchQueue> queues) {
    final Map<String, String> oldPathNewPath = new HashMap<>();
    for (RawDataFile file : files) {
      if (file.getAbsolutePath() == null) {
        logger.finest(() -> "File " + file.getName() + " does not have a path.");
        continue;
      }
      final String newPath = getZipPath(file, DATA_FILES_PREFIX, DATA_FILES_SUFFIX);
      if (!DATA_FILE_PATTERN.matcher(newPath).matches()) {
        throw new IllegalArgumentException(
            "Cannot save file. New path does not match the save pattern. Please contact the developers. absolutePath: "
                + file.getAbsolutePath() + " newPath: " + newPath);
      }
      oldPathNewPath.put(file.getAbsolutePath(), newPath);
    }

    for (final BatchQueue queue : queues) {
      for (MZmineProcessingStep<MZmineProcessingModule> step : queue) {
        for (Parameter<?> parameter : step.getParameterSet().getParameters()) {
          // adjust the file names and paths for import steps.
          if (parameter instanceof FileNamesParameter fnp) {
            List<File> newValue = new ArrayList<>();
            File[] oldValue = fnp.getValue();
            for (File file : oldValue) {
              String newPath = oldPathNewPath.get(file.getAbsolutePath());
              if (newPath == null) {
                logger.warning(() -> "No new path for file " + file.getAbsolutePath());
                continue;
              }
              newValue.add(new File(newPath));
            }
            fnp.setValue(newValue.toArray(File[]::new));
          } else if (parameter instanceof RawDataFilesParameter rfp && saveFilesInProject) {
            // if we save files in project, we have to adjust the paths and file selections
            RawDataFilesSelection selection = rfp.getValue();
            final RawDataFile[] files =
                selection.getSelectionType() == RawDataFilesSelectionType.SPECIFIC_FILES
                    ? selection.getSpecificFilesPlaceholders() : selection.getEvaluationResult();
            final RawDataFilePlaceholder[] placeholders = new RawDataFilePlaceholder[files.length];
            for (int i = 0; i < files.length; i++) {
              final RawDataFile file = files[i];
              placeholders[i] = new RawDataFilePlaceholder(file.getName(),
                  file.getAbsolutePath() != null ? getZipPath(file, DATA_FILES_PREFIX,
                      DATA_FILES_SUFFIX) : null);
            }
            selection.setSelectionType(RawDataFilesSelectionType.SPECIFIC_FILES);
            selection.setSpecificFiles(placeholders);
          }
        }
      }
    }
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    try {
      if (!saveRawDataFilesAsBatch()) {
        setStatus(TaskStatus.ERROR);
        return;
      }
    } catch (IOException | ParserConfigurationException e) {
      setStatus(TaskStatus.ERROR);
      e.printStackTrace();
    }

    setStatus(TaskStatus.FINISHED);
  }
}
