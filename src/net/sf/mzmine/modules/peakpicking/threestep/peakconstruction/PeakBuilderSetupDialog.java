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

package net.sf.mzmine.modules.peakpicking.threestep.peakconstruction;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.help.DefaultHelpBroker;
import javax.help.HelpBroker;
import javax.help.WindowPresentation;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.desktop.impl.MainWindow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.threestep.ThreeStepPickerParameters;
import net.sf.mzmine.modules.peakpicking.threestep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.Chromatogram;
import net.sf.mzmine.modules.peakpicking.threestep.xicconstruction.ChromatogramBuilder;
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

	// Data sets
	private Hashtable<Integer, PeakDataSet> peakDataSets;

	// Dialog components
	private JPanel pnlPlotXY, pnlLocal, pnlFileNameScanNumber;
	private JComboBox comboDataFileName;
	private JFormattedTextField minTxtFieldRet, maxTxtFieldRet, minTxtFieldMZ,
			maxTxtFieldMZ;
	private JCheckBox preview;
	private JButton btnLoad;
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
	private SimpleParameterSet cbParameters, pbParameters;
	private int peakBuilderTypeNumber, chromatogramBuilderTypeNumber;

	// Desktop
	private Desktop desktop = MZmineCore.getDesktop();

	/**
	 * @param parameters
	 * @param massDetectorTypeNumber
	 */
	public PeakBuilderSetupDialog(ThreeStepPickerParameters parameters,
			int chromatogramBuilderTypeNumber, int peakBuilderTypeNumber) {

		super(ThreeStepPickerParameters.peakBuilderNames[peakBuilderTypeNumber]
				+ "'s parameter setup dialog ", parameters
				.getPeakBuilderParameters(peakBuilderTypeNumber), "PeakBuild"
				+ peakBuilderTypeNumber);

		dataFiles = MZmineCore.getCurrentProject().getDataFiles();

		this.cbParameters = parameters
				.getChromatogramBuilderParameters(chromatogramBuilderTypeNumber);
		this.chromatogramBuilderTypeNumber = chromatogramBuilderTypeNumber;

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

			peakDataSets = new Hashtable<Integer, PeakDataSet>();

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

		if (((src instanceof JCheckBox) && (src != preview))) {
			if (preview.isSelected()) {
				removePeakDataSet();
				setPeakDataSet();
			}
		}

		if (src == btnLoad) {
			if (preview.isSelected()) {
				float minRT = Float.parseFloat(minTxtFieldRet.getValue()
						.toString());
				float maxRT = Float.parseFloat(maxTxtFieldRet.getValue()
						.toString());

				if (minRT > maxRT) {
					String message = "Retention time range not valid ";
					desktop.displayErrorMessage(message);
					return;
				}

				rtRange = new Range(minRT, maxRT);

				float minMZ = Float.parseFloat(minTxtFieldMZ.getValue()
						.toString());
				float maxMZ = Float.parseFloat(maxTxtFieldMZ.getValue()
						.toString());

				if (minMZ > maxMZ) {
					String message = "M/Z range not valid ";
					desktop.displayErrorMessage(message);
					return;
				}

				mzRange = new Range(minMZ, maxMZ);

				listScans = previewDataFile.getScanNumbers(1, rtRange);
				removePeakDataSet();
				setDataSet();
			}
		}

		if (src == comboDataFileName) {

			int ind = comboDataFileName.getSelectedIndex();

			previewDataFile = dataFiles[ind];
			listScans = previewDataFile.getScanNumbers(1);

			rtRange = previewDataFile.getDataRTRange(1);
			minTxtFieldRet.setValue(rtRange.getMin());
			maxTxtFieldRet.setValue(rtRange.getMax());

			mzRange = previewDataFile.getDataMZRange(1);
			minTxtFieldMZ.setValue(mzRange.getMin());
			maxTxtFieldMZ.setValue(mzRange.getMax());

			removePeakDataSet();
			setDataSet();
		}

		if (src == preview) {
			if (preview.isSelected()) {
				pnlFileNameScanNumber.setVisible(true);
				pnlLocal.add(pnlPlotXY, BorderLayout.CENTER);
				comboDataFileName.setEnabled(true);
				add(pnlLocal);
				pack();
				setDataSet();
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
				this.setResizable(true);
			} else {
				pnlFileNameScanNumber.setVisible(false);
				pnlLocal.remove(pnlPlotXY);
				comboDataFileName.setEnabled(false);
				removePeakDataSet();
				pack();
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
				this.setResizable(false);
			}
		}

		if (command.equals("TICDataSet_upgraded")) {
			setPeakDataSet();
		}

	}

	public void propertyChange(PropertyChangeEvent e) {

		synchronized (this) {
			if (preview.isSelected()) {
				removePeakDataSet();
				setPeakDataSet();
			}
		}
	}

	private void setDataSet() {
		ticDataset = new TICDataSet(previewDataFile, listScans, mzRange, this);
		ticPlot.getXYPlot().getDomainAxis().setRange(rtRange.getMin(),
				rtRange.getMax());
		freeMemory();
	}

	/**
	 * First get the actual values in the form, upgrade parameters for our local
	 * mass detector. After calculate all possible peaks, we create a new
	 * PeakListSet for the selected DataFile and Scan in the form.
	 * 
	 * @param ind
	 */
	public void setPeakDataSet() {

		// Create Chromatogram Builder
		String chromatogramBuilderClassName = ThreeStepPickerParameters.chromatogramBuilderClasses[chromatogramBuilderTypeNumber];
		ChromatogramBuilder chromatoBuilder;

		try {
			Class chromatogramBuilderClass = Class
					.forName(chromatogramBuilderClassName);
			Constructor chromatogramBuilderConstruct = chromatogramBuilderClass
					.getConstructors()[0];
			chromatoBuilder = (ChromatogramBuilder) chromatogramBuilderConstruct
					.newInstance(cbParameters);
		} catch (Exception e) {
			String message = "Error trying to make an instance of Chromatogram Builder "
					+ chromatogramBuilderClassName;
			desktop.displayErrorMessage(message);
			logger.finest(message);
			return;
		}

		// Create Peak Builder
		PeakBuilder peakBuilder;
		pbParameters = buildParameterSet(pbParameters);
		String peakBuilderClassName = ThreeStepPickerParameters.peakBuilderClasses[peakBuilderTypeNumber];

		try {
			Class peakBuilderClass = Class.forName(peakBuilderClassName);
			Constructor peakBuilderConstruct = peakBuilderClass
					.getConstructors()[0];
			peakBuilder = (PeakBuilder) peakBuilderConstruct
					.newInstance(pbParameters);
		} catch (Exception e) {
			String message = "Error trying to make an instance of Peak Builder "
					+ peakBuilderClassName;
			desktop.displayErrorMessage(message);
			logger.finest(message);
			return;
		}

		Peak[] peaks;

		for (int i = 0; i < listScans.length; i++) {

			MzPeak[] mzValues = { new MzPeak(new SimpleDataPoint(mzRange
					.getAverage(), ticDataset.getY(0, i).floatValue())) };
			chromatoBuilder.addScan(previewDataFile, previewDataFile
					.getScan(listScans[i]), mzValues);

		}

		Chromatogram[] allChromatograms = chromatoBuilder.finishChromatograms();
		int peakInd = 0;

		for (Chromatogram chromatogram : allChromatograms) {

			peaks = peakBuilder.addChromatogram(chromatogram, previewDataFile);

			if (peaks.length > 0)
				for (Peak p : peaks) {
					PeakDataSet peakDataSet = new PeakDataSet(new PreviewConnectedPeak(p));
					ticPlot.addPeakDataset(peakDataSet);
					peakDataSets.put(Integer.valueOf(peakInd), peakDataSet);
					peakInd++;
				}

			if (peakInd > 60) {
				String message = "Too many peaks detected, please set another parameter values";
				desktop.displayMessage(message);
				logger.finest(message);
				break;
			}

		}

		ticPlot.addTICDataset(ticDataset);
		freeMemory();

	}

	private void removePeakDataSet() {
		int dataSetCount = ticPlot.getXYPlot().getDatasetCount();
		for (int index = 0; index < dataSetCount; index++) {
			ticPlot.getXYPlot().setDataset(index, null);
		}
		ticPlot.startDatasetCounter();
		peakDataSets.clear();
		freeMemory();
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

		pnlpreview.add(new JSeparator(), BorderLayout.NORTH);
		pnlpreview.add(preview, BorderLayout.CENTER);
		pnlpreview.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

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
		minTxtFieldRet.setColumns(TEXTFIELD_COLUMNS);
		minTxtFieldRet.setValue(rtRange.getMin());

		maxTxtFieldRet = new JFormattedTextField(MZmineCore.getRTFormat());
		maxTxtFieldRet.setColumns(TEXTFIELD_COLUMNS);
		maxTxtFieldRet.setValue(rtRange.getMax());

		pnlRetention.add(minTxtFieldRet);
		GUIUtils.addLabel(pnlRetention, " - ");
		pnlRetention.add(maxTxtFieldRet);
		// <--

		// --> Elements of pnlMZ
		JPanel pnlMZ = new JPanel(new FlowLayout());

		minTxtFieldMZ = new JFormattedTextField(MZmineCore.getMZFormat());
		minTxtFieldMZ.setColumns(TEXTFIELD_COLUMNS);
		minTxtFieldMZ.setValue(mzRange.getMin());

		maxTxtFieldMZ = new JFormattedTextField(MZmineCore.getMZFormat());
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

		pnlSpace.add(new JLabel(" "));
		pnlSpace.add(Box.createVerticalStrut(25));
		pnlSpace.add(new JLabel("min. "));
		pnlSpace.add(Box.createVerticalStrut(25));
		pnlSpace.add(new JLabel("m/z "));

		// Elements of pnlLoad
		JPanel pnlLoad = new JPanel();
		pnlLoad.setLayout(new BoxLayout(pnlLoad, BoxLayout.X_AXIS));
		pnlLoad.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));

		btnLoad = new JButton("Update Chromatograph");
		btnLoad.addActionListener(this);

		pnlLoad
				.add(new JLabel(
						"<html><font size=2 color=#336699>For each scan, most intense data point<br> within given"
								+ " m/z range, is used to build<br> the chromatogram.</font><br><br></html>"));
		pnlLoad.add(btnLoad);

		JPanel pnlLoadSep = new JPanel(new BorderLayout());
		pnlLoadSep.add(pnlLoad, BorderLayout.NORTH);
		pnlLoadSep.add(new JSeparator(), BorderLayout.SOUTH);

		// Put all together
		pnlFileNameScanNumber = new JPanel(new BorderLayout());

		pnlFileNameScanNumber.add(pnlLab, BorderLayout.WEST);
		pnlFileNameScanNumber.add(pnlFlds, BorderLayout.CENTER);
		pnlFileNameScanNumber.add(pnlSpace, BorderLayout.EAST);
		pnlFileNameScanNumber.add(pnlLoadSep, BorderLayout.SOUTH);
		pnlFileNameScanNumber.setVisible(false);

		JPanel pnlVisible = new JPanel(new BorderLayout());

		pnlVisible.add(pnlpreview, BorderLayout.NORTH);
		pnlVisible.add(pnlFileNameScanNumber, BorderLayout.SOUTH);

		// Panel for XYPlot
		pnlPlotXY = new JPanel(new BorderLayout());
		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
		pnlPlotXY.setBackground(Color.white);

		ticPlot = new TICPlot((ActionListener) this);
		pnlPlotXY.add(ticPlot, BorderLayout.CENTER);

		toolBar = new TICToolBar(ticPlot);
		toolBar.getComponentAtIndex(0).setVisible(false);
		pnlPlotXY.add(toolBar, BorderLayout.EAST);

		labelsAndFields.add(pnlVisible, BorderLayout.SOUTH);

		// Complete panel for this dialog including pnlPlotXY
		pnlLocal = new JPanel(new BorderLayout());

		pnlLocal.add(pnlAll, BorderLayout.WEST);
	}

	public TICDataSet[] getDataSet() {
		TICDataSet[] ticDatasets = { ticDataset };
		return ticDatasets;
	}

	private void freeMemory() {
		System.gc();
	}

}