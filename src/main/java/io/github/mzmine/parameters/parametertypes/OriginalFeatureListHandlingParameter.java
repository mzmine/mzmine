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

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import org.w3c.dom.Element;

/**
 * Harmonizes the handling of original feature lists
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public class OriginalFeatureListHandlingParameter extends
    ComboParameter<OriginalFeatureListOption> {

  final boolean includeProcessInPlace;

  public OriginalFeatureListHandlingParameter(boolean includeProcessInPlace,
      OriginalFeatureListOption startValue) {
    super("Original feature list",
        "Defines the processing. Standard is to keep the original feature list and create a new "
        + "processed list. REMOVE saves memory. PROCESS IN PLACE is an advanced option to process "
        + "directly in the feature list and reduce memory consumption more - this might come with "
        + "side effects, apply with caution.",
        includeProcessInPlace ? OriginalFeatureListOption.values()
            : new OriginalFeatureListOption[]{OriginalFeatureListOption.KEEP,
                OriginalFeatureListOption.REMOVE}, startValue);
    this.includeProcessInPlace = includeProcessInPlace;
    this.value = startValue;
  }

  public OriginalFeatureListHandlingParameter(boolean includeProcessInPlace) {
    this(includeProcessInPlace, OriginalFeatureListOption.KEEP);
  }

  public enum OriginalFeatureListOption {
    KEEP, REMOVE, PROCESS_IN_PLACE;

    @Override
    public String toString() {
      return super.toString().replaceAll("_", " ");
    }

    /**
     * Add new list to project or just reflect the new name. Remove old feature list if option.
     *
     * @param suffix          add sufix to name for process in place
     * @param project         the project its added to
     * @param newFeatureList  the added feature list
     * @param origFeatureList the original feature list
     */
    public void reflectNewFeatureListToProject(String suffix, MZmineProject project,
        FeatureList newFeatureList, FeatureList origFeatureList) {
      switch (this) {
        case KEEP -> project.addFeatureList(newFeatureList);
        case REMOVE -> {
          project.removeFeatureList(origFeatureList);
          // Add new feature list to the project
          project.addFeatureList(newFeatureList);
        }
        case PROCESS_IN_PLACE -> newFeatureList.setName(newFeatureList.getName() + ' ' + suffix);
      }
    }

  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    super.loadValueFromXML(xmlElement);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    super.saveValueToXML(xmlElement);
  }

  @Override
  public ComboParameter<OriginalFeatureListOption> cloneParameter() {
    var clone = new OriginalFeatureListHandlingParameter(includeProcessInPlace, value);
    return clone;
  }
}
