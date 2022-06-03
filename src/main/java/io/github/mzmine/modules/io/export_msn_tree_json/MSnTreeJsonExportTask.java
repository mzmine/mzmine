/*
 * Copyright 2006-2022 The MZmine Development Team
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

package io.github.mzmine.modules.io.export_msn_tree_json;

import io.github.mzmine.datamodel.PrecursorIonTree;
import io.github.mzmine.datamodel.PrecursorIonTreeNode;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MSnTreeJsonExportTask extends AbstractTask {


  private final File outFile;
  private final RawDataFile[] raws;
  private final MZTolerance mzTol;
  private int total = 0;
  private int done;
  private String description = "Exporting raw files as MSn trees to jsonlines file";

  public MSnTreeJsonExportTask(ParameterSet parameters, Instant moduleCallDate) {
    super(null, moduleCallDate);
    outFile = parameters.getValue(MSnTreeJsonExportParameters.FILENAME);
    raws = parameters.getValue(MSnTreeJsonExportParameters.RAW_FILES).getMatchingRawDataFiles();
    mzTol = parameters.getValue(MSnTreeJsonExportParameters.MZ_TOL);
    description = String.format("Exporting %d raw files as MSn trees to jsonlines file %s",
        raws.length, outFile.getAbsolutePath());
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return total == 0 ? 0 : done / (double) total;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    total = raws.length;

    for (RawDataFile raw : raws) {
      final List<PrecursorIonTree> trees = ScanUtils.getMSnFragmentTrees(raw);
      for (PrecursorIonTree tree : trees) {
        List<String> lines = treeNodeToCSV(raw, tree.getRoot(), new ArrayList<>());
      }

      done++;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private List<String> treeNodeToCSV(RawDataFile raw, PrecursorIonTreeNode treeNode,
      List<String> lines) {
    // export all spectra
    for (var spec : treeNode.getFragmentScans()) {
      spectrumToCSV(raw, spec, lines);
    }

    // add children
    for (var child : treeNode.getChildPrecursors()) {
      treeNodeToCSV(raw, child, lines);
    }
    return lines;
  }

  private void spectrumToCSV(RawDataFile raw, Scan spec, List<String> lines) {

  }
}
