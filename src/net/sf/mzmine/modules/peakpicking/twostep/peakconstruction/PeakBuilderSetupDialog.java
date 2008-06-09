/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.twostep.peakconstruction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.twostep.TwoStepPickerParameters;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;
import net.sf.mzmine.modules.visualization.tic.PeakDataSet;
import net.sf.mzmine.modules.visualization.tic.TICDataSet;
import net.sf.mzmine.modules.visualization.tic.TICPlot;
import net.sf.mzmine.modules.visualization.tic.TICToolBar;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected mass detector and his parameters works
 * over the raw data file.
 */
public class PeakBuilderSetupDialog extends ParameterSetupDialog implements
		ActionListener, PropertyChangeListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private RawDataFile previewDataFile;
	private RawDataFile[] dataFiles;
	private String[] fileNames;

	// Dialog components
	private JPanel pnlPlotXY, pnlLocal;
	private JComboBox comboDataFileName;
	private JFormattedTextField minTxtFieldRet, maxTxtFieldRet, minTxtFieldMZ,
			maxTxtFieldMZ;
	private JCheckBox preview;
	private int indexComboFileName;

	// Currently loaded chromatograph
	private String[] currentScanNumberlist;
	private int[] listScans;
	private Range rtRange, mzRange;

	// XYPlot
	private TICToolBar toolBar;
	private TICPlot ticPlot;
	private TICDataSet ticDataset;

	// Mass Detector;
	private PeakBuilder peakBuilder;
	private SimpleParameterSet pbParameters;
	private int peakBuilderTypeNumber;

	// Desktop
	private Desktop desktop = MZmineCore.getDesktop();

	/**
	 * @param parameters
	 * @param massDetectorTypeNumber
	 */
	public PeakBuilderSetupDialog(TwoStepPickerParameters parameters,
			int peakBuilderTypeNumber) {

		super(TwoStepPickerParameters.peakBuilderNames[peakBuilderTypeNumber]
				+ "'s parameter setup dialog ", parameters
				.getPeakBuilderParameters(peakBuilderTypeNumber));

		dataFiles = MZmineCore.getCurrentProject().getDataFiles();
		this.peakBuilderTypeNumber = peakBuilderTypeNumber;

		if (dataFiles.length != 0) {

			if (desktop.getSelectedDataFiles().length != 0)
				previewDataFile = desktop.getSelectedDataFiles()[0];
			else
				previewDataFile = dataFiles[0];

			// Parameters of local mass detector to get preview values
			pbParameters = parameters.getPeakBuilderParameters(
					peakBuilderTypeNumber).clone();

			// List of scan to apply mass detector
			listScans = previewDataFile.getScanNumbers(1);
			rtRange = previewDataFile.getDataRTRange(1);
			mzRange = previewDataFile.getDataMZRange(1);

			currentScanNumberlist = new String[listScans.length];
			for (int i = 0; i < listScans.length; i++)
				currentScanNumberlist[i] = String.valueOf(listScans[i]);

			fileNames = new String[dataFiles.length];

			for (int i = 0; i < dataFiles.length; i++) {
				fileNames[i] = dataFiles[i].getFileName();
				if (fileNames[i].equals(previewDataFile.getFileName()))
					indexComboFileName = i;
			}

			// Set a listener in all parameters's fields to add functionality to
			// this dialog
			Component[] fields = pnlFields.getComponents();
			for (Component field : fields) {
				field.addPropertyChangeListener("value", this);
				if (field instanceof JCheckBox)
					((JCheckBox) field).addActionListener(this);
			}

			// Add all complementary components for this dialog
			addComponentsPnl();
			add(pnlLocal);
			pack();
			setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

		}
	}

	public void actionPerformed(ActionEvent event) {

		Object src = event.getSource();
		String command = event.getActionCommand();

		if (src == btnOK) {
			super.actionPerformed(event);
		}

		if (src == btnCancel) {
			dispose();
		}

		if (src == comboDataFileName) {
			int ind = comboDataFileName.getSelectedIndex();
			previewDataFile = dataFiles[ind];
			listScans = previewDataFile.getScanNumbers(1);
			rtRange = previewDataFile.getDataRTRange(1);
			mzRange = previewDataFile.getDataMZRange(1);

			enablePreviewComponents(false);

			minTxtFieldRet.setValue(rtRange.getMin());
			maxTxtFieldRet.setValue(rtRange.getMax());
			minTxtFieldMZ.setValue(mzRange.getMin());
			maxTxtFieldMZ.setValue(mzRange.getMax());

			enablePreviewComponents(true);
			removePeakDataSet();
			setDataSet();
		}

		if (src == preview) {
			if (preview.isSelected()) {
				pnlLocal.add(pnlPlotXY, BorderLayout.CENTER);
				comboDataFileName.setEnabled(true);
				add(pnlLocal);
				pack();
				enablePreviewComponents(true);
				setDataSet();
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
			} else {
				pnlLocal.remove(pnlPlotXY);
				comboDataFileName.setEnabled(false);
				removePeakDataSet();
				pack();
				enablePreviewComponents(false);
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
			}
		}

		if (command.equals("TICDataSet_upgraded")) {
			setPeakDataSet();
		}

	}

	public void propertyChange(PropertyChangeEvent e) {

		Object src = e.getSource();
		synchronized (this) {

			if (preview.isSelected()) {
				removePeakDataSet();
				if ((src == minTxtFieldRet) || (src == maxTxtFieldRet)) {
					rtRange = new Range(Float.parseFloat(minTxtFieldRet
							.getValue().toString()), Float
							.parseFloat(maxTxtFieldRet.getValue().toString()));
					listScans = previewDataFile.getScanNumbers(1, rtRange);
					setDataSet();
					return;
				}
				if ((src == minTxtFieldMZ) || (src == maxTxtFieldMZ)) {
					mzRange = new Range(Float.parseFloat(minTxtFieldMZ
							.getValue().toString()), Float
							.parseFloat(maxTxtFieldMZ.getValue().toString()));
					setDataSet();
					return;
				}
				setPeakDataSet();
			}
		}
	}

	
	synchronized public void setDataSet() {
		ticDataset = new TICDataSet(previewDataFile, listScans, mzRange, this);
		ticPlot.getXYPlot().getDomainAxis().setRange(rtRange.getMin(),
				rtRange.getMax());
	}

	/**
	 * First get the actual values in the form, upgrade parameters for our local
	 * mass detector. After calculate all possible peaks, we create a new
	 * PeakListSet for the selected DataFile and Scan in the form.
	 * 
	 * @param ind
	 */
	synchronized public void setPeakDataSet() {

		buildParameterSetPeakBuilder();
		String peakBuilderClassName = TwoStepPickerParameters.peakBuilderClasses[peakBuilderTypeNumber];

		try {
			Class peakBuilderClass = Class.forName(peakBuilderClassName);
			Constructor massDetectorConstruct = peakBuilderClass
					.getConstructors()[0];
			peakBuilder = (PeakBuilder) massDetectorConstruct
					.newInstance(pbParameters);
		} catch (Exception e) {
			desktop
					.displayErrorMessage("Error trying to make an instance of mass detector "
							+ peakBuilderClassName);
			logger.finest("Error trying to make an instance of mass detector "
					+ peakBuilderClassName);
			return;
		}

		Peak[] peaks;
		Vector<Peak> totalPeaks = new Vector<Peak>();
		float mz = mzRange.getAverage();
		for (int i = 0; i < listScans.length; i++) {
			MzPeak[] mzValues = { new MzPeak(new SimpleDataPoint(mz, ticDataset
					.getY(0, i).floatValue())) };
			peaks = peakBuilder.addScan(previewDataFile.getScan(listScans[i]),
					mzValues, previewDataFile);
			if (peaks.length > 0)
				for (Peak p : peaks)
					totalPeaks.add(p);
		}
		
		peaks = peakBuilder.finishPeaks();
		if (peaks.length > 0)
			for (Peak p : peaks)
				totalPeaks.add(p);
		
		if (!totalPeaks.isEmpty()) {
			for (Peak peak : totalPeaks) {
				PeakDataSet peakDataSet = new PeakDataSet(peak);
				ticPlot.addPeakDataset(peakDataSet);
			}
		}
		ticPlot.addTICDataset(ticDataset);

	}

	private void removePeakDataSet() {
		int dataSetCount = ticPlot.getXYPlot().getDatasetCount();
		for (int index = 0; index < dataSetCount; index++) {
			ticPlot.getXYPlot().setDataset(index, null);
		}
		ticPlot.startDatasetCounter();
	}

	/**
	 * This function collect all the information from the form's filed and build
	 * the ParameterSet.
	 * 
	 */
	void buildParameterSetPeakBuilder() {
		Iterator<Parameter> paramIter = parametersAndComponents.keySet()
				.iterator();
		while (paramIter.hasNext()) {
			Parameter p = paramIter.next();

			try {

				Object[] possibleValues = p.getPossibleValues();
				if (possibleValues != null) {
					JComboBox combo = (JComboBox) parametersAndComponents
							.get(p);
					pbParameters.setParameterValue(p, possibleValues[combo
							.getSelectedIndex()]);
					continue;
				}

				switch (p.getType()) {
				case INTEGER:
					JFormattedTextField intField = (JFormattedTextField) parametersAndComponents
							.get(p);
					Integer newIntValue = ((Number) intField.getValue())
							.intValue();
					pbParameters.setParameterValue(p, newIntValue);
					break;
				case FLOAT:
					JFormattedTextField doubleField = (JFormattedTextField) parametersAndComponents
							.get(p);
					Float newFloatValue = ((Number) doubleField.getValue())
							.floatValue();
					pbParameters.setParameterValue(p, newFloatValue);
					break;
				case RANGE:
					JPanel panel = (JPanel) parametersAndComponents.get(p);
					JFormattedTextField minField = (JFormattedTextField) panel
							.getComponent(0);
					JFormattedTextField maxField = (JFormattedTextField) panel
							.getComponent(2);
					float minValue = ((Number) minField.getValue())
							.floatValue();
					float maxValue = ((Number) maxField.getValue())
							.floatValue();
					Range rangeValue = new Range(minValue, maxValue);
					pbParameters.setParameterValue(p, rangeValue);
					break;
				case STRING:
					JTextField stringField = (JTextField) parametersAndComponents
							.get(p);
					pbParameters.setParameterValue(p, stringField.getText());
					break;
				case BOOLEAN:
					JCheckBox checkBox = (JCheckBox) parametersAndComponents
							.get(p);
					Boolean newBoolValue = checkBox.isSelected();
					pbParameters.setParameterValue(p, newBoolValue);
					break;
				}

			} catch (Exception invalidValueException) {
				desktop.displayMessage(invalidValueException.getMessage());
				return;
			}

		}
	}


	/**
	 * This function add all the additional components for this dialog over the
	 * original ParameterSetupDialog.
	 * 
	 */
	private void addComponentsPnl() {

		// Elements of pnlpreview
		JPanel pnlpreview = new JPanel(new BorderLayout());
		preview = new JCheckBox(" Show preview of peak building ");
		preview.addActionListener(this);
		preview.setHorizontalAlignment(SwingConstants.CENTER);

		JSeparator line = new JSeparator();

		pnlpreview.add(line, BorderLayout.NORTH);
		pnlpreview.add(preview, BorderLayout.CENTER);

		// Elements of pnlLab
		JPanel pnlLab = new JPanel();
		pnlLab.setLayout(new BoxLayout(pnlLab, BoxLayout.Y_AXIS));
		pnlLab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		JLabel lblFileSelected = new JLabel("Data file ");
		JLabel lblRetentionTime = new JLabel("Retention time");
		JLabel lblMZRange = new JLabel("M/Z range");

		pnlLab.add(lblFileSelected);
		pnlLab.add(Box.createVerticalStrut(25));
		pnlLab.add(lblRetentionTime);
		pnlLab.add(Box.createVerticalStrut(25));
		pnlLab.add(lblMZRange);

		// Elements of pnlFlds
		JPanel pnlFlds = new JPanel();
		pnlFlds.setLayout(new BoxLayout(pnlFlds, BoxLayout.Y_AXIS));
		pnlFlds.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		comboDataFileName = new JComboBox(fileNames);
		comboDataFileName.setSelectedIndex(indexComboFileName);
		comboDataFileName.setBackground(Color.WHITE);
		comboDataFileName.addActionListener(this);
		comboDataFileName.setEnabled(false);

		// --> Elements of pnlRetention
		JPanel pnlRetention = new JPanel(new FlowLayout());

		minTxtFieldRet = new JFormattedTextField(MZmineCore.getRTFormat());
		minTxtFieldRet.setEnabled(false);
		minTxtFieldRet.setColumns(TEXTFIELD_COLUMNS);
		minTxtFieldRet.setValue(rtRange.getMin());

		maxTxtFieldRet = new JFormattedTextField(MZmineCore.getRTFormat());
		maxTxtFieldRet.setEnabled(false);
		maxTxtFieldRet.setColumns(TEXTFIELD_COLUMNS);
		maxTxtFieldRet.setValue(rtRange.getMax());

		pnlRetention.add(minTxtFieldRet);
		GUIUtils.addLabel(pnlRetention, " - ");
		pnlRetention.add(maxTxtFieldRet);
		// <--

		// --> Elements of pnlMZ
		JPanel pnlMZ = new JPanel(new FlowLayout());

		minTxtFieldMZ = new JFormattedTextField(MZmineCore.getMZFormat());
		minTxtFieldMZ.setEnabled(false);
		minTxtFieldMZ.setColumns(TEXTFIELD_COLUMNS);
		minTxtFieldMZ.setValue(mzRange.getMin());

		maxTxtFieldMZ = new JFormattedTextField(MZmineCore.getMZFormat());
		maxTxtFieldMZ.setEnabled(false);
		maxTxtFieldMZ.setColumns(TEXTFIELD_COLUMNS);
		maxTxtFieldMZ.setValue(mzRange.getMax());

		pnlMZ.add(minTxtFieldMZ);
		GUIUtils.addLabel(pnlMZ, " - ");
		pnlMZ.add(maxTxtFieldMZ);
		// <--

		pnlFlds.add(comboDataFileName);
		pnlFlds.add(Box.createVerticalStrut(10));
		pnlFlds.add(pnlRetention);
		pnlFlds.add(Box.createVerticalStrut(10));
		pnlFlds.add(pnlMZ);
		pnlFlds.add(Box.createVerticalStrut(10));

		// Elements of pnlSpace
		JPanel pnlSpace = new JPanel();
		pnlSpace.setLayout(new BoxLayout(pnlSpace, BoxLayout.Y_AXIS));
		pnlSpace.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		pnlSpace.add(Box.createHorizontalStrut(50));

		// Put all together
		JPanel pnlFileNameScanNumber = new JPanel(new BorderLayout());

		pnlFileNameScanNumber.add(pnlpreview, BorderLayout.NORTH);
		pnlFileNameScanNumber.add(pnlLab, BorderLayout.WEST);
		pnlFileNameScanNumber.add(pnlFlds, BorderLayout.CENTER);
		pnlFileNameScanNumber.add(pnlSpace, BorderLayout.EAST);

		// Panel for XYPlot
		pnlPlotXY = new JPanel(new BorderLayout());
		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
		pnlPlotXY.setBackground(Color.white);

		/*
		 * spectrumPlot = new SpectraPlot(this); MzPeakToolTipGenerator
		 * mzPeakToolTipGenerator = new MzPeakToolTipGenerator(); spectrumPlot
		 * .setPeakToolTipGenerator((XYToolTipGenerator)
		 * mzPeakToolTipGenerator);
		 */

		ticPlot = new TICPlot(this);
		pnlPlotXY.add(ticPlot, BorderLayout.CENTER);

		toolBar = new TICToolBar(this);
		// spectrumPlot.setRelatedToolBar(toolBar);
		pnlPlotXY.add(toolBar, BorderLayout.EAST);

		labelsAndFields.add(pnlFileNameScanNumber, BorderLayout.SOUTH);

		// Complete panel for this dialog including pnlPlotXY
		pnlLocal = new JPanel(new BorderLayout());

		pnlLocal.add(pnlAll, BorderLayout.WEST);
	}

	private void enablePreviewComponents(boolean logic) {

		minTxtFieldRet.setEnabled(logic);
		maxTxtFieldRet.setEnabled(logic);
		minTxtFieldMZ.setEnabled(logic);
		maxTxtFieldMZ.setEnabled(logic);
		this.setResizable(logic);

		if (logic) {
			minTxtFieldRet.addPropertyChangeListener("value", this);
			maxTxtFieldRet.addPropertyChangeListener("value", this);
			minTxtFieldMZ.addPropertyChangeListener("value", this);
			maxTxtFieldMZ.addPropertyChangeListener("value", this);
		} else {
			minTxtFieldRet.removePropertyChangeListener("value", this);
			maxTxtFieldRet.removePropertyChangeListener("value", this);
			minTxtFieldMZ.removePropertyChangeListener("value", this);
			maxTxtFieldMZ.removePropertyChangeListener("value", this);
		}

	}

}