/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.desktop.impl.projecttree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.project.MZmineProject;

class ProjectTreeRenderer extends DefaultTreeCellRenderer {

	static final Icon projectIcon = new ImageIcon("icons/projecticon.png");
	static final Icon dataFileIcon = new ImageIcon("icons/xicicon.png");
	static final Icon spectrumIcon = new ImageIcon("icons/spectrumicon.png");
	static final Icon peakListIcon = new ImageIcon("icons/peaklisticon.png");
	static final Icon peakIcon = new ImageIcon("icons/peakicon.png");

	static final Font bigFont = new Font("SansSerif", Font.PLAIN, 12);
	static final Font smallerFont = new Font("SansSerif", Font.PLAIN, 11);
	static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

	ProjectTreeRenderer() {
		setOpenIcon(null);
		setClosedIcon(null);
		setLeafIcon(null);
	}

	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

		JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value,
				sel, expanded, leaf, row, hasFocus);

		if (value instanceof MZmineProject) {
			label.setIcon(projectIcon);
			label.setFont(bigFont);
		}

		if (value == ProjectTreeModel.dataFilesItem) {
			label.setIcon(dataFileIcon);
			label.setFont(bigFont);
		}

		if (value == ProjectTreeModel.peakListsItem) {
			label.setFont(bigFont);
			label.setIcon(peakListIcon);
		}

		if (value instanceof RawDataFile) {
			label.setFont(smallerFont);
		}

		if (value instanceof Scan) {
			Scan s = (Scan) value;
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

		if (value instanceof PeakList) {
			PeakList p = (PeakList) value;
			label.setIcon(null);
			if (p.getNumberOfRawDataFiles() > 1) {
				label.setFont(smallerFont.deriveFont(Font.BOLD));
			} else {
				label.setFont(smallerFont);
			}
		}

		if (value instanceof PeakListRow) {
			PeakListRow r = (PeakListRow) value;
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
