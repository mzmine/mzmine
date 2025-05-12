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

package io.github.mzmine.modules.dataprocessing.isolab_natabundance;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.ToleranceType;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import java.text.NumberFormat;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class IsotopeNaturalAbundanceParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "corrected");

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(
      ToleranceType.SCAN_TO_SCAN);

  public static final OptionalParameter<DoubleParameter> resolution = new OptionalParameter<>(
      new DoubleParameter("Resolution",
          "Resolution at which the data was originally measured. Will trigger high resolution correction."));

  public static final OptionalParameter<DoubleParameter> mzOfResolution = new OptionalParameter<>(
      new DoubleParameter("mz of the resolution",
          "mz at which the resolution has been determined. Required to trigger high resolution correction."));

  public static final OptionalParameter<StringParameter> resolutionFormulaCode = new OptionalParameter<>(
      new StringParameter("Instrument resolution type",
          "Type of instrument resolution. Choose between 'orbitrap', 'ft-icr' and 'constant'. Required to trigger high resolution correction."));

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  public static final IntegerParameter charge = new IntegerParameter("Charge",
      "Charge of the measured ion, usually 1 or -1.");

  // tracer purity as an array of double values
  public static final DoubleParameter tracerPurity = new DoubleParameter("Tracer purity",
      "Purity of the tracer isotope as a ratio of 0 to 1.", NumberFormat.getNumberInstance(), 1.00);

  public static final OptionalParameter<MobilityToleranceParameter> mobilityTolerace = new OptionalParameter<>(
      new MobilityToleranceParameter("Mobility tolerance",
          "If enabled (and mobility dimension was recorded), "
              + "isotopic peaks will only be grouped if they fit within the given tolerance."));

  // set background value to replace 0 values
  public static final OptionalParameter<DoubleParameter> backgroundValue = new OptionalParameter<>(
      new DoubleParameter("Background value",
          "If enabled, the background value will be replaced by the given value.",
          NumberFormat.getNumberInstance(), null));

  public static final StringParameter tracerIsotope = new StringParameter("Tracer isotope",
      "Tracer isotope name", "13C");

  // correct for natural abundance of the tracer isotope
  //public static final OptionalParameter<BooleanParameter> correct_NA_tracer = new OptionalParameter<>(
  //    new BooleanParameter("Tracer isotope correction",
  //        "If enabled, the natural abundance of the tracer isotope will be corrected.", true));

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public IsotopeNaturalAbundanceParameters() {
    super(new Parameter[]{peakLists, suffix, mzTolerance, resolution, mzOfResolution,
        resolutionFormulaCode, rtTolerance, charge, tracerPurity, mobilityTolerace, backgroundValue,
        tracerIsotope, handleOriginal}, "https://mzmine.github.io/mzmine_documentation");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public Map<String, Parameter<?>> getNameParameterMap() {
    // parameters were renamed but stayed the same type
    var nameParameterMap = super.getNameParameterMap();
    // we use the same parameters here so no need to increment the version. Loading will work fine
    nameParameterMap.put("m/z tolerance", mzTolerance);
    return nameParameterMap;
  }
}
