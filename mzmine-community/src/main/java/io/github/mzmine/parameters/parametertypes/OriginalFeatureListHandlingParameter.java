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
        "Defines the processing.\nKEEP is to keep the original feature list and create a new "
        + "processed list.\nREMOVE saves memory.\nPROCESS IN PLACE is an advanced option to process directly in the feature list and reduce memory consumption more - this might come with "
        + "side effects, apply with caution.",
        includeProcessInPlace ? OriginalFeatureListOption.values()
            : new OriginalFeatureListOption[]{OriginalFeatureListOption.KEEP,
                OriginalFeatureListOption.REMOVE}, startValue);
    this.includeProcessInPlace = includeProcessInPlace;
    this.value = startValue;
  }

  public OriginalFeatureListHandlingParameter(String name, String description, boolean includeProcessInPlace) {
    super(name,
        description,
        includeProcessInPlace ? OriginalFeatureListOption.values()
            : new OriginalFeatureListOption[]{OriginalFeatureListOption.KEEP,
                OriginalFeatureListOption.REMOVE},
        OriginalFeatureListOption.KEEP);
    this.includeProcessInPlace = includeProcessInPlace;
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

  public boolean isIncludeProcessInPlace() {
    return includeProcessInPlace;
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
