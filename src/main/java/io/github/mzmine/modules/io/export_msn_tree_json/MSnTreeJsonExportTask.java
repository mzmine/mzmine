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
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.File;
import java.time.Instant;
import java.util.List;

public class MSnTreeJsonExportTask extends AbstractTask {


  private final File outFile;
  private final RawDataFile[] raws;
  private final MZTolerance mzTol;
  private final int total = 0;
  private final int done = 0;
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
    for (RawDataFile raw : raws) {
      final List<PrecursorIonTree> trees = ScanUtils.getMSnFragmentTrees(raw);
      for (PrecursorIonTree tree : trees) {
        tree.getRoot().getFragmentScans()
      }
    }
  }
}
