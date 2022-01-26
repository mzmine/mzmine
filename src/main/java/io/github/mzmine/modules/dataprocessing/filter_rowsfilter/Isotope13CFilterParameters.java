/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
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
        elements});
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
