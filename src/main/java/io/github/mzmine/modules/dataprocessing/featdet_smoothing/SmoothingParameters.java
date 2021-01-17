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

/*
 * Code created was by or on behalf of Syngenta and is released under the open source license in use
 * for the pre-existing code or project. Syngenta does not assert ownership or copyright any over
 * pre-existing work.
 */

package io.github.mzmine.modules.dataprocessing.featdet_smoothing;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;

/**
 * Defines smoothing task parameters.
 * 
 * @author $Author$
 * @version $Revision$
 */
public class SmoothingParameters extends SimpleParameterSet {

  public static final FeatureListsParameter peakLists = new FeatureListsParameter();

  /**
   * Raw data file suffix.
   */
  public static final StringParameter SUFFIX = new StringParameter("Filename suffix",
      "Suffix to be appended to peak-list file name", "smoothed");

  /**
   * Filter width.
   */
  public static final ComboParameter<Integer> FILTER_WIDTH = new ComboParameter<Integer>(
      "Filter width (retention time)", "Number of data point covered by the smoothing filter",
      new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5);

  public static final ComboParameter<Integer> MOBILITY_FILTER_WIDTH = new ComboParameter<Integer>(
      "Filter width (mobility)",
      "Number of data point covered by the smoothing filter. Will not affect smoothing if there is no mobility dimension.",
      new Integer[]{0, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25}, 5);

  /**
   * Remove original data file.
   */
  public static final BooleanParameter REMOVE_ORIGINAL =
      new BooleanParameter("Remove original feature list",
          "If checked, the source feature list will be replaced by the smoothed version");

  /**
   * Create the parameter set.
   */
  public SmoothingParameters() {
    super(new Parameter[]{peakLists, SUFFIX, FILTER_WIDTH, MOBILITY_FILTER_WIDTH, REMOVE_ORIGINAL});
  }
}
