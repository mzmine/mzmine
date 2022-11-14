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

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.parameters.parametertypes.submodules.SubModuleParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;

/**
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public class LibraryBatchGenerationParameters extends SimpleParameterSet {


  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final FileNameParameter file = new FileNameParameter("Export file",
      "Local library file", FileSelectionType.SAVE);

  public static final ComboParameter<SpectralLibraryExportFormats> exportFormat = new ComboParameter<>(
      "Export format", "format to export", SpectralLibraryExportFormats.values(),
      SpectralLibraryExportFormats.json);

  public static final SubModuleParameter<LibraryBatchMetadataParameters> metadata = new SubModuleParameter<>(
      "Metadata", "Metadata for all entries", new LibraryBatchMetadataParameters());

  public static final OptionalParameter<MZToleranceParameter> mergeMzTolerance = new OptionalParameter<>(
      new MZToleranceParameter("m/z tolerance (merging)",
          "If selected, spectra from different collision energies will be merged.\n"
              + "The tolerance used to group signals during merging of spectra", 0.008, 25));

  public static final OptionalModuleParameter<HandleChimericMsMsParameters> handleChimerics = new OptionalModuleParameter<>(
      "Handle chimeric spectra",
      "Options to identify and handle chimeric spectra with multiple MS1 signals in the precusor ion selection",
      new HandleChimericMsMsParameters(), true);

  public static final SubModuleParameter<LibraryExportQualityParameters> quality = new SubModuleParameter<>(
      "Quality parameters", "Quality parameters for MS/MS spectra to be exported to the library.",
      new LibraryExportQualityParameters());

  public LibraryBatchGenerationParameters() {
    super(new Parameter[]{flists, file, exportFormat, metadata, mergeMzTolerance, handleChimerics,
        quality});
  }

}
