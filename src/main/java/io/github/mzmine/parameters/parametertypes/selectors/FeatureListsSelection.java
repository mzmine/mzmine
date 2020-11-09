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

  private FeatureListsSelectionType selectionType = FeatureListsSelectionType.GUI_SELECTED_PEAKLISTS;
  private FeatureList specificPeakLists[];
  private String namePattern;
  private FeatureList batchLastPeakLists[];

  public FeatureList[] getMatchingPeakLists() {

    switch (selectionType) {

      case GUI_SELECTED_PEAKLISTS:
        return MZmineCore.getDesktop().getSelectedPeakLists();
      case ALL_PEAKLISTS:
        return MZmineCore.getProjectManager().getCurrentProject().getPeakLists();
      case SPECIFIC_PEAKLISTS:
        if (specificPeakLists == null)
          return new FeatureList[0];
        return specificPeakLists;
      case NAME_PATTERN:
        if (Strings.isNullOrEmpty(namePattern))
          return new FeatureList[0];
        ArrayList<FeatureList> matchingPeakLists = new ArrayList<FeatureList>();
        FeatureList allPeakLists[] = MZmineCore.getProjectManager().getCurrentProject().getPeakLists();

        plCheck: for (FeatureList pl : allPeakLists) {

          final String plName = pl.getName();

          final String regex = TextUtils.createRegexFromWildcards(namePattern);

          if (plName.matches(regex)) {
            if (matchingPeakLists.contains(pl))
              continue;
            matchingPeakLists.add(pl);
            continue plCheck;
          }
        }
        return matchingPeakLists.toArray(new FeatureList[0]);
      case BATCH_LAST_PEAKLISTS:
        if (batchLastPeakLists == null)
          return new FeatureList[0];
        return batchLastPeakLists;
    }

    throw new IllegalStateException("This code should be unreachable");

  }

  public FeatureListsSelectionType getSelectionType() {
    return selectionType;
  }

  public void setSelectionType(FeatureListsSelectionType selectionType) {
    this.selectionType = selectionType;
  }

  public FeatureList[] getSpecificPeakLists() {
    return specificPeakLists;
  }

  public void setSpecificPeakLists(FeatureList[] specificPeakLists) {
    this.specificPeakLists = specificPeakLists;
  }

  public String getNamePattern() {
    return namePattern;
  }

  public void setNamePattern(String namePattern) {
    this.namePattern = namePattern;
  }

  public void setBatchLastPeakLists(FeatureList[] batchLastPeakLists) {
    this.batchLastPeakLists = batchLastPeakLists;
  }

  public FeatureListsSelection clone() {
    FeatureListsSelection newSelection = new FeatureListsSelection();
    newSelection.selectionType = selectionType;
    newSelection.specificPeakLists = specificPeakLists;
    newSelection.namePattern = namePattern;
    return newSelection;
  }

  public String toString() {
    StringBuilder str = new StringBuilder();
    FeatureList pls[] = getMatchingPeakLists();
    for (int i = 0; i < pls.length; i++) {
      if (i > 0)
        str.append("\n");
      str.append(pls[i].getName());
    }
    return str.toString();
  }
}
