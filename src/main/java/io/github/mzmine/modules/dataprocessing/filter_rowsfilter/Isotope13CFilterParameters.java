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

package io.github.mzmine.modules.dataprocessing.filter_rowsfilter;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.elements.ElementsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.Nullable;

public class Isotope13CFilterParameters extends SimpleParameterSet {

  public static final ElementsParameter elements = new ElementsParameter("Exclude isotopes",
      "Similar to remove 13C isotopes, this parameter allows to exclude main signals (rows) "
      + "that are flagged as isotopes of this element, by searching for preceding signals.");
  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter(0.0005, 10);
  public static final IntegerParameter maxCharge = new IntegerParameter("Max charge",
      "Maximum allowed charge state", 1);
  public static final BooleanParameter removeIfMainIs13CIsotope = new BooleanParameter(
      "Remove if 13C",
      "Checks if the main m/z (row) is a 13C isotope of a preceding signal. Default: true", true);
  public static final BooleanParameter applyMinCEstimation = new BooleanParameter(
      "Estimate minimum carbon",
      "Estimates the minimum number of carbons based on m/z and mass defect. Uses estimates derived from the COCONUT database. Default: true",
      true);

  private static final Logger logger = Logger.getLogger(Isotope13CFilterParameters.class.getName());

  public Isotope13CFilterParameters() {
    super(new Parameter[]{mzTolerance, maxCharge, applyMinCEstimation, removeIfMainIs13CIsotope,
        elements},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_isotope_filter/isotope_filter.html");
  }

  /**
   * Creates a filter instance to validate 13C isotope pattern
   *
   * @param parameters instance of this parameter class
   * @return a filter or null
   */
  @Nullable
  public static Isotope13CFilter createFilter(ParameterSet parameters) {
    try {
      return new Isotope13CFilter(parameters.getValue(mzTolerance), parameters.getValue(maxCharge),
          parameters.getValue(removeIfMainIs13CIsotope), parameters.getValue(elements),
          parameters.getValue((applyMinCEstimation)));
    } catch (Exception ex) {
      logger.log(Level.WARNING, "Cannot create filter. " + ex.getMessage(), ex);
      return null;
    }
  }

  /**
   * Creates a filter instance to validate 13C isotope pattern
   *
   * @return a filter or null
   */
  public Isotope13CFilter createFilter() {
    return createFilter(this);
  }
}
