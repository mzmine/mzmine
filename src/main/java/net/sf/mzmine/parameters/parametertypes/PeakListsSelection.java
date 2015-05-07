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

package net.sf.mzmine.parameters.parametertypes;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;

import com.google.common.base.Strings;

public class PeakListsSelection implements Cloneable {

    private PeakListSelectionType selectionType = PeakListSelectionType.GUI_SELECTED_PEAKLISTS;
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

                // Generate a regular expression, replacing * with .*
                try {
                    final StringBuilder regex = new StringBuilder("^");
                    String sections[] = namePattern.split("\\*", -1);
                    for (int i = 0; i < sections.length; i++) {
                        if (i > 0)
                            regex.append(".*");
                        regex.append(Pattern.quote(sections[i]));
                    }
                    regex.append("$");

                    if (plName.matches(regex.toString())) {
                        if (matchingPeakLists.contains(pl))
                            continue;
                        matchingPeakLists.add(pl);
                        continue plCheck;
                    }
                } catch (PatternSyntaxException e) {
                    e.printStackTrace();
                    continue;
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

    public PeakListSelectionType getSelectionType() {
        return selectionType;
    }

    public void setSelectionType(PeakListSelectionType selectionType) {
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

}
