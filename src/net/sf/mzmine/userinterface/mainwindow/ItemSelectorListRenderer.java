/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.userinterface.mainwindow;

import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.io.RawDataFile;
import net.sf.mzmine.main.MZmineCore;

class ItemSelectorListRenderer extends DefaultListCellRenderer {

    /**
     * Main rendering method
     */
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean hasFocus) {

        // First get original rendered component
        final Component component = super.getListCellRendererComponent(list,
                value, index, isSelected, hasFocus);

        // Check if we are rendering list of raw data files
        if (value instanceof RawDataFile) {

            RawDataFile dataFile = (RawDataFile) value;

            boolean peakListSelected = false;

            PeakList selectedPeakLists[] = MZmineCore.getDesktop().getSelectedPeakLists();

            for (PeakList peakList : selectedPeakLists)
                if (peakList.hasRawDataFile(dataFile))
                    peakListSelected = true;

            if (peakListSelected) {
                Font font = component.getFont();
                font = font.deriveFont(Font.BOLD);
                component.setFont(font);
            }

        }

        // Check if we are rendering list of peak lists
        if (value instanceof PeakList) {

            PeakList peakList = (PeakList) value;

            boolean fileSelected = false;

            RawDataFile selectedDataFiles[] = MZmineCore.getDesktop().getSelectedDataFiles();

            for (RawDataFile dataFile : selectedDataFiles)
                if (peakList.hasRawDataFile(dataFile))
                    fileSelected = true;

            if (fileSelected) {
                Font font = component.getFont();
                font = font.deriveFont(Font.BOLD);
                component.setFont(font);
            }
        }

        return component;
    }

}
