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

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;

/**
 * Clone of {@link SampleMetadataExtractionParameters} without the raw data file selection. Used as
 * an embedded parameter set in other modules (e.g. raw data import) that provide the files
 * themselves rather than via a
 * {@link io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter}.
 */
public class SampleMetadataExtractionEmbeddedParameters extends SimpleParameterSet {

  public static final MetadataRegexExtractionParameter mappings = new MetadataRegexExtractionParameter(
      SampleMetadataExtractionParameters.mappings.getName(),
      SampleMetadataExtractionParameters.mappings.getDescription()
          .replace("each selected", "each imported"));

  public static final BooleanParameter overwrite = new BooleanParameter("Overwrite existing values",
      "If checked, existing metadata values are overwritten. If unchecked, only empty cells are "
          + "filled and existing values are kept.", true);

  public SampleMetadataExtractionEmbeddedParameters() {
    super(overwrite, mappings);
  }

  /**
   * Creates an extraction task from these parameters for the externally provided files.
   *
   * @param parameters     the (selected) embedded parameters of this class
   * @param moduleCallDate the module call date
   * @param moduleClass    the calling module (recorded as applied method)
   * @param raws           the files provided by the parent task
   * @return a runnable extraction task
   */
  public static SampleMetadataExtractionTask createTask(@NotNull final ParameterSet parameters,
      @NotNull final Instant moduleCallDate,
      @NotNull final Class<? extends MZmineModule> moduleClass, @NotNull final RawDataFile[] raws) {
    return new SampleMetadataExtractionTask(moduleCallDate, parameters, moduleClass, raws,
        parameters.getValue(mappings), parameters.getValue(overwrite));
  }
}
