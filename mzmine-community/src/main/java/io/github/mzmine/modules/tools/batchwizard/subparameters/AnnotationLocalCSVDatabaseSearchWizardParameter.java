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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.javafx.components.factories.FxButtons;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchParameters;
import io.github.mzmine.parameters.parametertypes.ImportType;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleComponent;
import io.github.mzmine.parameters.parametertypes.submodules.OptionalModuleParameter;
import java.io.File;
import java.util.List;
import javafx.scene.control.Button;

/**
 * Adds button to export example csv based on multiple parameters
 */
public class AnnotationLocalCSVDatabaseSearchWizardParameter extends
    OptionalModuleParameter<AnnotationLocalCSVDatabaseSearchParameters> {

  public AnnotationLocalCSVDatabaseSearchWizardParameter(String name, String description) {
    this(name, description, new AnnotationLocalCSVDatabaseSearchParameters(), false);
  }

  public AnnotationLocalCSVDatabaseSearchWizardParameter(String name, String description,
      AnnotationLocalCSVDatabaseSearchParameters embeddedParameters, boolean defaultVal) {
    super(name, description, embeddedParameters, defaultVal);
  }

  @Override
  public OptionalModuleComponent createEditingComponent() {
    final OptionalModuleComponent moduleComp = super.createEditingComponent();

    final Button exampleButton = FxButtons.createButton("Example", FxIcons.SAVE, """
        Export an example table with all selected column headers.
        Also considers the filename column if the filter is activated.
        Format depends on extension .csv (comma-separated) or .tsv (tab-separated).""", () -> {
      final AnnotationLocalCSVDatabaseSearchParameters params = getEmbeddedParameters();

      final List<ImportType<?>> allTypes = params.getValue(
          AnnotationLocalCSVDatabaseSearchParameters.columns);
      final String fileNameColumn = params.getEmbeddedParameterValueIfSelectedOrElse(
          AnnotationLocalCSVDatabaseSearchParameters.filterSamplesColumn, null);
      final File dataBaseFile = params.getValue(
          AnnotationLocalCSVDatabaseSearchParameters.dataBaseFile);
      LocalCSVDatabaseSearchParameters.exportExampleFile(allTypes, fileNameColumn, dataBaseFile);
    });

    moduleComp.getTopPane().getChildren().add(exampleButton);
    return moduleComp;
  }

  @Override
  public OptionalModuleParameter<AnnotationLocalCSVDatabaseSearchParameters> cloneParameter() {
    final AnnotationLocalCSVDatabaseSearchParameters embeddedParametersClone = (AnnotationLocalCSVDatabaseSearchParameters) getEmbeddedParameters().cloneParameterSet();
    return new AnnotationLocalCSVDatabaseSearchWizardParameter(getName(), getDescription(),
        embeddedParametersClone, getValue());
  }
}
