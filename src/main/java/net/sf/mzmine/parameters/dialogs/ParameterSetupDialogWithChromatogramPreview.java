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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.parameters.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.RangeComponent;
import net.sf.mzmine.util.Range;

/**
 * This class extends ParameterSetupDialog class, including a TICPlot. This is
 * used to preview how the selected raw data filters work.
 */
public abstract class ParameterSetupDialogWithChromatogramPreview extends
	ParameterSetupDialog {

    private RawDataFile[] dataFiles;
    private RawDataFile previewDataFile;

    // Dialog components
    private JPanel pnlPreviewFields;
    private JComboBox comboDataFileName;
    private RangeComponent rtRangeBox, mzRangeBox;
    private JCheckBox previewCheckBox;

    // XYPlot
    private TICPlot ticPlot;

    public ParameterSetupDialogWithChromatogramPreview(ParameterSet parameters) {
	super(parameters);
    }

    /**
     * Get the parameters related to the plot and call the function
     * addRawDataFile() to add the data file to the plot
     * 
     * @param dataFile
     */
    protected abstract void loadPreview(TICPlot ticPlot, RawDataFile dataFile,
	    Range rtRange, Range mzRange);

    private void updateTitle() {

	NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
	NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

	Range rtRange = rtRangeBox.getValue();
	Range mzRange = mzRangeBox.getValue();

	String title = "m/z: " + mzFormat.format(mzRange.getMin()) + " - "
		+ mzFormat.format(mzRange.getMax()) + ", RT: "
		+ rtFormat.format(rtRange.getMin()) + " - "
		+ rtFormat.format(rtRange.getMax());

	// update plot title
	ticPlot.setTitle(previewDataFile.getName(), title);
    }

    public void actionPerformed(ActionEvent event) {

	super.actionPerformed(event);

	Object src = event.getSource();

	if (src == comboDataFileName) {
	    int ind = comboDataFileName.getSelectedIndex();
	    if (ind >= 0) {
		previewDataFile = dataFiles[ind];
		parametersChanged();
	    }
	}

	if (src == previewCheckBox) {
	    if (previewCheckBox.isSelected()) {
		// Set the height of the preview to 200 cells, so it will span
		// the whole vertical length of the dialog (buttons are at row
		// no 100). Also, we set the weight to 10, so the preview
		// component will consume most of the extra available space.
		mainPanel.add(ticPlot, 3, 0, 1, 200, 10, 10,
			GridBagConstraints.BOTH);
		pnlPreviewFields.setVisible(true);
		updateMinimumSize();
		pack();
		parametersChanged();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
	    } else {
		mainPanel.remove(ticPlot);
		pnlPreviewFields.setVisible(false);
		updateMinimumSize();
		pack();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
	    }
	}

    }

    protected void parametersChanged() {

	// Update preview as parameters have changed
	if ((previewCheckBox == null) || (!previewCheckBox.isSelected()))
	    return;

	Range rtRange = rtRangeBox.getValue();
	Range mzRange = mzRangeBox.getValue();
	updateParameterSetFromComponents();

	loadPreview(ticPlot, previewDataFile, rtRange, mzRange);

	updateTitle();

    }

    /**
     * This function add all the additional components for this dialog over the
     * original ParameterSetupDialog.
     * 
     */
    @Override
    protected void addDialogComponents() {

	super.addDialogComponents();

	dataFiles = MZmineCore.getCurrentProject().getDataFiles();

	if (dataFiles.length == 0)
	    return;

	RawDataFile selectedFiles[] = MZmineCore.getDesktop()
		.getSelectedDataFiles();

	if (selectedFiles.length > 0)
	    previewDataFile = selectedFiles[0];
	else
	    previewDataFile = dataFiles[0];

	previewCheckBox = new JCheckBox("Show preview");
	previewCheckBox.addActionListener(this);
	previewCheckBox.setHorizontalAlignment(SwingConstants.CENTER);

	mainPanel.add(new JSeparator(), 0, getNumberOfParameters() + 1, 3, 1);
	mainPanel.add(previewCheckBox, 0, getNumberOfParameters() + 2, 3, 1);

	// Elements of pnlLab
	JPanel pnlLab = new JPanel();
	pnlLab.setLayout(new BoxLayout(pnlLab, BoxLayout.Y_AXIS));
	pnlLab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	pnlLab.add(new JLabel("Data file "));
	pnlLab.add(Box.createVerticalStrut(30));
	pnlLab.add(new JLabel("RT range "));
	pnlLab.add(Box.createVerticalStrut(25));
	pnlLab.add(new JLabel("m/z range "));

	// Elements of pnlFlds
	JPanel pnlFlds = new JPanel();
	pnlFlds.setLayout(new BoxLayout(pnlFlds, BoxLayout.Y_AXIS));
	pnlFlds.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	comboDataFileName = new JComboBox(dataFiles);
	comboDataFileName.setSelectedItem(previewDataFile);
	comboDataFileName.addActionListener(this);

	rtRangeBox = new RangeComponent(MZmineCore.getConfiguration()
		.getRTFormat());
	rtRangeBox.setValue(previewDataFile.getDataRTRange(1));

	mzRangeBox = new RangeComponent(MZmineCore.getConfiguration()
		.getMZFormat());
	mzRangeBox.setValue(previewDataFile.getDataMZRange(1));

	pnlFlds.add(comboDataFileName);
	pnlFlds.add(Box.createVerticalStrut(10));
	pnlFlds.add(rtRangeBox);
	pnlFlds.add(mzRangeBox);

	// Put all together
	pnlPreviewFields = new JPanel(new BorderLayout());

	pnlPreviewFields.add(pnlLab, BorderLayout.WEST);
	pnlPreviewFields.add(pnlFlds, BorderLayout.CENTER);
	pnlPreviewFields.setVisible(false);

	ticPlot = new TICPlot(this);
	ticPlot.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
	ticPlot.setMinimumSize(new Dimension(400, 300));

	mainPanel.add(pnlPreviewFields, 0, getNumberOfParameters() + 3, 3, 1,
		0, 0);

	updateMinimumSize();
	pack();

	setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

}
