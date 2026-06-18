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

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.impl.SingleTaskRawDataFilesModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extracts sample metadata columns from the file names or paths of selected raw data files using
 * user-defined regular expressions and value mappings.
 */
public class SampleMetadataExtractionModule extends SingleTaskRawDataFilesModule {

  public static final String MODULE_NAME = "Extract metadata from file names";

  public SampleMetadataExtractionModule() {
    super(MODULE_NAME, SampleMetadataExtractionParameters.class,
        MZmineModuleCategory.PROJECTMETADATA, """
            Extract sample metadata columns from the file name or path of selected raw data files. \
            Define regular expressions to capture values, choose the target column name and type, and \
            optionally map extracted values to other values (e.g. media → blank). Results are written \
            to the project sample metadata.""");
  }

  @Override
  public @NotNull Task createTask(final @NotNull MZmineProject project,
      final @NotNull ParameterSet parameters, final @NotNull Instant moduleCallDate,
      final @Nullable MemoryMapStorage storage, final RawDataFile[] raws) {
    return new SampleMetadataExtractionTask(moduleCallDate, parameters,
        SampleMetadataExtractionModule.class, raws);
  }
}
