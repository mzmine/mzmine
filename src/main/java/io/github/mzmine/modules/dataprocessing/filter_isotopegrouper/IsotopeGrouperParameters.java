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

package io.github.mzmine.modules.dataprocessing.filter_isotopegrouper;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class IsotopeGrouperParameters extends SimpleParameterSet {

  public static final String ChooseTopIntensity = "Most intense";
  public static final String ChooseLowestMZ = "Lowest m/z";

  public static final String[] representativeIsotopeValues = {ChooseTopIntensity, ChooseLowestMZ};

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  public static final StringParameter suffix = new StringParameter("Name suffix",
      "Suffix to be added to feature list name", "deisotoped");

  public static final MZToleranceParameter mzTolerance = new MZToleranceParameter();

  public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

  public static final OptionalParameter<MobilityToleranceParameter> mobilityTolerace = new OptionalParameter<>(
      new MobilityToleranceParameter("Mobility tolerance",
          "If enabled (and mobility dimension was recorded), "
              + "isotopic peaks will only be grouped if they fit within the given tolerance."));

  public static final BooleanParameter monotonicShape = new BooleanParameter("Monotonic shape",
      "If true, then monotonically decreasing height of isotope pattern is required");


  public static final BooleanParameter keepAllMS2 = new BooleanParameter(
      "Never remove feature with MS2",
      "If checked, all rows with MS2 are retained without applying any further filters on them.",
      true);

  public static final IntegerParameter maximumCharge = new IntegerParameter("Maximum charge",
      "Maximum charge to consider for detecting the isotope patterns");

  public static final ComboParameter<String> representativeIsotope = new ComboParameter<String>(
      "Representative isotope",
      "Which peak should represent the whole isotope pattern. For small molecular weight\n"
          + "compounds with monotonically decreasing isotope pattern, the most intense isotope\n"
          + "should be representative. For high molecular weight peptides, the lowest m/z\n"
          + "peptides, the lowest m/z isotope may be the representative.",
      representativeIsotopeValues);

  public static final OriginalFeatureListHandlingParameter handleOriginal = new OriginalFeatureListHandlingParameter(
      true);

  public IsotopeGrouperParameters() {
    super(new Parameter[]{peakLists, suffix, mzTolerance, rtTolerance, mobilityTolerace,
            monotonicShape, maximumCharge, representativeIsotope, keepAllMS2, handleOriginal},
        "https://mzmine.github.io/mzmine_documentation/module_docs/filter_isotope_filter/isotope_filter.html");
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
