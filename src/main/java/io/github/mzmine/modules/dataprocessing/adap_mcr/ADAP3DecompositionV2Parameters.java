/*
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */
package io.github.mzmine.modules.dataprocessing.adap_mcr;

import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsSelectionType;
import java.awt.Window;
import java.text.NumberFormat;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.util.ExitCode;

/**
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionV2Parameters extends SimpleParameterSet {

  public static final FeatureListsParameter CHROMATOGRAM_LISTS =
      new FeatureListsParameter("Chromatograms", 1, Integer.MAX_VALUE);

  public static final FeatureListsParameter PEAK_LISTS =
      new FeatureListsParameter("Peaks", 1, Integer.MAX_VALUE);

  // ------------------------------------------------------------------------
  // ----- First-phase parameters -------------------------------------------
  // ------------------------------------------------------------------------

  public static final DoubleParameter PREF_WINDOW_WIDTH = new DoubleParameter(
      "Deconvolution window width (min)", "Preferred width of deconvolution windows (in minutes).",
      NumberFormat.getNumberInstance(), 0.2);

  // ------------------------------------------------------------------------
  // ----- End of First-phase parameters ------------------------------------
  // ------------------------------------------------------------------------

  // ------------------------------------------------------------------------
  // ----- Second-phase parameters ------------------------------------------
  // ------------------------------------------------------------------------

  public static final DoubleParameter RET_TIME_TOLERANCE = new DoubleParameter(
      "Retention time tolerance (min)",
      "Retention time tolerance value (between 0 and 1) is used for determine the number of components"
          + " in a window. The larger tolerance, the smaller components are determined.",
      NumberFormat.getNumberInstance(), 0.05, 0.0, Double.MAX_VALUE);

  public static final IntegerParameter MIN_CLUSTER_SIZE = new IntegerParameter(
      "Minimum Number of Peaks", "Minimum number of peaks that can form a component", 1);

  public static final BooleanParameter ADJUST_APEX_RET_TIME = new BooleanParameter(
      "Adjust Apex Ret Times",
      "If this option is checked, the apex retention time is calculated by fitting a parabola into "
          + "the top half of an EIC peak",
      false);

  // ------------------------------------------------------------------------
  // ----- End of Second-phase parameters -----------------------------------
  // ------------------------------------------------------------------------

  public static final StringParameter SUFFIX = new StringParameter("Suffix",
      "This string is added to feature list name as suffix", "Spectral Deconvolution");

  public static final BooleanParameter AUTO_REMOVE =
      new BooleanParameter("Remove original feature lists",
          "If checked, original chromomatogram and feature lists will be removed");

  public ADAP3DecompositionV2Parameters() {
    super(new Parameter[] {CHROMATOGRAM_LISTS, PEAK_LISTS, PREF_WINDOW_WIDTH, RET_TIME_TOLERANCE,
        MIN_CLUSTER_SIZE, ADJUST_APEX_RET_TIME, SUFFIX, AUTO_REMOVE},
        "https://mzmine.github.io/mzmine_documentation/module_docs/featdet_multivariate_curve_res/featdet_multivar_curve_res.html");
  }

  @Override
  public ExitCode showSetupDialog(boolean valueCheckRequired) {
    CHROMATOGRAM_LISTS.setValue(FeatureListsSelectionType.GUI_SELECTED_FEATURELISTS);
    PEAK_LISTS.setValue(FeatureListsSelectionType.GUI_SELECTED_FEATURELISTS);

    final ADAP3DecompositionV2SetupDialog dialog =
        new ADAP3DecompositionV2SetupDialog(valueCheckRequired, this);

    dialog.showAndWait();
    return dialog.getExitCode();
  }
}
