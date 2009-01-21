/*
 * Copyright 2006-2009 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.spectra;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.ProjectListener;
import net.sf.mzmine.util.GUIUtils;

/**
 * Spectra visualizer's bottom panel
 */
class SpectraBottomPanel extends JPanel implements ProjectListener,
		InternalFrameListener {

	// get arrow characters by their UTF16 code
	public static final String leftArrow = new String(new char[] { '\u2190' });
	public static final String rightArrow = new String(new char[] { '\u2192' });

	public static final Font smallFont = new Font("SansSerif", Font.PLAIN, 10);

	private JPanel topPanel, bottomPanel;
	private JComboBox msmsSelector, peakListSelector;

	private SpectraVisualizerWindow masterFrame;
	private RawDataFile dataFile;
	private MZmineProject project;
	private boolean isotopeFlag;

	SpectraBottomPanel(SpectraVisualizerWindow masterFrame,
			RawDataFile dataFile, SpectraVisualizerType type) {

		super(new BorderLayout());
		this.dataFile = dataFile;
		this.masterFrame = masterFrame;
		isotopeFlag = type == SpectraVisualizerType.ISOTOPE;

		setBackground(Color.white);

		topPanel = new JPanel();
		topPanel.setBackground(Color.white);
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		add(topPanel, BorderLayout.CENTER);

		topPanel.add(Box.createHorizontalStrut(10));

		JButton prevScanBtn = GUIUtils.addButton(topPanel, leftArrow, null,
				masterFrame, "PREVIOUS_SCAN");
		prevScanBtn.setBackground(Color.white);
		prevScanBtn.setFont(smallFont);

		topPanel.add(Box.createHorizontalGlue());

		if (!isotopeFlag) {

			GUIUtils.addLabel(topPanel, "Peak list: ", SwingConstants.RIGHT);

			peakListSelector = new JComboBox();
			peakListSelector.setBackground(Color.white);
			peakListSelector.setFont(smallFont);
			peakListSelector.addActionListener(masterFrame);
			peakListSelector.setActionCommand("PEAKLIST_CHANGE");
			topPanel.add(peakListSelector);

			topPanel.add(Box.createHorizontalGlue());

		}

		JButton nextScanBtn = GUIUtils.addButton(topPanel, rightArrow, null,
				masterFrame, "NEXT_SCAN");
		nextScanBtn.setBackground(Color.white);
		nextScanBtn.setFont(smallFont);

		if (!isotopeFlag) {

			topPanel.add(Box.createHorizontalStrut(10));

			bottomPanel = new JPanel();
			bottomPanel.setBackground(Color.white);
			bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
			add(bottomPanel, BorderLayout.SOUTH);

			bottomPanel.add(Box.createHorizontalGlue());

			GUIUtils.addLabel(bottomPanel, "MS/MS: ", SwingConstants.RIGHT);

			msmsSelector = new JComboBox();
			msmsSelector.setBackground(Color.white);
			msmsSelector.setFont(smallFont);
			bottomPanel.add(msmsSelector);

			JButton showButton = GUIUtils.addButton(bottomPanel, "Show", null,
					masterFrame, "SHOW_MSMS");
			showButton.setBackground(Color.white);
			showButton.setFont(smallFont);

			bottomPanel.add(Box.createHorizontalGlue());

		}

		project = MZmineCore.getCurrentProject();
		project.addProjectListener(this);

		masterFrame.addInternalFrameListener(this);

	}

	JComboBox getMSMSSelector() {
		return msmsSelector;
	}

	void setMSMSSelectorVisible(boolean visible) {
		bottomPanel.setVisible(visible);
	}

	/**
	 * Returns selected peak list
	 */
	PeakList getSelectedPeakList() {
		PeakList selectedPeakList = (PeakList) peakListSelector
				.getSelectedItem();
		return selectedPeakList;
	}

	/**
	 * Reloads peak lists from the project to the selector combo box
	 */
	void rebuildPeakListSelector(MZmineProject project) {
		PeakList selectedPeakList = (PeakList) peakListSelector
				.getSelectedItem();
		PeakList currentPeakLists[] = project.getPeakLists(dataFile);
		peakListSelector.removeAllItems();
		for (int i = currentPeakLists.length - 1; i >= 0; i--) {
			peakListSelector.addItem(currentPeakLists[i]);
		}
		if (selectedPeakList != null)
			peakListSelector.setSelectedItem(selectedPeakList);
	}

	public void projectModified(ProjectEvent event, MZmineProject project) {
		if ((event == ProjectEvent.PEAKLIST_CHANGE) && (!isotopeFlag))
			rebuildPeakListSelector(project);
	}

	public void internalFrameActivated(InternalFrameEvent event) {
		// Ignore
	}

	/**
	 * We have to remove the listener when the window is closed, because
	 * otherwise the project would always keep a reference to this window and
	 * the GC would not be able to collect it
	 */
	public void internalFrameClosed(InternalFrameEvent event) {
		project.removeProjectListener(this);
		masterFrame.removeInternalFrameListener(this);
	}

	public void internalFrameClosing(InternalFrameEvent event) {
		// Ignore
	}

	public void internalFrameDeactivated(InternalFrameEvent event) {
		// Ignore
	}

	public void internalFrameDeiconified(InternalFrameEvent event) {
		// Ignore
	}

	public void internalFrameIconified(InternalFrameEvent event) {
		// Ignore
	}

	public void internalFrameOpened(InternalFrameEvent event) {
		// Ignore
	}
}
