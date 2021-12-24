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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;

/**
 * Harmonizes the handling of original feature lists
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class OriginalFeatureListHandlingParameter extends
    ComboParameter<OriginalFeatureListOption> {

  public OriginalFeatureListHandlingParameter(boolean includeProcessInPlace) {
    super("Original feature list",
        "Defines the processing. Standard is to keep the original feature list and create a new "
        + "processed list. REMOVE saves memory. PROCESS IN PLACE is an advanced option to process "
        + "directly in the feature list and reduce memory consumption more - this might come with "
        + "side effects, apply with caution.",
        includeProcessInPlace ? OriginalFeatureListOption.values()
            : new OriginalFeatureListOption[]{OriginalFeatureListOption.KEEP,
                OriginalFeatureListOption.REMOVE}, OriginalFeatureListOption.KEEP);
  }

  public enum OriginalFeatureListOption {
    KEEP, REMOVE, PROCESS_IN_PLACE;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }
  }

}
