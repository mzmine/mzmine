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

package io.github.mzmine.modules.dataprocessing.id_camera;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.PercentParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.util.R.REngineType;
import java.text.NumberFormat;
import javafx.collections.FXCollections;

/**
 * Parameters for a <code>CameraSearchTask</code>.
 *
 */
@Deprecated
public class CameraSearchParameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  // Sigma.
  public static final DoubleParameter FWHM_SIGMA = new DoubleParameter("FWHM sigma",
      "Fitted peak (Gaussian) width multiplier used when grouping peaks by RT",
      NumberFormat.getNumberInstance(), 0.2, 0.0, null);

  // Percentage of FWHM.
  public static final PercentParameter FWHM_PERCENTAGE = new PercentParameter("FWHM percentage",
      "Percentage of the FWHM of a peak used when grouping peaks by RT", 0.01, 0.0, 1.0);

  // Max charge.
  public static final IntegerParameter ISOTOPES_MAX_CHARGE =
      new IntegerParameter("Isotopes max. charge",
          "The maximum charge considered when identifying isotopes", 3, 1, null);

  // Max isotopes.
  public static final IntegerParameter ISOTOPES_MAXIMUM = new IntegerParameter(
      "Isotopes max. per cluster", "The maximum number of isotopes per cluster", 4, 0, null);

  // Isotope m/z tolerance.
  public static final MZToleranceParameter ISOTOPES_MZ_TOLERANCE =
      new MZToleranceParameter("Isotopes mass tolerance",
          "Mass tolerance used when identifying isotopes (both values required)");

  // Correlation threshold.
  public static final DoubleParameter CORRELATION_THRESHOLD =
      new DoubleParameter("Correlation threshold",
          "Minimum correlation required between two peaks' EICs when grouping by peak shape",
          NumberFormat.getNumberInstance(), 0.9, 0.0, 1.0);

  // Correlation threshold.
  public static final DoubleParameter CORRELATION_P_VALUE =
      new DoubleParameter("Correlation p-value",
          "Required p-value when testing the significance of peak shape correlation",
          NumberFormat.getNumberInstance(), 0.05, 0.0, 1.0);

  // Ionization Polarity
  public static final ComboParameter POLARITY = new ComboParameter("Ionization Polarity",
      "Ionization Polarity", FXCollections.observableArrayList("positive", "negative"), "positive");

  public static final BooleanParameter DONT_SPLIT_ISOTOPES =
      new BooleanParameter("Do not split isotopes",
          "If checked, isotopes will not be split even if shape-similarity is low", true);

  public static final String FIND_ISOTOPES_FIRST =
      "Perform Isotope search before Shape correlation";
  public static final String GROUP_CORR_FIRST = "Perform Shape correlation before Isotope search";

  public static final ComboParameter<String> ORDER =
      new ComboParameter<>("Order", "Order of Isotope search and Shape correlation steps",
          FXCollections.observableArrayList(FIND_ISOTOPES_FIRST, GROUP_CORR_FIRST),
          FIND_ISOTOPES_FIRST);

  public static final BooleanParameter CREATE_NEW_LIST =
      new BooleanParameter("Create new list", "If checked, a new list will be created", true);

  public static final String GROUP_BY_ISOTOPE = "Isotope ID";
  public static final String GROUP_BY_PCGROUP = "PCGroup ID";

  public static final ComboParameter<String> GROUP_BY =
      new ComboParameter<>("Group peaks by", "Order of Isotope search and Shape correlation steps",
          FXCollections.observableArrayList(GROUP_BY_ISOTOPE, GROUP_BY_PCGROUP), GROUP_BY_ISOTOPE);

  public static final BooleanParameter INCLUDE_SINGLETONS =
      new BooleanParameter("Include singletons",
          "If checked, features with no found isotope pattern will be included", false);

  public static final StringParameter SUFFIX = new StringParameter("Suffix",
      "This string is added to feature list name as suffix", "CAMERA");

  /**
   * R engine type.
   */
  public static final ComboParameter<REngineType> RENGINE_TYPE = new ComboParameter<REngineType>(
      "R engine", "The R engine to be used for communicating with R.",
      FXCollections.observableArrayList(REngineType.values()), REngineType.RCALLER);

  public CameraSearchParameters() {

    super(new Parameter[] {PEAK_LISTS, FWHM_SIGMA, FWHM_PERCENTAGE, ISOTOPES_MAX_CHARGE,
        ISOTOPES_MAXIMUM, ISOTOPES_MZ_TOLERANCE, CORRELATION_THRESHOLD, CORRELATION_P_VALUE,
        POLARITY, DONT_SPLIT_ISOTOPES, ORDER, CREATE_NEW_LIST, GROUP_BY, INCLUDE_SINGLETONS, SUFFIX,
        RENGINE_TYPE});
  }
}
