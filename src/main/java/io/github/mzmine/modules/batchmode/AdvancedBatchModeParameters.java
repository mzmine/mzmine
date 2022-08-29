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

package io.github.mzmine.modules.batchmode;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.DirectoryParameter;

public class AdvancedBatchModeParameters extends SimpleParameterSet {

  public static final DirectoryParameter processingParentDir = new DirectoryParameter(
      "Parent directory",
      "Select the parent directory, each folder in this directory will be considered a different dataset. All datafiles will be imported and processed.");

  public static final BooleanParameter skipOnError = new BooleanParameter("Skip on error",
      "Skip datasets (sub directories) on error. Otherwise error out and stop the batch", true);

  public static final BooleanParameter createResultsDirectory = new BooleanParameter(
      "Create results directory",
      "Push all results into a results directory with folders for datasets", true);
  public static final BooleanParameter includeSubdirectories = new BooleanParameter(
      "Search files in subdirs",
      "Search for files in sub directories. Still uses the first subdirectories as datasets each.",
      false);

  public AdvancedBatchModeParameters() {
    super(new Parameter[]{skipOnError, processingParentDir, includeSubdirectories,
        createResultsDirectory});
  }

}
