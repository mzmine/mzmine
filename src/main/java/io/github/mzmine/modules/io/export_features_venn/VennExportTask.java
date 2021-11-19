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
