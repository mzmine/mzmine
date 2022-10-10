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
