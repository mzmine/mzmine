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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.datamodel.identities.iontype.IonLibraries;
import io.github.mzmine.datamodel.identities.iontype.IonLibrary;
import io.github.mzmine.modules.dataprocessing.id_localcsvsearch.LocalCSVDatabaseSearchParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.ImportTypeParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.ionidentity.IonLibraryParameter;
import io.github.mzmine.util.files.ExtensionFilters;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Reduced set of parameter for {@link LocalCSVDatabaseSearchParameters} used in the wizard
 */
public class AnnotationLocalCSVDatabaseSearchParameters extends SimpleParameterSet {

  public static final FileNameParameter dataBaseFile = new FileNameParameter("Database file",
      "Name of file that contains information for peak identification",
      ExtensionFilters.CSV_TSV_IMPORT, FileSelectionType.OPEN);

  public static final OptionalParameter<StringParameter> filterSamplesColumn = new OptionalParameter<>(
      new StringParameter("Filename column (for library generation)", """
          Column header to filter matches to only occur in the given sample. Sample name needs to contain
          the value of this column. Use something unique as identifier, this could be the full data file name,
          or a sample id _A1_. Use a pre- and suffix (here _) to make this ID unique, otherwise A1 also
          matches A11, A12, [..]. Used for library generation workflows.
          """, "filename"), false);
  public static ComboParameter<MassOptions> massOptionsComboParameter = new ComboParameter<>(
      "Use precursor m/z",
      "Either use the precursor m/z from the database, or calculate various ions from the neutral mass/formula/smiles",
      MassOptions.values(), MassOptions.MASS_AND_IONS);

  public static final OptionalParameter<IonLibraryParameter> adductLibrary = new OptionalParameter<>(
      new IonLibraryParameter("Specific adducts",
          "Select the adducts used to calculate ion m/z values from neutral mass, formula, or SMILES. "
              + "When disabled, mzmine uses its default main adducts for the selected polarity.",
          IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_MAIN), false);

  public static final ImportTypeParameter columns = new ImportTypeParameter("Columns",
      "Select the columns you want to import from the library file.",
      LocalCSVDatabaseSearchParameters.importTypes);

  public AnnotationLocalCSVDatabaseSearchParameters() {
    super(dataBaseFile, massOptionsComboParameter, filterSamplesColumn, adductLibrary.cloneParameter(),
        columns);
  }

  @Override
  public @NotNull Map<String, Parameter<?>> getNameParameterMap() {
    final Map<String, Parameter<?>> parameters = super.getNameParameterMap();
    parameters.put("Override adducts", getParameter(adductLibrary));
    return parameters;
  }

  @Override
  public void handleLoadedParameters(@NotNull final Map<String, Parameter<?>> loadedParams,
      final int loadedVersion) {
    super.handleLoadedParameters(loadedParams, loadedVersion);
    if (!loadedParams.containsKey(adductLibrary.getName())) {
      setParameter(adductLibrary, false);
    }
  }

  /**
   * Returns the configured adduct override when selected, otherwise the same polarity-specific
   * library that the wizard used before this setting was introduced.
   */
  public @NotNull IonLibrary getEffectiveIonLibrary(@NotNull final WizardMsPolarity polarity) {
    if (getValue(adductLibrary)) {
      return Objects.requireNonNull(getEmbeddedParameterValue(adductLibrary));
    }
    return switch (polarity) {
      case Positive -> IonLibraries.MZMINE_DEFAULT_POS_MAIN;
      case Negative -> IonLibraries.MZMINE_DEFAULT_NEG_MAIN;
      case No_filter -> IonLibraries.MZMINE_DEFAULT_DUAL_POLARITY_MAIN;
    };
  }

  public enum MassOptions {
    PRECURSOR_MZ, MASS_AND_IONS;

    @Override
    public String toString() {
      return switch (this) {
        case MASS_AND_IONS -> "as calculated from neutral mass (or formula/smiles) + ions";
        case PRECURSOR_MZ -> "as listed in database";
      };
    }
  }
}
