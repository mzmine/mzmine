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
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.tic.TICPlotType;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.ranges.DoubleRangeComponent;

import com.google.common.collect.Range;

/**
 * This class extends ParameterSetupDialog class, including a TICPlot. This is
 * used to preview how the selected raw data filters work.
 * 
 * Slightly modified to add the possibility of switching to TIC (versus Base
 * Peak) preview.
 */
public abstract class ParameterSetupDialogWithChromatogramPreview extends
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
    private DoubleRangeComponent rtRangeBox, mzRangeBox;
    private JCheckBox previewCheckBox;

    // Show as TIC
    private JComboBox<TICPlotType> ticViewComboBox;

    // XYPlot
    private TICPlot ticPlot;

    public ParameterSetupDialogWithChromatogramPreview(Window parent,
	    boolean valueCheckRequired, ParameterSet parameters) {
	super(parent, valueCheckRequired, parameters);
    }

    /**
     * Get the parameters related to the plot and call the function
     * addRawDataFile() to add the data file to the plot
     * 
     * @param dataFile
     */
    protected abstract void loadPreview(TICPlot ticPlot, RawDataFile dataFile,
	    Range<Double> rtRange, Range<Double> mzRange);

    private void updateTitle() {

	NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
	NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

	Range<Double> rtRange = rtRangeBox.getValue();
	Range<Double> mzRange = mzRangeBox.getValue();

	String title = "m/z: " + mzFormat.format(mzRange.lowerEndpoint())
		+ " - " + mzFormat.format(mzRange.upperEndpoint()) + ", RT: "
		+ rtFormat.format(rtRange.lowerEndpoint()) + " - "
		+ rtFormat.format(rtRange.upperEndpoint());

	// update plot title
	ticPlot.setTitle(previewDataFile.getName(), title);
    }

    public void actionPerformed(ActionEvent event) {

	Object src = event.getSource();

	// Avoid calling twice "parametersChanged()" for the widgets specific to
	// this inherited dialog class
	if (src != comboDataFileName && src != previewCheckBox
		&& src != ticViewComboBox) {
	    super.actionPerformed(event);
	}

	// Specific widgets

	if (src == comboDataFileName) {
	    int ind = comboDataFileName.getSelectedIndex();
	    if (ind >= 0) {
		previewDataFile = dataFiles[ind];
		parametersChanged();
	    }
	}

	if (src == previewCheckBox) {
	    if (previewCheckBox.isSelected()) {
		showPreview();
	    } else {
		hidePreview();
	    }
	}

	if (src == ticViewComboBox) {
	    parametersChanged();
	}

    }

    public void showPreview() {
	// Set the height of the preview to 200 cells, so it will span
	// the whole vertical length of the dialog (buttons are at row
	// no 100). Also, we set the weight to 10, so the preview
	// component will consume most of the extra available space.
	mainPanel.add(ticPlot, 3, 0, 1, 200, 10, 10, GridBagConstraints.BOTH);
	pnlPreviewFields.setVisible(true);
	updateMinimumSize();
	pack();
	parametersChanged();
	// previewCheckBox.setSelected(true);
    }

    public void hidePreview() {
	mainPanel.remove(ticPlot);
	pnlPreviewFields.setVisible(false);
	updateMinimumSize();
	pack();
	previewCheckBox.setSelected(false);
    }

    public TICPlotType getPlotType() {
	return (TICPlotType) (ticViewComboBox.getSelectedItem());
    }

    public void setPlotType(TICPlotType plotType) {
	ticViewComboBox.setSelectedItem(plotType);
    }

    public RawDataFile getPreviewDataFile() {
	return this.previewDataFile;
    }

    protected void parametersChanged() {

	// Update preview as parameters have changed
	if ((previewCheckBox == null) || (!previewCheckBox.isSelected()))
	    return;

	Range<Double> rtRange = rtRangeBox.getValue();
	Range<Double> mzRange = mzRangeBox.getValue();
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

	mainPanel.add(new JSeparator(), 0, getNumberOfParameters() + 1, 3, 1);
	mainPanel.add(previewCheckBox, 0, getNumberOfParameters() + 2, 3, 1);

	// Elements of pnlLab
	JPanel pnlLab = new JPanel();
	pnlLab.setLayout(new BoxLayout(pnlLab, BoxLayout.Y_AXIS));
	pnlLab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	pnlLab.add(Box.createVerticalStrut(5));
	pnlLab.add(new JLabel("Data file "));
	pnlLab.add(Box.createVerticalStrut(20));
	pnlLab.add(new JLabel("Plot Type "));
	pnlLab.add(Box.createVerticalStrut(25));
	pnlLab.add(new JLabel("RT range "));
	pnlLab.add(Box.createVerticalStrut(15));
	pnlLab.add(new JLabel("m/z range "));

	// Elements of pnlFlds
	JPanel pnlFlds = new JPanel();
	pnlFlds.setLayout(new BoxLayout(pnlFlds, BoxLayout.Y_AXIS));
	pnlFlds.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	comboDataFileName = new JComboBox<RawDataFile>(dataFiles);
	comboDataFileName.setSelectedItem(previewDataFile);
	comboDataFileName.addActionListener(this);

	ticViewComboBox = new JComboBox<TICPlotType>(TICPlotType.values());
	ticViewComboBox.setSelectedItem(TICPlotType.TIC);
	ticViewComboBox.addActionListener(this);

	rtRangeBox = new DoubleRangeComponent(MZmineCore.getConfiguration()
		.getRTFormat());
	rtRangeBox.setValue(previewDataFile.getDataRTRange(1));

	mzRangeBox = new DoubleRangeComponent(MZmineCore.getConfiguration()
		.getMZFormat());
	mzRangeBox.setValue(previewDataFile.getDataMZRange(1));

	pnlFlds.add(comboDataFileName);
	pnlFlds.add(Box.createVerticalStrut(10));
	pnlFlds.add(ticViewComboBox);
	pnlFlds.add(Box.createVerticalStrut(20));
	pnlFlds.add(rtRangeBox);
	pnlFlds.add(Box.createVerticalStrut(5));
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
    }

}
