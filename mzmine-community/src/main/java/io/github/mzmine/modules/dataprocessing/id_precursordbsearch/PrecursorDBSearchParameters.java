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

package io.github.mzmine.modules.dataprocessing.id_precursordbsearch;

import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class PrecursorDBSearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter featureLists = new FeatureListsParameter();

  public static final SpectralLibrarySelectionParameter libraries = new SpectralLibrarySelectionParameter();

  public static final MZToleranceParameter mzTolerancePrecursor = new MZToleranceParameter(
      "Precursor m/z tolerance", "Precursor m/z tolerance is used to filter library entries", 0.001,
      5);

  public static final OptionalParameter<RTToleranceParameter> rtTolerance = new OptionalParameter<>(
      new RTToleranceParameter());

  public static final OptionalParameter<MobilityToleranceParameter> mobTolerance = new OptionalParameter<>(
      new MobilityToleranceParameter(new MobilityTolerance(0.01f)));
  public static final OptionalParameter<PercentParameter> ccsTolerance = new OptionalParameter<>(
      new PercentParameter("CCS tolerance (%)",
          "Maximum allowed difference (in per cent) for two ccs values.", 0.05));

  public PrecursorDBSearchParameters() {
    super(
        "https://mzmine.github.io/mzmine_documentation/module_docs/id_prec_local_spectra_lib/local-spectra-lib-search.html",
        featureLists, libraries, mzTolerancePrecursor, rtTolerance, mobTolerance, ccsTolerance);
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
