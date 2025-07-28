/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_merge_libraries;

import io.github.mzmine.modules.io.spectraldbsubmit.batch.SpectralLibraryExportFormats;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizerComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelection;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionParameter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionType;
import io.github.mzmine.util.StringUtils;
import java.util.List;

public class MergeLibrariesParameters extends SimpleParameterSet {

  public static final SpectralLibrarySelectionParameter speclibs = new SpectralLibrarySelectionParameter(
      new SpectralLibrarySelection(SpectralLibrarySelectionType.AS_SELECTED_IN_MAIN_WINDOW,
          List.of()));

  public static final FileNameSuffixExportParameter newLibraryFile = new FileNameSuffixExportParameter(
      "Merged library file", "Specify the file the libraries shall be merged into.",
      "merged_library");

  public static final ComboParameter<SpectralLibraryExportFormats> exportFormat = new ComboParameter<>(
      "Export format", "format to export", SpectralLibraryExportFormats.values(),
      SpectralLibraryExportFormats.json_mzmine);

  public static final BooleanParameter removeAndImport = new BooleanParameter(
      "Remove libraries, import merged library",
      "If selected, the merged library will be imported and the selected libraries will be removed from the project (library files will not be deleted).",
      false);

  public static final ComboParameter<IdHandlingOption> idHandling = new ComboParameter<>(
      "Entry IDs", """
      Specify how entry IDs of the existing entries shall be handled during export.
      %s""".formatted(
      StringUtils.join(IdHandlingOption.values(), "\n", IdHandlingOption::getDescription)),
      IdHandlingOption.values(), IdHandlingOption.NEW_ID_WITH_DATASET_ID);

  public static final IntensityNormalizerComboParameter normalizer = IntensityNormalizerComboParameter.createWithoutScientific();

  public MergeLibrariesParameters() {
    super(speclibs, newLibraryFile, exportFormat, idHandling, removeAndImport, normalizer);
  }
}
