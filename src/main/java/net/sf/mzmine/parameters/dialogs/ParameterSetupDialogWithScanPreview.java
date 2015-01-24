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

package net.sf.mzmine.parameters.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class extends ParameterSetupDialog class, including a SpectraPlot. This
 * is used to preview how the selected mass detector and his parameters works
 * over the raw data file.
 */
public abstract class ParameterSetupDialogWithScanPreview extends
	ParameterSetupDialog {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private RawDataFile[] dataFiles;
    private RawDataFile previewDataFile;

    // Dialog components
    private JPanel pnlPreviewFields;
    private JComboBox<RawDataFile> comboDataFileName;
    private JComboBox<Integer> comboScanNumber;
    private JCheckBox previewCheckBox;

    // XYPlot
    private SpectraPlot spectrumPlot;

    /**
     * @param parameters
     * @param massDetectorTypeNumber
     */
    public ParameterSetupDialogWithScanPreview(Window parent,
	    boolean valueCheckRequired, ParameterSet parameters) {
	super(parent, valueCheckRequired, parameters);
    }

    /**
     * This method must be overloaded by derived class to load all the preview
     * data sets into the spectrumPlot
     */
    protected abstract void loadPreview(SpectraPlot spectrumPlot,
	    Scan previewScan);

    private void updateTitle(Scan currentScan) {

	// Formats
	NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
	NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
	NumberFormat intensityFormat = MZmineCore.getConfiguration()
		.getIntensityFormat();

	// Set window and plot titles
	String title = "[" + previewDataFile.getName() + "] scan #"
		+ currentScan.getScanNumber();

	String subTitle = "MS" + currentScan.getMSLevel() + ", RT "
		+ rtFormat.format(currentScan.getRetentionTime());

	DataPoint basePeak = currentScan.getHighestDataPoint();
	if (basePeak != null) {
	    subTitle += ", base peak: " + mzFormat.format(basePeak.getMZ())
		    + " m/z ("
		    + intensityFormat.format(basePeak.getIntensity()) + ")";
	}
	spectrumPlot.setTitle(title, subTitle);

    }

    /**
     * @see net.sf.mzmine.util.dialogs.ParameterSetupDialog#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

	super.actionPerformed(event);

	Object src = event.getSource();
	String command = event.getActionCommand();

	if (src == comboDataFileName) {
	    int ind = comboDataFileName.getSelectedIndex();
	    if (ind >= 0) {
		previewDataFile = dataFiles[ind];
		int scanNumbers[] = previewDataFile.getScanNumbers();
		Integer scanNumbersObj[] = CollectionUtils
			.toIntegerArray(scanNumbers);
		ComboBoxModel<Integer> model = new DefaultComboBoxModel<Integer>(
			scanNumbersObj);
		comboScanNumber.setModel(model);
		comboScanNumber.setSelectedIndex(0);
		parametersChanged();
	    }
	}

	if (src == previewCheckBox) {
	    if (previewCheckBox.isSelected()) {
		// Set the height of the preview to 200 cells, so it will span
		// the whole vertical length of the dialog (buttons are at row
		// no 100). Also, we set the weight to 10, so the preview
		// component will consume most of the extra available space.
		mainPanel.add(spectrumPlot, 3, 0, 1, 200, 10, 10,
			GridBagConstraints.BOTH);
		pnlPreviewFields.setVisible(true);
		updateMinimumSize();
		pack();
		parametersChanged();
	    } else {
		mainPanel.remove(spectrumPlot);
		pnlPreviewFields.setVisible(false);
		updateMinimumSize();
		pack();
	    }
	}

	if (command.equals("PREVIOUS_SCAN")) {
	    int ind = comboScanNumber.getSelectedIndex() - 1;
	    if (ind >= 0)
		comboScanNumber.setSelectedIndex(ind);
	}

	if (command.equals("NEXT_SCAN")) {
	    int ind = comboScanNumber.getSelectedIndex() + 1;
	    if (ind < (comboScanNumber.getItemCount() - 1))
		comboScanNumber.setSelectedIndex(ind);
	}

    }

    protected void parametersChanged() {

	// Update preview as parameters have changed
	if ((comboScanNumber == null) || (!previewCheckBox.isSelected()))
	    return;

	Integer scanNumber = (Integer) comboScanNumber.getSelectedItem();
	if (scanNumber == null)
	    return;
	Scan currentScan = previewDataFile.getScan(scanNumber);

	updateParameterSetFromComponents();

	loadPreview(spectrumPlot, currentScan);

	updateTitle(currentScan);

    }

    /**
     * This function add all the additional components for this dialog over the
     * original ParameterSetupDialog.
     * 
     */
    @Override
    protected void addDialogComponents() {

	super.addDialogComponents();

	dataFiles = MZmineCore.getProjectManager().getCurrentProject()
		.getDataFiles();

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

	mainPanel.add(new JSeparator(), 0, getNumberOfParameters() + 1, 3, 1,
		0, 0, GridBagConstraints.HORIZONTAL);
	mainPanel.add(previewCheckBox, 0, getNumberOfParameters() + 2, 3, 1, 0,
		0, GridBagConstraints.HORIZONTAL);

	// Elements of pnlLab
	JPanel pnlLab = new JPanel();
	pnlLab.setLayout(new BoxLayout(pnlLab, BoxLayout.Y_AXIS));
	pnlLab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	pnlLab.add(new JLabel("Data file "));
	pnlLab.add(Box.createVerticalStrut(25));
	pnlLab.add(new JLabel("Scan number "));

	// Elements of pnlFlds
	JPanel pnlFlds = new JPanel();
	pnlFlds.setLayout(new BoxLayout(pnlFlds, BoxLayout.Y_AXIS));
	pnlFlds.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	comboDataFileName = new JComboBox<RawDataFile>(dataFiles);
	comboDataFileName.setSelectedItem(previewDataFile);
	comboDataFileName.addActionListener(this);

	int scanNumbers[] = previewDataFile.getScanNumbers();
	Integer scanNumbersObj[] = CollectionUtils.toIntegerArray(scanNumbers);

	comboScanNumber = new JComboBox<Integer>(scanNumbersObj);
	comboScanNumber.setSelectedIndex(0);
	comboScanNumber.addActionListener(this);

	pnlFlds.add(comboDataFileName);
	pnlFlds.add(Box.createVerticalStrut(10));

	// --> Elements of pnlScanArrows

	JPanel pnlScanArrows = new JPanel();
	pnlScanArrows.setLayout(new BoxLayout(pnlScanArrows, BoxLayout.X_AXIS));
	String leftArrow = new String(new char[] { '\u2190' });
	GUIUtils.addButton(pnlScanArrows, leftArrow, null, this,
		"PREVIOUS_SCAN");

	pnlScanArrows.add(Box.createHorizontalStrut(5));
	pnlScanArrows.add(comboScanNumber);
	pnlScanArrows.add(Box.createHorizontalStrut(5));

	String rightArrow = new String(new char[] { '\u2192' });
	GUIUtils.addButton(pnlScanArrows, rightArrow, null, this, "NEXT_SCAN");

	pnlFlds.add(pnlScanArrows);

	// Put all together
	pnlPreviewFields = new JPanel(new BorderLayout());

	pnlPreviewFields.add(pnlLab, BorderLayout.WEST);
	pnlPreviewFields.add(pnlFlds, BorderLayout.CENTER);
	pnlPreviewFields.setVisible(false);

	spectrumPlot = new SpectraPlot(this);
	spectrumPlot.setBorder(BorderFactory
		.createEtchedBorder(EtchedBorder.RAISED));
	spectrumPlot.setMinimumSize(new Dimension(400, 300));

	mainPanel.add(pnlPreviewFields, 0, getNumberOfParameters() + 3, 3, 1,
		0, 0);

	updateMinimumSize();
	pack();
    }

}