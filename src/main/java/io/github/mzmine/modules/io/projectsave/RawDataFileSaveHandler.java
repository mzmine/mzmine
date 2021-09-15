/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.projectsave;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.util.StreamCopy;
import io.github.mzmine.util.ZipUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class RawDataFileSaveHandler {

  public static final String RAW_DATA_IMPORT_BATCH_FILENAME = "raw_data_import_batch.xml";
  public static final String ROOT_ELEMENT = "root";
  public static final String BATCH_QUEUES_ROOT = "batch-queue-list";
  public static final String BATCH_QUEUE_ELEMENT = "batch-queue";
  public static final String TEMP_FILE_NAME = "mzmine_project_rawimportbatch";

  private final MZmineProject project;
  private Logger logger = Logger.getLogger(this.getClass().getName());
  private ZipOutputStream zipStream;
  private boolean canceled = false;
  private double progress = 0;
  private final List<RawDataFile> files;
  private final boolean saveFilesInProject = true;

  public RawDataFileSaveHandler(MZmineProject project, ZipOutputStream zipOutputStream) {
    this.project = project;
    this.zipStream = zipOutputStream;
    files = List.of(project.getDataFiles());
  }

  private void replaceRawFilePaths(List<BatchQueue> queues) {
    final Map<String, String> oldPathNewPath = new HashMap<>();
    for (RawDataFile file : files) {
      if (file.getAbsolutePath() == null) {
        logger.finest(() -> "File " + file.getName() + " does not have a path.");
        continue;
      }
      oldPathNewPath.put(file.getAbsolutePath(), getZipPath(file));
    }

    for (BatchQueue queue : queues) {
      for (MZmineProcessingStep<MZmineProcessingModule> step : queue) {
        for (Parameter<?> parameter : step.getParameterSet().getParameters()) {
          if (parameter instanceof FileNamesParameter fnp) {
            List<File> newValue = new ArrayList<>();
            File[] oldValue = fnp.getValue();
            for (File file : oldValue) {
              String newPath = oldPathNewPath.get(file.getAbsolutePath());
              if (newPath == null) {
                logger.warning(() -> "No new path for file " + file.getAbsolutePath());
              }
              newValue.add(new File(newPath));
            }
            fnp.setValue(newValue.toArray(File[]::new));
          }
        }
      }
    }
  }

  private void copyRawDataFilesToZip() throws IOException {

    for (final RawDataFile file : files) {
      if (file.getAbsolutePath() == null) {
        continue;
      }

      logger.finest(() -> "Copying data file " + file.getAbsolutePath() + " to project file.");

      final File f = new File(file.getAbsolutePath());
      if (f.isDirectory()) {
        ZipUtils.addDirectoryToZip(zipStream, f, getZipPath(file));
      } else {
        String zipPath = getZipPath(file);
        StreamCopy cpy = new StreamCopy();
        zipStream.putNextEntry(new ZipEntry(zipPath));

        FileInputStream inputStream = new FileInputStream(file.getAbsolutePath());
        cpy.copy(inputStream, zipStream);
        inputStream.close();
      }

      progress = (files.indexOf(file) + 1) / (double) files.size();
    }
  }

  public boolean saveRawDataFilesAsBatch() throws IOException, ParserConfigurationException {

    final Map<RawDataFile, BatchQueue> rawDataSteps = dissectRawDataMethods();
    final List<BatchQueue> mergedBatchQueues = mergeBatchQueues(rawDataSteps);

    if (saveFilesInProject) {
      replaceRawFilePaths(mergedBatchQueues);
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

      TransformerFactory transfac = TransformerFactory.newInstance();
      Transformer transformer = transfac.newTransformer();
      transformer.setOutputProperty(OutputKeys.METHOD, "xml");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

      for (final BatchQueue mergedBatchQueue : mergedBatchQueues) {
        final Element batchQueueEntry = batchQueueFile.createElement(BATCH_QUEUE_ELEMENT);
        mergedBatchQueue.saveToXml(batchQueueEntry);
        batchRoot.appendChild(batchQueueEntry);
      }

      final File tmpFile = File.createTempFile(TEMP_FILE_NAME, ".tmp");
      final StreamResult result = new StreamResult(new FileOutputStream(tmpFile));
      final DOMSource source = new DOMSource(batchQueueFile);
      transformer.transform(source, result);

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
    return true;
  }

  /**
   * @return A map of every raw data file with a batch queue of the methods applied to it.
   * Parameters are adjusted in a way that they only fit the specific raw data file.
   */
  private Map<RawDataFile, BatchQueue> dissectRawDataMethods() {

    final Map<RawDataFile, BatchQueue> rawDataSteps = new LinkedHashMap<>();

    for (final RawDataFile file : files) {
      var importStep = extractImportStep(file);
      if (importStep != null) {
        BatchQueue q = rawDataSteps.computeIfAbsent(file, f -> new BatchQueue());
        q.add(importStep);
      }

      var appliedMethods = file.getAppliedMethods();
      for (FeatureListAppliedMethod appliedMethod : appliedMethods) {
        if (importStep != null && appliedMethod.getModule() == importStep.getModule()) {
          continue;
        }
        if (!(appliedMethod.getModule() instanceof MZmineProcessingModule)) {
          logger.fine(() -> "Cannot save module " + appliedMethod.getModule().getClass().getName()
              + " is not an MZmine processing step.");
        }

        ParameterSet parameters = appliedMethod.getParameters().cloneParameterSet();
        for (Parameter<?> param : parameters.getParameters()) {
          if (param instanceof RawDataFilesParameter rfp) {
            // todo? it would be ideal to have SPECIFIC_FILES working for files that are not loaded yet
            /*((RawDataFilesParameter) param)
                .setValue(RawDataFilesSelectionType.SPECIFIC_FILES, new RawDataFile[]{file});*/
            rfp.setValue(RawDataFilesSelectionType.BATCH_LAST_FILES);
          }
        }
        BatchQueue q = rawDataSteps.computeIfAbsent(file, f -> new BatchQueue());
        q.add(new MZmineProcessingStepImpl<>((MZmineProcessingModule) appliedMethod.getModule(),
            parameters));
      }
    }

    return rawDataSteps;
  }

  /**
   * Merges batch queues consisting of the same module calls with the same parameters.
   *
   * @return The merged batch queues.
   */
  private List<BatchQueue> mergeBatchQueues(Map<RawDataFile, BatchQueue> rawDataSteps) {

    List<BatchQueue> originalQueues = rawDataSteps.values().stream().toList();
    Map<BatchQueue, List<BatchQueue>> mergableQueues = new HashMap<>();

    // find queues that are equal (same module calls and same parameters)
    for (BatchQueue originalQueue : originalQueues) {
      List<BatchQueue> equalQueueEntries = mergableQueues.keySet().stream()
          .filter(key -> SavingUtils.queuesEqual(key, originalQueue)).toList();
      if (equalQueueEntries.size() > 1) {
        logger.warning(() -> "More than one queue is equal to the current queue.");
      }

      final List<BatchQueue> mapping;
      if (equalQueueEntries.isEmpty()) {
        mergableQueues.put(originalQueue, new ArrayList<>());
        mapping = mergableQueues.get(originalQueue);
      } else {
        mapping = mergableQueues.get(equalQueueEntries.get(0));
      }
      mapping.add(originalQueue);
    }

    // merge equal module calls
    List<BatchQueue> mergedBatchQueues = new ArrayList<>();
    for (List<BatchQueue> value : mergableQueues.values()) {
      // if we just have one queue, add id directly.
      if (value.size() == 1) {
        mergedBatchQueues.add(value.get(0));
        continue;
      }

      Iterator<BatchQueue> iterator = value.iterator();
      BatchQueue merged = iterator.next();
      while (iterator.hasNext()) {
        merged = SavingUtils.mergeQueues(merged, iterator.next());
      }
      mergedBatchQueues.add(merged);
    }

    return mergedBatchQueues;
  }

  /**
   * @param file The raw data file.
   * @return An {@link MZmineProcessingStep} with the given {@link RawDataFile} as the only file in
   * the {@link FileNamesParameter}.
   */
  private MZmineProcessingStep<MZmineProcessingModule> extractImportStep(
      @NotNull final RawDataFile file) {
    if (file.getAppliedMethods().isEmpty()) {
      return null;
    }

    var appliedMethods = file.getAppliedMethods();
    var step = appliedMethods.get(0);

    if (!(step.getModule() instanceof MZmineProcessingModule importModule)) {
      logger.info(
          () -> "First module (" + step.getModule().getName() + ") applied to raw data file " + file
              .getName() + " is not am import module.");
      return null;
    } else if (file.getAbsolutePath() == null) {
      logger.fine(() -> "Cannot save raw data file " + file.getName()
          + " since there is no path associated.");
      return null;
    } else {
      final ParameterSet clone = step.getParameters().cloneParameterSet();
      for (Parameter<?> parameter : clone.getParameters()) {
        if (!(parameter instanceof FileNamesParameter fnp)) {
          continue;
        }
        fnp.setValue(new File[]{new File(file.getAbsolutePath())});
        return new MZmineProcessingStepImpl<>(importModule, clone);
      }
    }
    return null;
  }

  /**
   * @return the progress of these functions saving the raw data information to the zip file.
   */
  double getProgress() {
    return progress;
  }

  void cancel() {
    canceled = true;
  }

  public static String getZipPath(RawDataFile file) {
    return "msdatafiles/" + file.getAbsolutePath()
        .substring(file.getAbsolutePath().lastIndexOf("\\") + 1);
  }
}
