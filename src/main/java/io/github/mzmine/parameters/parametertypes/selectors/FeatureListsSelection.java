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

package io.github.mzmine.parameters.parametertypes.selectors;

import com.google.common.base.Strings;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.TextUtils;
import java.util.ArrayList;
import java.util.stream.Stream;

public class FeatureListsSelection implements Cloneable {

  private FeatureListsSelectionType selectionType = FeatureListsSelectionType.GUI_SELECTED_FEATURELISTS;
  private ModularFeatureList specificFeatureLists[];
  private String namePattern;
  private ModularFeatureList batchLastFeatureLists[];


  /**
   * Uses specific feature lists
   * @param flists specific feature lists
   */
  public FeatureListsSelection(ModularFeatureList... flists) {
    this();
    specificFeatureLists = flists;
    selectionType = FeatureListsSelectionType.SPECIFIC_FEATURELISTS;
  }
  public FeatureListsSelection(FeatureListsSelectionType selectionType) {
    this();
    this.selectionType = selectionType;
  }
  public FeatureListsSelection() {
  }

  public ModularFeatureList[] getMatchingFeatureLists() {

    switch (selectionType) {

      case GUI_SELECTED_FEATURELISTS:
        return Stream.of(MZmineCore.getDesktop().getSelectedPeakLists())
            .map(ModularFeatureList.class::cast).toArray(ModularFeatureList[]::new);
      case ALL_FEATURELISTS:
        return MZmineCore.getProjectManager().getCurrentProject().getCurrentFeatureLists()
            .toArray(ModularFeatureList[]::new);
      case SPECIFIC_FEATURELISTS:
        if (specificFeatureLists == null) {
          return new ModularFeatureList[0];
        }
        return specificFeatureLists;
      case NAME_PATTERN:
        if (Strings.isNullOrEmpty(namePattern)) {
          return new ModularFeatureList[0];
        }
        ArrayList<ModularFeatureList> matchingFeatureLists = new ArrayList<>();
        ModularFeatureList allFeatureLists[] = MZmineCore.getProjectManager().getCurrentProject()
            .getCurrentFeatureLists().toArray(ModularFeatureList[]::new);

        plCheck:
        for (ModularFeatureList pl : allFeatureLists) {

          final String plName = pl.getName();

          final String regex = TextUtils.createRegexFromWildcards(namePattern);

          if (plName.matches(regex)) {
            if (matchingFeatureLists.contains(pl)) {
              continue;
            }
            matchingFeatureLists.add(pl);
            continue plCheck;
          }
        }
        return matchingFeatureLists.toArray(new ModularFeatureList[0]);
      case BATCH_LAST_FEATURELISTS:
        if (batchLastFeatureLists == null) {
          return new ModularFeatureList[0];
        }
        return batchLastFeatureLists;
    }

    throw new IllegalStateException("This code should be unreachable");

  }

  public FeatureListsSelectionType getSelectionType() {
    return selectionType;
  }

  public void setSelectionType(FeatureListsSelectionType selectionType) {
    this.selectionType = selectionType;
  }

  public FeatureList[] getSpecificFeatureLists() {
    return specificFeatureLists;
  }

  public void setSpecificFeatureLists(FeatureList[] specificFeatureLists) {
    this.specificFeatureLists = toModular(specificFeatureLists);
  }

  public String getNamePattern() {
    return namePattern;
  }

  public void setNamePattern(String namePattern) {
    this.namePattern = namePattern;
  }

  public void setBatchLastFeatureLists(FeatureList[] batchLastFeatureLists) {
    this.batchLastFeatureLists = toModular(batchLastFeatureLists);
  }

  public ModularFeatureList[] toModular(FeatureList[] flist) {
    return Stream.of(flist).map(ModularFeatureList.class::cast).toArray(ModularFeatureList[]::new);
  }

  public FeatureListsSelection clone() {
    FeatureListsSelection newSelection = new FeatureListsSelection();
    newSelection.selectionType = selectionType;
    newSelection.specificFeatureLists = specificFeatureLists;
    newSelection.namePattern = namePattern;
    return newSelection;
  }

  public String toString() {
    StringBuilder str = new StringBuilder();
    FeatureList pls[] = getMatchingFeatureLists();
    for (int i = 0; i < pls.length; i++) {
      if (i > 0)
        str.append("\n");
      str.append(pls[i].getName());
    }
    return str.toString();
  }
}
