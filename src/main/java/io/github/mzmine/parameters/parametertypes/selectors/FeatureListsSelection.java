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

package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.data.FeatureList;
import java.util.ArrayList;

import com.google.common.base.Strings;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.TextUtils;

public class FeatureListsSelection implements Cloneable {

  private FeatureListsSelectionType selectionType = FeatureListsSelectionType.GUI_SELECTED_FEATURELISTS;
  private FeatureList specificFeatureLists[];
  private String namePattern;
  private FeatureList batchLastFeatureLists[];

  public FeatureList[] getMatchingFeatureLists() {

    switch (selectionType) {

      case GUI_SELECTED_FEATURELISTS:
        return MZmineCore.getDesktop().getSelectedPeakLists();
      case ALL_FEATURELISTS:
        return MZmineCore.getProjectManager().getCurrentProject().getFeatureLists().toArray(FeatureList[]::new);
      case SPECIFIC_FEATURELISTS:
        if (specificFeatureLists == null)
          return new FeatureList[0];
        return specificFeatureLists;
      case NAME_PATTERN:
        if (Strings.isNullOrEmpty(namePattern))
          return new FeatureList[0];
        ArrayList<FeatureList> matchingFeatureLists = new ArrayList<FeatureList>();
        FeatureList allFeatureLists[] = MZmineCore.getProjectManager().getCurrentProject()
            .getFeatureLists().toArray(FeatureList[]::new);

        plCheck: for (FeatureList pl : allFeatureLists) {

          final String plName = pl.getName();

          final String regex = TextUtils.createRegexFromWildcards(namePattern);

          if (plName.matches(regex)) {
            if (matchingFeatureLists.contains(pl))
              continue;
            matchingFeatureLists.add(pl);
            continue plCheck;
          }
        }
        return matchingFeatureLists.toArray(new FeatureList[0]);
      case BATCH_LAST_FEATURELISTS:
        if (batchLastFeatureLists == null)
          return new FeatureList[0];
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
    this.specificFeatureLists = specificFeatureLists;
  }

  public String getNamePattern() {
    return namePattern;
  }

  public void setNamePattern(String namePattern) {
    this.namePattern = namePattern;
  }

  public void setBatchLastFeatureLists(FeatureList[] batchLastFeatureLists) {
    this.batchLastFeatureLists = batchLastFeatureLists;
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
