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

/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 *
 * It is freely available under the GNU GPL licence of MZmine2.
 *
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 *
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import io.github.mzmine.modules.dataanalysis.spec_chimeric_precursor.HandleChimericMsMsParameters;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.SpectraMergeSelectParameter;
import io.github.mzmine.modules.dataprocessing.filter_scan_merge_select.options.SpectraMergeSelectPresets;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.AdvancedParametersParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntensityNormalizerComboParameter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter.Options;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilterParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameSuffixExportParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.ParameterSetParameter;
import org.jetbrains.annotations.NotNull;

/**
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class LibraryBatchGenerationParameters extends SimpleParameterSet {

  public static final MsLevelFilterParameter postMergingMsLevelFilter = new MsLevelFilterParameter(
      new Options[]{Options.MS2, Options.MSn}, new MsLevelFilter(Options.MSn));

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameSuffixExportParameter file = new FileNameSuffixExportParameter(
      "Export file", "Local library file", "batch_library");

  public static final ComboParameter<SpectralLibraryExportFormats> exportFormat = new ComboParameter<>(
      "Export format", "format to export", SpectralLibraryExportFormats.values(),
      SpectralLibraryExportFormats.json_mzmine);

  public static final ParameterSetParameter<LibraryBatchMetadataParameters> metadata = new ParameterSetParameter<>(
      "Metadata", "Metadata for all entries", new LibraryBatchMetadataParameters());

  public static final IntensityNormalizerComboParameter normalizer = IntensityNormalizerComboParameter.createWithoutScientific();

  // Use representative scans or MSn tree so that we export each energy and across energies for each MSn precursor
  // this is specific to library generation
  public static final SpectraMergeSelectParameter merging = new SpectraMergeSelectParameter(
      SpectraMergeSelectPresets.REPRESENTATIVE_MSn_TREE);

  public static final OptionalModuleParameter<HandleChimericMsMsParameters> handleChimerics = new OptionalModuleParameter<>(
      "Handle chimeric spectra",
      "Options to identify and handle chimeric spectra with multiple MS1 signals in the precusor ion selection",
      new HandleChimericMsMsParameters(), true);

  public static final ParameterSetParameter<LibraryExportQualityParameters> quality = new ParameterSetParameter<>(
      "Quality parameters", "Quality parameters for MS/MS spectra to be exported to the library.",
      new LibraryExportQualityParameters());

  public static final AdvancedParametersParameter<AdvancedLibraryBatchGenerationParameters> advanced = new AdvancedParametersParameter<>(
      new AdvancedLibraryBatchGenerationParameters(), false);

  public LibraryBatchGenerationParameters() {
    super(flists, file, exportFormat, postMergingMsLevelFilter, metadata, normalizer, merging,
        handleChimerics, quality, advanced);
  }


  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
