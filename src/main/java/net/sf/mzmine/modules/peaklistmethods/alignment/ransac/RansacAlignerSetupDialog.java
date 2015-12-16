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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.alignment.ransac;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;

import com.google.common.collect.Range;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected mass detector and his parameters works
 * over the raw data file.
 */
public class RansacAlignerSetupDialog extends ParameterSetupDialog implements
	ActionListener {

    private static final long serialVersionUID = 1L;

    // Dialog components
    private JPanel pnlPlotXY, peakListsPanel;
    private JCheckBox previewCheckBox;
    private AlignmentRansacPlot chart;
    private JComboBox<PeakList> peakListsComboX, peakListsComboY;
    private JButton alignmentPreviewButton;

    public RansacAlignerSetupDialog(Window parent, boolean valueCheckRequired,
	    RansacAlignerParameters parameters) {
	super(parent, valueCheckRequired, parameters);
    }

    /**
     * @see net.sf.mzmine.util.dialogs.ParameterSetupDialog#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

	super.actionPerformed(event);
	Object src = event.getSource();

	if (src == previewCheckBox) {
	    if (previewCheckBox.isSelected()) {
		// Set the height of the preview to 200 cells, so it will span
		// the whole vertical length of the dialog (buttons are at row
		// no 100). Also, we set the weight to 10, so the preview
		// component will consume most of the extra available space.
		mainPanel.add(pnlPlotXY, 3, 0, 1, 200, 10, 10,
			GridBagConstraints.BOTH);
		peakListsPanel.setVisible(true);
		updateMinimumSize();
		pack();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
	    } else {
		mainPanel.remove(pnlPlotXY);
		peakListsPanel.setVisible(false);
		updateMinimumSize();
		pack();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());
	    }
	}

	if ((src == alignmentPreviewButton)) {
	    updatePreview();
	}

    }

    /**
     * This function add all the additional components for this dialog over the
     * original ParameterSetupDialog.
     * 
     */
    @Override
    protected void addDialogComponents() {

	super.addDialogComponents();

	PeakList peakLists[] = MZmineCore.getProjectManager()
		.getCurrentProject().getPeakLists();
	if (peakLists.length < 2)
	    return;

	PeakList selectedPeakLists[] = MZmineCore.getDesktop()
		.getSelectedPeakLists();

	// Preview check box
	previewCheckBox = new JCheckBox("Show preview of RANSAC alignment");
	previewCheckBox.addActionListener(this);
	previewCheckBox.setHorizontalAlignment(SwingConstants.CENTER);

	mainPanel.add(new JSeparator(), 0, getNumberOfParameters() + 1, 3, 1,
		0, 0, GridBagConstraints.HORIZONTAL);
	mainPanel.add(previewCheckBox, 0, getNumberOfParameters() + 2, 3, 1, 0,
		0, GridBagConstraints.HORIZONTAL);

	// Panel for the combo boxes with the peak lists
	JPanel comboPanel = new JPanel(new GridLayout(3, 1));

	peakListsComboX = new JComboBox<PeakList>(peakLists);
	peakListsComboX.addActionListener(this);
	peakListsComboY = new JComboBox<PeakList>(peakLists);
	peakListsComboY.addActionListener(this);
	comboPanel.add(peakListsComboX);
	comboPanel.add(peakListsComboY);

	alignmentPreviewButton = new JButton("Preview alignment");
	alignmentPreviewButton.addActionListener(this);
	comboPanel.add(alignmentPreviewButton);

	if (selectedPeakLists.length >= 2) {
	    peakListsComboX.setSelectedItem(selectedPeakLists[0]);
	    peakListsComboY.setSelectedItem(selectedPeakLists[1]);
	} else {
	    peakListsComboX.setSelectedItem(peakLists[0]);
	    peakListsComboY.setSelectedItem(peakLists[1]);
	}

	peakListsPanel = new JPanel();
	peakListsPanel.add(comboPanel);
	peakListsPanel.setVisible(false);

	// Panel for XYPlot
	pnlPlotXY = new JPanel(new BorderLayout());
	Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
	Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
	pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
	pnlPlotXY.setBackground(Color.white);

	chart = new AlignmentRansacPlot();
	pnlPlotXY.add(chart, BorderLayout.CENTER);

	mainPanel.add(peakListsPanel, 0, getNumberOfParameters() + 3, 3, 1, 0,
		0, GridBagConstraints.BOTH);

	updateMinimumSize();
	pack();
	setLocationRelativeTo(MZmineCore.getDesktop().getMainWindow());

    }

    /**
     * Create the vector which contains all the possible aligned peaks.
     * 
     * @return vector which contains all the possible aligned peaks.
     */
    private Vector<AlignStructMol> getVectorAlignment(PeakList peakListX,
	    PeakList peakListY, RawDataFile file, RawDataFile file2) {

	Vector<AlignStructMol> alignMol = new Vector<AlignStructMol>();

	for (PeakListRow row : peakListX.getRows()) {

	    // Calculate limits for a row with which the row can be aligned
	    MZTolerance mzTolerance = super.parameterSet.getParameter(
		    RansacAlignerParameters.MZTolerance).getValue();
	    RTTolerance rtTolerance = super.parameterSet.getParameter(
		    RansacAlignerParameters.RTToleranceBefore).getValue();
	    Range<Double> mzRange = mzTolerance.getToleranceRange(row
		    .getAverageMZ());
	    Range<Double> rtRange = rtTolerance.getToleranceRange(row
		    .getAverageRT());

	    // Get all rows of the aligned peaklist within parameter limits
	    PeakListRow candidateRows[] = peakListY
		    .getRowsInsideScanAndMZRange(rtRange, mzRange);

	    for (PeakListRow candidateRow : candidateRows) {
		if (file == null || file2 == null) {
		    alignMol.addElement(new AlignStructMol(row, candidateRow));
		} else {
		    if (candidateRow.getPeak(file2) != null) {
			alignMol.addElement(new AlignStructMol(row,
				candidateRow, file, file2));
		    }
		}
	    }
	}
	return alignMol;
    }

    private void updatePreview() {

	PeakList peakListX = (PeakList) peakListsComboX.getSelectedItem();
	PeakList peakListY = (PeakList) peakListsComboY.getSelectedItem();

	if ((peakListX == null) || (peakListY == null))
	    return;

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

	// Update the parameter set from dialog components
	updateParameterSetFromComponents();

	// Check the parameter values
	ArrayList<String> errorMessages = new ArrayList<String>();
	boolean parametersOK = super.parameterSet
		.checkParameterValues(errorMessages);
	if (!parametersOK) {
	    StringBuilder message = new StringBuilder(
		    "Please check the parameter settings:\n\n");
	    for (String m : errorMessages) {
		message.append(m);
		message.append("\n");
	    }
	    MZmineCore.getDesktop().displayMessage(this, message.toString());
	    return;
	}

	// Ransac Alignment
	Vector<AlignStructMol> list = this.getVectorAlignment(peakListX,
		peakListY, file, file2);
	RANSAC ransac = new RANSAC(super.parameterSet);
	ransac.alignment(list);

	// Plot the result
	this.chart.removeSeries();
	this.chart.addSeries(list,
		peakListX.getName() + " vs " + peakListY.getName(),
		super.parameterSet.getParameter(RansacAlignerParameters.Linear)
			.getValue());
	this.chart.printAlignmentChart(peakListX.getName() + " RT",
		peakListY.getName() + " RT");

    }

}
