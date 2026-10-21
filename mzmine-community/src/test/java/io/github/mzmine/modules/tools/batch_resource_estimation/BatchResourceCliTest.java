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

package io.github.mzmine.modules.tools.batch_resource_estimation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.batchmode.BatchQueue;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.XMLUtils;
import io.mzio.mzmine.startup.EstimateResourcesSettings;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BatchResourceCliTest {

  @TempDir
  Path directory;

  @BeforeAll
  static void initializePreferences() {
    ConfigService.getPreferences();
  }

  @Test
  void unreadableBatchReturnsAnErrorTask() {
    final File batch = directory.resolve("missing.mzbatch").toFile();
    final File statistics = directory.resolve("statistics.tsv").toFile();
    final Task task = BatchResourceEstimationModule.runBatchFile(batch, null, null, null, null,
        null, new EstimateResourcesSettings(6, 2, statistics), Instant.now());
    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertTrue(task.getErrorMessage().startsWith("Cannot load batch file"));
    final var parameters = ((AbstractSimpleToolTask) task).getParameters();
    assertEquals(batch, parameters.getValue(BatchResourceEstimationParameters.batchFile));
    assertEquals(6, parameters.getValue(BatchResourceEstimationParameters.smallestSubset));
    assertEquals(2, parameters.getValue(BatchResourceEstimationParameters.subsetSizes));
    assertEquals(statistics, parameters.getEmbeddedParameterValueIfSelectedOrElse(
        BatchResourceEstimationParameters.statisticsFile, null));
  }

  @Test
  void cliInputOverridesAreAppliedBeforeCheckingTheRequiredSubsetSize() throws Exception {
    final File batch = saveImportBatch();
    final File[] files = {directory.resolve("first.mzML").toFile(),
        directory.resolve("second.mzML").toFile()};
    final Task task = BatchResourceEstimationModule.runBatchFile(batch, files, null, null, null,
        null, new EstimateResourcesSettings(4, 3, null), Instant.now());
    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertTrue(task.getErrorMessage().startsWith("The batch has 2 files"), task.getErrorMessage());
  }

  @Test
  void aMissingDatabaseStepDoesNotSilentlyIgnoreTheCliOverride() throws Exception {
    final Task task = BatchResourceEstimationModule.runBatchFile(saveImportBatch(), null, null,
        null, null, directory.resolve("database.csv").toFile(),
        new EstimateResourcesSettings(4, 3, null), Instant.now());
    assertEquals(TaskStatus.ERROR, task.getStatus());
    assertEquals("Cannot apply the CLI compound database override", task.getErrorMessage());
  }

  private @NotNull File saveImportBatch() throws Exception {
    final File input = Files.createFile(directory.resolve("original.mzML")).toFile();
    final AllSpectralDataImportParameters parameters = new AllSpectralDataImportParameters();
    parameters.setParameter(AllSpectralDataImportParameters.fileNames, new File[]{input});
    final BatchQueue queue = new BatchQueue();
    queue.add(new MZmineProcessingStepImpl<>(new AllSpectralDataImportModule(), parameters));
    final var document = XMLUtils.newDocument();
    final var root = document.createElement("batch");
    document.appendChild(root);
    queue.saveToXml(root);
    final File batch = directory.resolve("workflow.mzbatch").toFile();
    XMLUtils.saveToFile(batch, document);
    return batch;
  }
}
