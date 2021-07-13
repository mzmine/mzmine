/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.dataprocessing.filter_groupms2;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;
import org.jetbrains.annotations.NotNull;

public class GroupMS2Parameters extends SimpleParameterSet {

  public static final FeatureListsParameter PEAK_LISTS = new FeatureListsParameter();

  public static final MZToleranceParameter mzTol = new MZToleranceParameter();

  public static final RTToleranceParameter rtTol = new RTToleranceParameter();

  public static final BooleanParameter limitRTByFeature = new BooleanParameter("Limit by RT edges",
      "Use the feature's edges (retention time) as a filter.", false);

  public static final BooleanParameter combineTimsMsMs = new BooleanParameter(
      "Combine MS/MS spectra (TIMS)",
      "If checked, all assigned MS/MS spectra with the same collision energy will be merged into a single MS/MS spectrum.",
      false);

  public GroupMS2Parameters() {
    super(new Parameter[] {PEAK_LISTS, rtTol, mzTol, limitRTByFeature, combineTimsMsMs});
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }
}
