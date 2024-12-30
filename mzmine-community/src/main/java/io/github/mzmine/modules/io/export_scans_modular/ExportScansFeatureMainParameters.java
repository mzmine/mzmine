/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.io.export_scans_modular;

import io.github.mzmine.modules.io.export_features_sirius.SiriusExportTask;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.LibraryBatchMetadataParameters;
import io.github.mzmine.modules.io.spectraldbsubmit.batch.SpectralLibraryExportFormats;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizerComboParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import org.jetbrains.annotations.NotNull;

public class ExportScansFeatureMainParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameSuffixExportParameter file = new FileNameSuffixExportParameter(
      "Export file", """
      Local scans file. Use pattern "%s" in the file name to substitute with feature list name.
      (i.e. "prefix_%s_suffix.mgf" would become "prefix_SourceFeatureListName_suffix.mgf").
      If the file already exists, it will be overwritten.
      Filename without this pattern means all selected feature lists are exported to the same file.""".formatted(
      SiriusExportTask.MULTI_NAME_PATTERN, SiriusExportTask.MULTI_NAME_PATTERN), "scans");

  public static final ComboParameter<SpectralLibraryExportFormats> exportFormat = new ComboParameter<>(
      "Export format", "format to export", SpectralLibraryExportFormats.values(),
      SpectralLibraryExportFormats.json_mzmine);

  public static final ParameterSetParameter<LibraryBatchMetadataParameters> metadata = new ParameterSetParameter<>(
      "Metadata", "Metadata for all entries", new LibraryBatchMetadataParameters());

  public static final IntensityNormalizerComboParameter normalizer = IntensityNormalizerComboParameter.createDefaultScientific();

  public static final AdvancedParametersParameter<AdvancedExportScansFeatureParameters> advanced = new AdvancedParametersParameter<>(
      new AdvancedExportScansFeatureParameters(), true);

  public static final OptionalModuleParameter<ExportMs1ScansFeatureParameters> exportMs1 = new OptionalModuleParameter<>(
      "Export MS1", "Option to also export MS1 scans.", new ExportMs1ScansFeatureParameters());

  public static final SubModuleParameter<ExportFragmentScansFeatureParameters> exportFragmentScans = new SubModuleParameter<>(
      "Export fragment scans", "Control the selection, merging, and export of fragment scans.",
      new ExportFragmentScansFeatureParameters());


  /*
   * additional requirements:
   * Export MS1
   */
  public ExportScansFeatureMainParameters() {
    super(flists, file, exportFormat, metadata, exportMs1, exportFragmentScans, normalizer,
        advanced);
  }


  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
