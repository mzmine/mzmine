/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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

package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.PlotMode;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.modules.visualization.spectra.datasets.ScanDataSet;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.GUIUtils;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected mass detector and his parameters works
 * over the raw data file.
 */
public class ParameterSetupDialogWithScanPreview extends ParameterSetupDialog
		implements ActionListener, PropertyChangeListener {

	private RawDataFile[] dataFiles;
	private RawDataFile previewDataFile;

	// Dialog components
	private JPanel pnlFileNameScanNumber;
	private JComboBox comboDataFileName, comboScanNumber;
	private JCheckBox previewCheckBox;
	private JButton nextScanBtn, prevScanBtn;

	// XYPlot
	private SpectraPlot spectrumPlot;

	/**
	 * @param parameters
	 * @param massDetectorTypeNumber
	 */
	public ParameterSetupDialogWithScanPreview(String name,
			SimpleParameterSet parameters, String helpFile) {

		super(name, parameters, helpFile);

		dataFiles = MZmineCore.getCurrentProject().getDataFiles();

		if (dataFiles.length != 0) {

			RawDataFile selectedFiles[] = MZmineCore.getDesktop()
					.getSelectedDataFiles();

			if (selectedFiles.length > 0)
				previewDataFile = selectedFiles[0];
			else
				previewDataFile = dataFiles[0];

			// Set a listener in all parameters's fields to add functionality to
			// this dialog
			for (Parameter p : parameters.getParameters()) {

				JComponent field = getComponentForParameter(p);
				field.addPropertyChangeListener("value", this);
				if (field instanceof JCheckBox)
					((JCheckBox) field).addActionListener(this);
				if (field instanceof JComboBox)
					((JComboBox) field).addActionListener(this);
			}

		}

		addComponents();

	}

	private void reloadPreview() {
		Integer scanNumber = (Integer) comboScanNumber.getSelectedItem();
		if (scanNumber == null)
			return;
		Scan currentScan = previewDataFile.getScan(scanNumber);
		updateParameterSetFromComponents();
		loadPreview(spectrumPlot, currentScan);
		updateTitle(currentScan);
	}

	/**
	 * This method may be overloaded by derived class to load all the preview
	 * data sets into the spectrumPlot
	 */
	protected void loadPreview(SpectraPlot spectrumPlot, Scan previewScan) {

		ScanDataSet spectraDataSet = new ScanDataSet(previewScan);

		spectrumPlot.removeAllDataSets();
		spectrumPlot.addDataSet(spectraDataSet,
				SpectraVisualizerWindow.scanColor, false);

		// Set plot mode only if it hasn't been set before
		// if the scan is centroided, switch to centroid mode
		if (previewScan.isCentroided()) {
			spectrumPlot.setPlotMode(PlotMode.CENTROID);
		} else {
			spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
		}

	}

	private void updateTitle(Scan currentScan) {

		// Formats
		NumberFormat rtFormat = MZmineCore.getRTFormat();
		NumberFormat mzFormat = MZmineCore.getMZFormat();
		NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

		// Set window and plot titles
		String title = "[" + previewDataFile.toString() + "] scan #"
				+ currentScan.getScanNumber();

		String subTitle = "MS" + currentScan.getMSLevel() + ", RT "
				+ rtFormat.format(currentScan.getRetentionTime());

		DataPoint basePeak = currentScan.getBasePeak();
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

		if ((src == comboScanNumber)
				|| ((src instanceof JCheckBox) && (src != previewCheckBox))
				|| ((src instanceof JComboBox) && (src != comboDataFileName))) {
			if (previewCheckBox.isSelected()) {
				reloadPreview();
			}
		}

		if (src == comboDataFileName) {
			int ind = comboDataFileName.getSelectedIndex();
			if (ind >= 0) {
				previewDataFile = dataFiles[ind];
				int scanNumbers[] = previewDataFile.getScanNumbers(1);
				Integer scanNumbersObj[] = CollectionUtils
						.toIntegerArray(scanNumbers);
				ComboBoxModel model = new DefaultComboBoxModel(scanNumbersObj);
				comboScanNumber.setModel(model);
				comboScanNumber.setSelectedIndex(0);
				reloadPreview();
			}
		}

		if (src == previewCheckBox) {
			if (previewCheckBox.isSelected()) {
				mainPanel.add(spectrumPlot, BorderLayout.CENTER);
				pnlFileNameScanNumber.setVisible(true);
				pack();
				reloadPreview();
				this.setResizable(true);
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
			} else {
				mainPanel.remove(spectrumPlot);
				pnlFileNameScanNumber.setVisible(false);
				this.setResizable(false);
				pack();
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
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

	/**
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent e) {
		if (previewCheckBox.isSelected()) {
			reloadPreview();
		}
	}

	/**
	 * This function add all the additional components for this dialog over the
	 * original ParameterSetupDialog.
	 * 
	 */
	private void addComponents() {

		// Button's parameters
		String leftArrow = new String(new char[] { '\u2190' });
		String rightArrow = new String(new char[] { '\u2192' });

		// Elements of pnlpreview
		JPanel pnlpreview = new JPanel(new BorderLayout());

		previewCheckBox = new JCheckBox(" Show preview ");
		previewCheckBox.addActionListener(this);
		previewCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
		pnlpreview.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

		pnlpreview.add(new JSeparator(), BorderLayout.NORTH);
		pnlpreview.add(previewCheckBox, BorderLayout.CENTER);

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
		if (previewDataFile != null) {
			comboDataFileName = new JComboBox(dataFiles);
			comboDataFileName.setSelectedItem(previewDataFile);
			comboDataFileName.addActionListener(this);

			int scanNumbers[] = previewDataFile.getScanNumbers(1);
			Integer scanNumbersObj[] = CollectionUtils
					.toIntegerArray(scanNumbers);

			comboScanNumber = new JComboBox(scanNumbersObj);
			comboScanNumber.setSelectedIndex(0);
			comboScanNumber.addActionListener(this);

			pnlFlds.add(comboDataFileName);
			pnlFlds.add(Box.createVerticalStrut(10));

			// --> Elements of pnlScanArrows

			JPanel pnlScanArrows = new JPanel();
			pnlScanArrows.setLayout(new BoxLayout(pnlScanArrows,
					BoxLayout.X_AXIS));

			prevScanBtn = GUIUtils.addButton(pnlScanArrows, leftArrow, null,
					(ActionListener) this, "PREVIOUS_SCAN");
			prevScanBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

			pnlScanArrows.add(Box.createHorizontalStrut(5));
			pnlScanArrows.add(comboScanNumber);
			pnlScanArrows.add(Box.createHorizontalStrut(5));

			nextScanBtn = GUIUtils.addButton(pnlScanArrows, rightArrow, null,
					(ActionListener) this, "NEXT_SCAN");
			nextScanBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

			// <--

			pnlFlds.add(pnlScanArrows);
		}
		// Elements of pnlSpace
		JPanel pnlSpace = new JPanel();
		pnlSpace.setLayout(new BoxLayout(pnlSpace, BoxLayout.Y_AXIS));
		pnlSpace.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		pnlSpace.add(Box.createHorizontalStrut(50));

		// Put all together
		pnlFileNameScanNumber = new JPanel(new BorderLayout());

		pnlFileNameScanNumber.add(pnlpreview, BorderLayout.NORTH);
		pnlFileNameScanNumber.add(pnlLab, BorderLayout.WEST);
		pnlFileNameScanNumber.add(pnlFlds, BorderLayout.CENTER);
		pnlFileNameScanNumber.add(pnlSpace, BorderLayout.EAST);
		pnlFileNameScanNumber.setVisible(false);

		JPanel pnlVisible = new JPanel(new BorderLayout());

		pnlVisible.add(pnlpreview, BorderLayout.NORTH);

		JPanel tmp = new JPanel();
		tmp.add(pnlFileNameScanNumber);
		pnlVisible.add(tmp, BorderLayout.CENTER);

		spectrumPlot = new SpectraPlot(this);

		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		spectrumPlot.setBorder(BorderFactory.createCompoundBorder(one, two));

		componentsPanel.add(pnlVisible, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
	}

}