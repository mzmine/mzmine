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
import io.github.mzmine.modules.presets.ModulePreset;
import io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// use in its own module to extract metadata, or as a sub-module
public class SampleMetadataExtractionParameters extends SimpleParameterSet {

  /// dataFiles is optional and only present in main module parameters not if this parameters class
  /// is used as sub parameters
  public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter(
      new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES));

  public static final MetadataRegexExtractionParameter mappings = new MetadataRegexExtractionParameter(
      "Regex column mappings", """
      Define one or multiple mappings that extract a metadata column from the file name or path of \
      each raw data file. Each mapping defines the input source (file name or path), the \
      target column name and type, a regular expression with a capture group, and optional \
      case-insensitive value mappings (e.g. media → blank).""");

  public static final BooleanParameter overwrite = new BooleanParameter("Overwrite existing values",
      "If checked, existing metadata values are overwritten. If unchecked, only empty cells are "
          + "filled and existing values are kept.", true);

  /// Default parameterset with all parameters including {@link #dataFiles}
  public SampleMetadataExtractionParameters() {
    this(false);
  }

  public SampleMetadataExtractionParameters(boolean isSubModule) {
    Parameter<?>[] parameters = isSubModule ? new Parameter<?>[]{overwrite, mappings}
        : new Parameter<?>[]{dataFiles, overwrite, mappings};
    super(parameters);
  }

  @Override
  public @NotNull List<ModulePreset> createDefaultPresets() {
    // Preset for extracting media/blank samples
    final String presetGroup = new SampleMetadataExtractionModule().getUniqueID();
    var mediaPreset = new ModulePreset("Extract media (blank)", presetGroup, create(List.of(
        MetadataRegexMapping.createUnmappedDefault(RegexInputSource.FILE_NAME,
            MetadataColumn.SAMPLE_TYPE_HEADER, ExtractColumnType.TEXT,
            ".*(?<![a-zA-Z])(media(?:[_-]?blank)?)(?![a-zA-Z]).*", "media"))));

    var pooledqcPreset = new ModulePreset("Extract pooled QC", presetGroup, create(List.of(
        MetadataRegexMapping.createUnmappedDefault(RegexInputSource.FILE_NAME,
            MetadataColumn.SAMPLE_TYPE_HEADER, ExtractColumnType.TEXT,
            ".*(?<![a-zA-Z])(pool(?:ed)?(?:[_-]?qc)?)(?![a-zA-Z]).*", "pooled_qc"))));

    return List.of(pooledqcPreset, mediaPreset);
  }

  public static SampleMetadataExtractionParameters create(List<MetadataRegexMapping> mappings) {
    final SampleMetadataExtractionParameters params = new SampleMetadataExtractionParameters().cloneParameterSet();
    params.setParameter(dataFiles, new RawDataFilesSelection(RawDataFilesSelectionType.ALL_FILES));
    params.setParameter(overwrite, true);
    params.setParameter(SampleMetadataExtractionParameters.mappings, mappings);
    return params;
  }

  @Override
  public SampleMetadataExtractionParameters cloneParameterSet() {
    return (SampleMetadataExtractionParameters) super.cloneParameterSet();
  }


  /// files selected by drag and drop or in the data import or wizard
  public void setSelectedFiles(@Nullable final File[] files) {
    getParameter(mappings).setSelectedFiles(files != null ? Arrays.asList(files) : List.of());
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
  public static SampleMetadataExtractionTask createTaskWithDataFiles(
      @NotNull final ParameterSet parameters, @NotNull final Instant moduleCallDate,
      @NotNull final Class<? extends MZmineModule> moduleClass, @NotNull final RawDataFile[] raws) {
    return new SampleMetadataExtractionTask(moduleCallDate, parameters, moduleClass, raws,
        parameters.getValue(mappings), parameters.getValue(overwrite));
  }

  public void resetDefaults() {
    setParameter(overwrite, true);
    setParameter(mappings, List.of());
    tryGetParameter(dataFiles).ifPresent(p -> p.setValue(RawDataFilesSelectionType.ALL_FILES));
  }
}
