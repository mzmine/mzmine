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
package net.sf.mzmine.modules.alignment.ransac;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected mass detector and his parameters works
 * over the raw data file.
 */
public class RansacAlignerSetupDialog extends ParameterSetupDialog implements
		ActionListener{

	private Logger logger = Logger.getLogger(this.getClass().getName());
	// Dialog components
	private JPanel pnlPlotXY,  peakListsPanel;
	private JCheckBox preview;
	private AlignmentRansacPlot chart;
	private JComboBox peakListsComboX, peakListsComboY;
	private JButton alignmentPreviewButton;
	private RansacAlignerParameters parameters;

	/**
	 * @param parameters
	 * @param massDetectorTypeNumber
	 */
	public RansacAlignerSetupDialog(String title, RansacAlignerParameters parameters, String helpID) {

		super(title, parameters, helpID);
		this.parameters = parameters;
		addComponents();
	}

	/**
	 * @see net.sf.mzmine.util.dialogs.ParameterSetupDialog#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {

		super.actionPerformed(event);
		Object src = event.getSource();

		if (src == preview) {
			if (preview.isSelected()) {
				mainPanel.add(pnlPlotXY, BorderLayout.EAST);
				peakListsPanel.setVisible(true);
				pack();
				this.setResizable(true);
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
			} else {
				mainPanel.remove(pnlPlotXY);
				peakListsPanel.setVisible(false);
				this.setResizable(false);
				pack();
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
			}
		}

		if (src == alignmentPreviewButton) {
			PeakList peakListX = (PeakList) peakListsComboX.getSelectedItem();
			PeakList peakListY = (PeakList) peakListsComboY.getSelectedItem();

			// Select the rawDataFile which has more peaks in each peakList
			int numPeaks = 0;
			RawDataFile file = null;
			RawDataFile file2 = null;

			for (RawDataFile rfile : peakListX.getRawDataFiles()) {
				if (peakListX.getPeaks(rfile).length > numPeaks) {
					numPeaks = peakListX.getPeaks(rfile).length;
					file = rfile;
				}
			}
			numPeaks = 0;
			for (RawDataFile rfile : peakListY.getRawDataFiles()) {
				if (peakListY.getPeaks(rfile).length > numPeaks) {
					numPeaks = peakListY.getPeaks(rfile).length;
					file2 = rfile;
				}
			}

			// Ransac Alignment
			Vector<AlignStructMol> list = this.getVectorAlignment(peakListX, peakListY, file, file2);
			super.updateParameterSetFromComponents();
			RANSAC ransac = new RANSAC(parameters);
			ransac.alignment(list);

			// Plot the result
			this.chart.removeSeries();			
			this.chart.addSeries(list, peakListX.getName() + " vs " + peakListY.getName());
			this.chart.printAlignmentChart(peakListX.getName() + " RT", peakListY.getName() + " RT");
		}

	}
	

	/**
	 * This function add all the additional components for this dialog over the
	 * original ParameterSetupDialog.
	 * 
	 */
	private void addComponents() {

		// Elements of pnlpreview
		JPanel pnlpreview = new JPanel(new BorderLayout());
		preview = new JCheckBox(" Show preview of mass peak detection ");
		preview.addActionListener(this);
		preview.setHorizontalAlignment(SwingConstants.CENTER);
		pnlpreview.add(new JSeparator(), BorderLayout.NORTH);
		pnlpreview.add(preview, BorderLayout.CENTER);
		pnlpreview.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

		pnlpreview.add(new JSeparator(), BorderLayout.NORTH);
		pnlpreview.add(preview, BorderLayout.CENTER);

		// Panel for the combo boxes with the peak lists
		peakListsPanel = new JPanel();
		peakListsPanel.setLayout(new BoxLayout(peakListsPanel, BoxLayout.PAGE_AXIS));

		PeakList[] peakLists = MZmineCore.getDesktop().getSelectedPeakLists();

		peakListsComboX = new JComboBox();
		peakListsComboY = new JComboBox();
		for (PeakList peakList : peakLists) {
			peakListsComboX.addItem(peakList);
			peakListsComboY.addItem(peakList);
		}		

		alignmentPreviewButton = new JButton("Preview Alignmnet");
		alignmentPreviewButton.addActionListener(this);
		peakListsPanel.add(peakListsComboX);
		peakListsPanel.add(peakListsComboY);
		peakListsPanel.add(alignmentPreviewButton);
		peakListsPanel.setVisible(false);

		JPanel pnlVisible = new JPanel(new BorderLayout());
		pnlVisible.add(pnlpreview, BorderLayout.NORTH);
		pnlVisible.add(peakListsPanel, BorderLayout.CENTER);

		// Panel for XYPlot
		pnlPlotXY = new JPanel(new BorderLayout());
		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
		pnlPlotXY.setBackground(Color.white);

		chart = new AlignmentRansacPlot();
		chart.setVisible(true);
		pnlPlotXY.add(chart);

		componentsPanel.add(pnlVisible, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}


	/**
	 * Create the vector which contains all the possible aligned peaks.	 
	 * @return vector which contains all the possible aligned peaks.
	 */
	private Vector<AlignStructMol> getVectorAlignment(PeakList peakListX, PeakList peakListY, RawDataFile file, RawDataFile file2) {

		Vector<AlignStructMol> alignMol = new Vector<AlignStructMol>();

		for (PeakListRow row : peakListX.getRows()) {

			// Calculate limits for a row with which the row can be aligned
			double mzTolerance = (Double) parameters.getParameterValue(RansacAlignerParameters.MZTolerance);
			double rtToleranceValueAbs = (Double) parameters.getParameterValue(RansacAlignerParameters.RTTolerance);
			double mzMin = row.getAverageMZ() - mzTolerance;
			double mzMax = row.getAverageMZ() + mzTolerance;
			double rtMin, rtMax;
			double rtToleranceValue = rtToleranceValueAbs;
			rtMin = row.getAverageRT() - rtToleranceValue;
			rtMax = row.getAverageRT() + rtToleranceValue;

			// Get all rows of the aligned peaklist within parameter limits
			PeakListRow candidateRows[] = peakListY.getRowsInsideScanAndMZRange(
					new Range(rtMin, rtMax), new Range(mzMin, mzMax));

			for (PeakListRow candidateRow : candidateRows) {
				if (file == null || file2 == null) {
					alignMol.addElement(new AlignStructMol(row, candidateRow));
				} else {
					if (candidateRow.getPeak(file2) != null) {
						alignMol.addElement(new AlignStructMol(row, candidateRow, file, file2));
					}
				}
			}
		}
		return alignMol;
	}
}