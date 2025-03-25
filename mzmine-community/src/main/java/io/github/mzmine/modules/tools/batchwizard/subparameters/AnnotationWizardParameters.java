/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.io.download.AssetCategory;
import io.github.mzmine.modules.io.download.DownloadAssets;
import io.github.mzmine.modules.io.import_spectral_library.SpectralLibraryImportParameters;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.AnnotationWizardParameterFactory;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNamesWithDownloadParameter;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import io.github.mzmine.util.files.ExtensionFilters;

/**
 * Reuses spectral library files parameters of {@link SpectralLibraryImportParameters}
 *
 * @author Robin Schmid <a href="https://github.com/robinschmid">https://github.com/robinschmid</a>
 */
public final class AnnotationWizardParameters extends WizardStepParameters {

  public static final OptionalModuleParameter<AnnotationLocalCSVDatabaseSearchParameters> localCsvSearch = new OptionalModuleParameter<>(
      "Local compound database search", """
      Search a local CSV or TSV database as comma- or tab-separated data, respectively.
      Matches are done based on m/z, retention time, and ion mobility if applicable and selected.
      """, new AnnotationLocalCSVDatabaseSearchParameters());

  public static final BooleanParameter lipidAnnotation = new BooleanParameter("Annotate lipids", """
      Lipid annotation can be applied on MS1 only data (including imaging),
      or in combination with MS2 information from either DDA or DIA experiments.
      """, true);

  public static final FileNamesWithDownloadParameter dataBaseFiles = new FileNamesWithDownloadParameter(
      "Spectral library files", """
      Path of spectral library files in common formats
      (GNPS json, MONA json, NIST msp, mgf, JCAMP-DX jdx)""", ExtensionFilters.ALL_LIBRARY,
      DownloadAssets.forAssetGroup(AssetCategory.SPECTRAL_LIBRARIES));

  public AnnotationWizardParameters() {
    super(WizardPart.ANNOTATION, AnnotationWizardParameterFactory.Annotation,
        // parameters
        localCsvSearch, lipidAnnotation, dataBaseFiles);
  }

}
