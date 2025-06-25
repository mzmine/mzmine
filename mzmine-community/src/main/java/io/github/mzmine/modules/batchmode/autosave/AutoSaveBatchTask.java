/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.batchmode.autosave;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.batchmode.BatchTask;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.taskcontrol.AbstractSimpleTask;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.XMLUtils;
import io.github.mzmine.util.files.ExtensionFilters;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import javafx.scene.control.Alert.AlertType;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AutoSaveBatchTask extends AbstractSimpleTask {

  private static final Logger logger = Logger.getLogger(AutoSaveBatchTask.class.getName());

  protected AutoSaveBatchTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      @NotNull ParameterSet parameters, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of();
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return List.of();
  }

  @Override
  protected void process() {
    if (!TaskService.getController().isTaskInstanceRunningOrQueued(BatchTask.class)) {
      // don't error since we don't want to stop processing
      DialogLoggerUtil.showDialog(AlertType.WARNING, "Cannot save batch",
          "Cannot save current batch since no batch is running. Please consider saving manually.");
      return;
    }

    final BatchTask batchTask = TaskService.getController().getReadOnlyTasksSnapshot().stream()
        .filter(task -> task.getActualTask() instanceof BatchTask)
        .map(task -> (BatchTask) task.getActualTask()).findFirst().orElse(null);
    assert batchTask != null;

    final BatchQueue queueCopy = batchTask.getQueueCopy();

    final String mzbatch = ExtensionFilters.getExtensionName(ExtensionFilters.MZ_BATCH);

    File savePath = parameters.getOptionalValue(AutoSaveBatchParameters.savePath).orElse(null);
    if (savePath == null || savePath.toString().isBlank()) {
      final File commonFromExport = ParameterUtils.extractMajorityExportPath(queueCopy);
      final File commonImport = ParameterUtils.extractMajorityRawFileImportFilePath(queueCopy);
      if (commonFromExport != null) {
        savePath = new File(commonFromExport, "autosave_batch.%s".formatted(mzbatch));
      } else if (commonImport != null) {
        savePath = new File(commonImport, "autosave_batch.%s".formatted(mzbatch));
      }
    }

    if (savePath == null || savePath.toString().isBlank()) {
      // open a dialog, but don't fail the batch
      DialogLoggerUtil.showDialog(AlertType.WARNING, "Cannot save batch",
          "The %s module cannot automatically determine a path to export the batch file to. Please save the batch manually.",
          false);
      return;
    }

    if (!savePath.exists()) {
      try {
        savePath.createNewFile();
      } catch (IOException e) {
        error("Cannot create file to save batch.", e);
        return;
      }
    }

    logger.info("Saving batch to %s".formatted(savePath.getAbsolutePath()));

    try {
      final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
          .newDocument();
      final Element element = document.createElement("batch");
      document.appendChild(element);

      // Serialize batch queue.
      queueCopy.saveToXml(element);

      XMLUtils.saveToFile(savePath, document);
    } catch (ParserConfigurationException | IOException | TransformerException e) {
      error("Error while saving batch", e);
    }
  }

  @Override
  public String getTaskDescription() {
    return "Saving batch file";
  }
}
