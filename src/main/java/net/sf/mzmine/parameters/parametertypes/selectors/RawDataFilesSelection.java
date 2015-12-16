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

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.TextUtils;

public class RawDataFilesSelection implements Cloneable {

    private RawDataFilesSelectionType selectionType;
    private RawDataFile specificFiles[];
    private String namePattern;
    private RawDataFile batchLastFiles[];

    public RawDataFilesSelection() {
        this(RawDataFilesSelectionType.GUI_SELECTED_FILES);
    }

    public RawDataFilesSelection(RawDataFilesSelectionType selectionType) {
        this.selectionType = selectionType;
    }

    public RawDataFile[] getMatchingRawDataFiles() {

        switch (selectionType) {

        case GUI_SELECTED_FILES:
            return MZmineCore.getDesktop().getSelectedDataFiles();
        case ALL_FILES:
            return MZmineCore.getProjectManager().getCurrentProject()
                    .getDataFiles();
        case SPECIFIC_FILES:
            if (specificFiles == null)
                return new RawDataFile[0];
            return specificFiles;
        case NAME_PATTERN:
            if (Strings.isNullOrEmpty(namePattern))
                return new RawDataFile[0];
            ArrayList<RawDataFile> matchingDataFiles = new ArrayList<RawDataFile>();
            RawDataFile allDataFiles[] = MZmineCore.getProjectManager()
                    .getCurrentProject().getDataFiles();

            fileCheck: for (RawDataFile file : allDataFiles) {

                final String fileName = file.getName();

                final String regex = TextUtils
                        .createRegexFromWildcards(namePattern);

                if (fileName.matches(regex)) {
                    if (matchingDataFiles.contains(file))
                        continue;
                    matchingDataFiles.add(file);
                    continue fileCheck;
                }
            }
            return matchingDataFiles.toArray(new RawDataFile[0]);
        case BATCH_LAST_FILES:
            if (batchLastFiles == null)
                return new RawDataFile[0];
            return batchLastFiles;
        }

        throw new IllegalStateException("This code should be unreachable");

    }

    public RawDataFilesSelectionType getSelectionType() {
        return selectionType;
    }

    public void setSelectionType(RawDataFilesSelectionType selectionType) {
        this.selectionType = selectionType;
    }

    public RawDataFile[] getSpecificFiles() {
        return specificFiles;
    }

    public void setSpecificFiles(RawDataFile[] specificFiles) {
        this.specificFiles = specificFiles;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }

    public void setBatchLastFiles(RawDataFile[] batchLastFiles) {
        this.batchLastFiles = batchLastFiles;
    }

    public RawDataFilesSelection clone() {
        RawDataFilesSelection newSelection = new RawDataFilesSelection();
        newSelection.selectionType = selectionType;
        newSelection.specificFiles = specificFiles;
        newSelection.namePattern = namePattern;
        return newSelection;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        RawDataFile files[] = getMatchingRawDataFiles();
        for (int i = 0; i < files.length; i++) {
            if (i > 0)
                str.append("\n");
            str.append(files[i].getName());
        }
        return str.toString();
    }

}
