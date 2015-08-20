/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.parametertypes.selectors;

import java.util.ArrayList;

import com.google.common.base.Strings;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.TextUtils;

public class PeakListsSelection implements Cloneable {

    private PeakListsSelectionType selectionType = PeakListsSelectionType.GUI_SELECTED_PEAKLISTS;
    private PeakList specificPeakLists[];
    private String namePattern;
    private PeakList batchLastPeakLists[];

    public PeakList[] getMatchingPeakLists() {

        switch (selectionType) {

        case GUI_SELECTED_PEAKLISTS:
            return MZmineCore.getDesktop().getSelectedPeakLists();
        case ALL_PEAKLISTS:
            return MZmineCore.getProjectManager().getCurrentProject()
                    .getPeakLists();
        case SPECIFIC_PEAKLISTS:
            if (specificPeakLists == null)
                return new PeakList[0];
            return specificPeakLists;
        case NAME_PATTERN:
            if (Strings.isNullOrEmpty(namePattern))
                return new PeakList[0];
            ArrayList<PeakList> matchingPeakLists = new ArrayList<PeakList>();
            PeakList allPeakLists[] = MZmineCore.getProjectManager()
                    .getCurrentProject().getPeakLists();

            plCheck: for (PeakList pl : allPeakLists) {

                final String plName = pl.getName();

                final String regex = TextUtils
                        .createRegexFromWildcards(namePattern);

                if (plName.matches(regex)) {
                    if (matchingPeakLists.contains(pl))
                        continue;
                    matchingPeakLists.add(pl);
                    continue plCheck;
                }
            }
            return matchingPeakLists.toArray(new PeakList[0]);
        case BATCH_LAST_PEAKLISTS:
            if (batchLastPeakLists == null)
                return new PeakList[0];
            return batchLastPeakLists;
        }

        throw new IllegalStateException("This code should be unreachable");

    }

    public PeakListsSelectionType getSelectionType() {
        return selectionType;
    }

    public void setSelectionType(PeakListsSelectionType selectionType) {
        this.selectionType = selectionType;
    }

    public PeakList[] getSpecificPeakLists() {
        return specificPeakLists;
    }

    public void setSpecificPeakLists(PeakList[] specificPeakLists) {
        this.specificPeakLists = specificPeakLists;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    public void setBatchLastPeakLists(PeakList[] batchLastPeakLists) {
        this.batchLastPeakLists = batchLastPeakLists;
    }

    public PeakListsSelection clone() {
        PeakListsSelection newSelection = new PeakListsSelection();
        newSelection.selectionType = selectionType;
        newSelection.specificPeakLists = specificPeakLists;
        newSelection.namePattern = namePattern;
        return newSelection;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        PeakList pls[] = getMatchingPeakLists();
        for (int i = 0; i < pls.length; i++) {
            if (i > 0)
                str.append("\n");
            str.append(pls[i].getName());
        }
        return str.toString();
    }
}
