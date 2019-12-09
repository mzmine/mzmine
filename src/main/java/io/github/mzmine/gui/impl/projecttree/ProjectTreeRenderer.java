/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.impl.projecttree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.util.RawDataFileUtils;
import io.github.mzmine.util.swing.IconUtil;

class ProjectTreeRenderer extends DefaultTreeCellRenderer {

    private static final long serialVersionUID = 1L;

    private static final Icon projectIcon = IconUtil
            .loadIconFromResources("icons/projecticon.png");
    private static final Icon dataFileIcon = IconUtil
            .loadIconFromResources("icons/xicicon.png");
    private static final Icon spectrumIcon = IconUtil
            .loadIconFromResources("icons/spectrumicon.png");
    private static final Icon peakListsIcon = IconUtil
            .loadIconFromResources("icons/peaklistsicon.png");
    private static final Icon peakIcon = IconUtil
            .loadIconFromResources("icons/peakicon.png");
    private static final Icon peakListIcon = IconUtil
            .loadIconFromResources("icons/peaklisticon_single.png");
    private static final Icon alignedPeaklistIcon = IconUtil
            .loadIconFromResources("icons/peaklisticon_aligned.png");
    private static final Icon fileIcon = IconUtil
            .loadIconFromResources("icons/fileicon.png");
    private static final Icon fileWithMassListIcon = IconUtil
            .loadIconFromResources("icons/filewithmasslisticon.png");

    static final Font bigFont = new Font("SansSerif", Font.PLAIN, 12);
    static final Font smallerFont = new Font("SansSerif", Font.PLAIN, 11);
    static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

    ProjectTreeRenderer() {
        setOpenIcon(null);
        setClosedIcon(null);
        setLeafIcon(null);
    }

    public Component getTreeCellRendererComponent(JTree tree, Object node,
            boolean sel, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {

        JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, node,
                sel, expanded, leaf, row, hasFocus);

        DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
        Object embeddedObject = treeNode.getUserObject();

        if (embeddedObject instanceof MZmineProject) {
            label.setIcon(projectIcon);
            label.setFont(bigFont);
        }

        if (embeddedObject == RawDataTreeModel.dataFilesNodeName) {
            label.setIcon(dataFileIcon);
            label.setFont(bigFont);
        }

        if (embeddedObject == PeakListTreeModel.peakListsNodeName) {
            label.setFont(bigFont);
            label.setIcon(peakListsIcon);
        }

        if (embeddedObject instanceof RawDataFile) {
            label.setFont(smallerFont);

            boolean hasMassList = RawDataFileUtils
                    .hasMassLists((RawDataFile) embeddedObject);
            if (hasMassList)
                label.setIcon(fileWithMassListIcon);
            else
                label.setIcon(fileIcon);
        }

        if (embeddedObject instanceof Scan) {
            Scan s = (Scan) embeddedObject;
            label.setIcon(spectrumIcon);
            label.setFont(smallFont);

            // Change the color only if the row is not selected, otherwise we
            // could get blue text on blue background
            if (!sel) {
                if (s.getMSLevel() > 1)
                    label.setForeground(Color.red);
                else
                    label.setForeground(Color.blue);
            }
        }

        if (embeddedObject instanceof MassList) {
            label.setIcon(peakListIcon);
            label.setFont(smallFont);
        }

        if (embeddedObject instanceof PeakList) {
            PeakList p = (PeakList) embeddedObject;
            if (p.getNumberOfRawDataFiles() > 1) {
                label.setFont(smallerFont.deriveFont(Font.BOLD));
                label.setIcon(alignedPeaklistIcon);
            } else {
                label.setFont(smallerFont);
                label.setIcon(peakListIcon);
            }
        }

        if (embeddedObject instanceof PeakListRow) {
            PeakListRow r = (PeakListRow) embeddedObject;
            label.setIcon(peakIcon);
            label.setFont(smallFont);

            // Change the color only if the row is not selected
            if (!sel) {
                if (r.getPreferredPeakIdentity() != null) {
                    label.setForeground(Color.red);
                }
            }

        }

        return label;
    }

}
