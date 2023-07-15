/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_repo_rt;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class RepoRtAnnotationsTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(RepoRtAnnotationsTask.class.getName());
  private final FeatureList[] featureLists;
  private final File fileName;
  private final String separator = "\t";
  private final RepoRtColumns reportColumns;


  private int processedRows = 0, totalRows = 0;

  public RepoRtAnnotationsTask(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureLists = parameters.getValue(RepoRtAnnotationsParameters.featureLists)
        .getMatchingFeatureLists();
    fileName = FileAndPathUtil.getRealFilePath(
        parameters.getValue(RepoRtAnnotationsParameters.filename), "tsv");
    String datasetId = parameters.getValue(RepoRtAnnotationsParameters.datasetId);

    reportColumns = new RepoRtColumns(datasetId, 0);
  }

  @Override
  public String getTaskDescription() {
    return "Exporting compound annotations to RepoRT of feature list(s) " + Arrays.toString(
        featureLists) + " to tsv file(s) ";
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0 : processedRows / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Total number of rows
    for (FeatureList featureList : featureLists) {
      totalRows += featureList.getNumberOfRows();
    }

    // Open file
    try (BufferedWriter writer = Files.newBufferedWriter(fileName.toPath(),
        StandardCharsets.UTF_8)) {
      // Process feature lists
      for (FeatureList featureList : featureLists) {
        // Cancel?
        if (isCanceled()) {
          return;
        }

        exportFeatureList(featureList, writer);
      }
    } catch (IOException e) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Error during compound annotations csv export to " + fileName);
      logger.log(Level.WARNING,
          "Error during compound annotations csv export of feature list: " + e.getMessage(), e);
      return;
    }

    if (getStatus() == TaskStatus.PROCESSING) {
      setStatus(TaskStatus.FINISHED);
    }
  }

  private void exportFeatureList(FeatureList featureList, BufferedWriter writer)
      throws IOException {
    try {
      // Create a list of columns
      List<DBEntryField> columns = RepoRtColumns.createColumns();
      String header = String.join(separator, RepoRtColumns.getHeaders());
      // write header to file
      writer.append(header).append("\n");

      var methodCounter = new Object2IntOpenHashMap<String>(4);
      // loop through all rows in the feature list
      for (FeatureListRow row : featureList.getRows()) {
        methodCounter.clear();
        List<Object> featureAnnotations = row.getAllFeatureAnnotations();
        for (Object object : featureAnnotations) {
          if (object instanceof FeatureAnnotation annotation) {
            String method = annotation.getAnnotationMethodUniqueId();
            // count exported for method
            int alreadyExported = methodCounter.computeIfAbsent(method, m -> 0);
            // Export fields from the FeatureAnnotation object
            String dataLine = columns.stream()
                .map(field -> reportColumns.getValueString(row, annotation, field))
                .map(o -> requireNonNullElse(o, "")).collect(Collectors.joining(separator));

            writer.append(dataLine).append("\n");

            processedRows++;
            methodCounter.put(method, alreadyExported + 1);
          }
        }
      }
      System.out.println("Export successful!");
    } catch (IOException e) {
      System.out.println("Error exporting feature annotations: " + e.getMessage());
    }
  }
}
