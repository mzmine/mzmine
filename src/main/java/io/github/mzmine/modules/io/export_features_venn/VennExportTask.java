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

package io.github.mzmine.modules.io.export_features_venn;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exports a feature list to a csv that can be plotted as a venn diagram by other software such as
 * VennDis: https://analyticalsciencejournals.onlinelibrary.wiley.com/doi/10.1002/pmic.201400320 or
 * VENNY https://bioinfogp.cnb.csic.es/tools/venny/index.html
 *
 * @author https://github.com/SteffenHeu
 */
public class VennExportTask extends AbstractTask {

  private final ModularFeatureList flist;
  private final ParameterSet parameterSet;
  private final File path;
  private final boolean manualAsDetected = true;

  private final int maxRows;
  private int processedRows;


  protected VennExportTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      ModularFeatureList flist, ParameterSet parameterSet) {
    super(storage, moduleCallDate);
    this.flist = flist;
    this.parameterSet = parameterSet;
    maxRows = flist.getNumberOfRows();
    path = parameterSet.getParameter(VennExportParameters.directory).getValue();
  }

  @Override
  public String getTaskDescription() {
    return "Exporting " + flist.getName() + " as venn diagramm.";
  }

  @Override
  public double getFinishedPercentage() {
    return processedRows / (double) maxRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (!path.isDirectory()) {
      setErrorMessage("Given path is not a directory.");
      setStatus(TaskStatus.ERROR);
      return;
    }
    // Cleanup from illegal filename characters
    String cleanFlName = flist.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
    // Substitute
    final File curFile = new File(path.getPath(), cleanFlName + "_venn" + ".csv");
    if (!curFile.exists()) {
      try {
        curFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
        setErrorMessage("Cannot create new file");
        setStatus(TaskStatus.ERROR);
        return;
      }
    }

    try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
        StandardCharsets.UTF_8)) {

      final List<RawDataFile> files = flist.getRawDataFiles();

      for (int i = 0; i < files.size(); i++) {
        final RawDataFile rawDataFile = files.get(i);
        writer.append(rawDataFile.getName());
        if (i < files.size() - 1) {
          writer.append(",");
        }
      }
      writer.newLine();

      for (FeatureListRow row : flist.getRows()) {

        for (int i = 0; i < files.size(); i++) {
          final RawDataFile rawDataFile = files.get(i);
          final Feature feature = row.getFeature(rawDataFile);

          if (feature != null && feature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
            if (feature.getFeatureStatus() == FeatureStatus.DETECTED || (manualAsDetected
                && feature.getFeatureStatus() == FeatureStatus.MANUAL)) {
              writer.append(String.valueOf(row.getID()));
            }
          }

          if (i < files.size() - 1) {
            writer.append(",");
          }
        }

        writer.newLine();
        processedRows++;
      }

      writer.flush();
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
      setErrorMessage("Cannot create writer.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }
}
