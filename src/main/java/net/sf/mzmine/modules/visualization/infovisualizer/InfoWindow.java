/*
 * Copyright 2006-2012 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.visualization.infovisualizer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;

class InfoWindow extends JInternalFrame {

	private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
	private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

	Range rtRange, mzRange;
	int numOfRows, numOfIdentities;

	InfoWindow(PeakList peakList) {

		super("Peak list information", true, true, true, true);

		// this.setTitle(peakList.getName() + " information");

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		// setBackground(Color.white);

		this.getInfoRange(peakList);

		if (peakList.getNumberOfRows() == 0) {
			mzRange = new Range(0, 0);
			rtRange = new Range(0, 0);
		}
		
		// Raw data file list
		JList rawDataFileList = new JList(peakList.getRawDataFiles());
		rawDataFileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		rawDataFileList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane rawlistScroller = new JScrollPane(rawDataFileList);
		rawlistScroller.setPreferredSize(new Dimension(250, 60));
		rawlistScroller.setAlignmentX(LEFT_ALIGNMENT);
		JPanel rawPanel = new JPanel();
		rawPanel.setLayout(new BoxLayout(rawPanel, BoxLayout.Y_AXIS));
		JLabel label = new JLabel("List of raw data files");
		// label.setLabelFor(rawDataFileList);
		rawPanel.add(label);
		rawPanel.add(rawlistScroller);
		rawPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		// Applied methods list
		AppliedMethodList appliedMethodList = new AppliedMethodList(peakList.getAppliedMethods());
		appliedMethodList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		appliedMethodList.setLayoutOrientation(JList.VERTICAL);
		JScrollPane methodlistScroller = new JScrollPane(appliedMethodList);
		methodlistScroller.setPreferredSize(new Dimension(250, 80));
		methodlistScroller.setAlignmentX(LEFT_ALIGNMENT);

		JPanel methodPanel = new JPanel();
		methodPanel.setLayout(new BoxLayout(methodPanel, BoxLayout.Y_AXIS));
		// JLabel label = new JLabel("List of applied methods");
		// label.setLabelFor(processInfoList);
		methodPanel.add(new JLabel("List of applied methods"));
		methodPanel.add(methodlistScroller);
		methodPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		// Panels
		JPanel pnlGrid = new JPanel();
		pnlGrid.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		c.gridwidth = 1;

		c.gridx = 0;
		c.gridy = 0;
		pnlGrid.add(new JLabel("<html>Name: <font color=\"blue\">"
				+ peakList.getName() + "</font></html>"), c);
		c.gridx = 0;
		c.gridy = 1;
		pnlGrid.add(new JLabel(
				"<html>Created (yyyy/MM/dd HH:mm:ss): <font color=\"blue\">"
						+ ((SimplePeakList) peakList).getDateCreated()
						+ "</font></html>"), c);
		c.gridx = 0;
		c.gridy = 2;
		pnlGrid.add(rawPanel, c);
		c.gridx = 0;
		c.gridy = 3;
		pnlGrid.add(new JLabel("<html>Number of rows: <font color=\"blue\">"
				+ numOfRows + "</font></html>"), c);
		c.gridx = 0;
		c.gridy = 4;
		String text = mzFormat.format(
				mzRange.getMin()) + " - "
				+ mzFormat.format(
						mzRange.getMax());
		pnlGrid.add(new JLabel("<html>m/z range: <font color=\"blue\">" + text
				+ "</font></html>"), c);
		c.gridx = 0;
		c.gridy = 5;
		text = rtFormat.format(rtRange.getMin()) + " - "
				+ rtFormat.format(rtRange.getMax());
		pnlGrid.add(new JLabel("<html>RT range: <font color=\"blue\">" + text
				+ "</font> min</html>"), c);
		c.gridx = 0;
		c.gridy = 6;
		pnlGrid.add(new JLabel(
				"<html>Number of identified peaks: <font color=\"blue\">"
						+ numOfIdentities + "</font></html>"), c);
		c.gridx = 0;
		c.gridy = 7;
		pnlGrid.add(methodPanel, c);

		add(pnlGrid);
		setResizable(false);

		pack();

	}

	void getInfoRange(PeakList peakList) {
		PeakListRow[] rows = peakList.getRows();
		numOfRows = rows.length;

		mzRange = peakList.getRowsMZRange();
		rtRange = peakList.getRowsRTRange();
		for (PeakListRow row : rows) {
			if (row.getPreferredPeakIdentity() != null)
				numOfIdentities++;
		}

	}
}
