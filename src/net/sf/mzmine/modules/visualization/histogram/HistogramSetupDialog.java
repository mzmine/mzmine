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

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.mzmineclient.MZmineCore;
import net.sf.mzmine.modules.visualization.histogram.histogramdatalabel.HistogramDataType;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.components.ExtendedCheckBox;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class HistogramSetupDialog extends ParameterSetupDialog {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private SimpleParameterSet localParameters;
	private JComboBox dataTypeComponent; 
	private JPanel dataRangeComponent;
	private PeakList peakList;
	private RawDataFile[] rawDataFiles;
	private boolean initial = true;

	public HistogramSetupDialog(String title, SimpleParameterSet parameters,
			PeakList peakList) {

		super(title, parameters);
		this.localParameters = parameters;
		this.peakList = peakList;

		Vector<RawDataFile> dataFiles = new Vector<RawDataFile>();
		for (ExtendedCheckBox box : multipleCheckBoxes) {
			Object genericObject = box.getObject();
			dataFiles.add((RawDataFile) genericObject);
			box.addActionListener(this);
		}
		rawDataFiles = dataFiles.toArray(new RawDataFile[0]);

		Parameter p = localParameters.getParameter("Plotted data type");
		dataTypeComponent = (JComboBox) getComponentForParameter(p);
		dataTypeComponent.addActionListener(this);

		p = localParameters.getParameter("Plotted data range");
		dataRangeComponent = (JPanel) getComponentForParameter(p);
		
		actionPerformed(new ActionEvent(dataTypeComponent, 0, ""));

	}

	/**
	 * 
	 */
	@Override
	public void actionPerformed(ActionEvent event) {

		super.actionPerformed(event);

		Object source = event.getSource();

		if ((source instanceof JComboBox) ||
			(source instanceof ExtendedCheckBox) ){

			try {

				if (!initial) {
					Vector<RawDataFile> dataFiles = new Vector<RawDataFile>();
					for (ExtendedCheckBox box : multipleCheckBoxes) {
						if (box.isSelected()) {
							Object genericObject = box.getObject();
							dataFiles.add((RawDataFile) genericObject);
						}
					}
					rawDataFiles = dataFiles.toArray(new RawDataFile[0]);
				}
				
				initial = false;

				if (rawDataFiles.length == 0) {
					throw (new Exception(
							"Please select at least one option from multiple selection parameter"));
				}

				HistogramDataType dataType = (HistogramDataType) dataTypeComponent.getSelectedItem();
				Range valueRange = calculateRange(dataType);
				NumberFormat formatter = getAxisNumberFormat(dataType);
				JPanel panel = (JPanel) dataRangeComponent;
				
				JFormattedTextField minField = new JFormattedTextField(formatter);//(JFormattedTextField) panel
						//.getComponent(0);
				minField.setValue(valueRange.getMin());
				minField.setPreferredSize(new Dimension(80,minField.getPreferredSize().height));
				minField.setHorizontalAlignment(JFormattedTextField.CENTER);
				panel.getComponent(0).setVisible(false);
				panel.remove(0);
				panel.add(minField, 0);
				
				JFormattedTextField maxField = new JFormattedTextField(formatter);//(JFormattedTextField) panel
						//.getComponent(2);
				maxField.setValue(valueRange.getMax());
				maxField.setPreferredSize(new Dimension(80,maxField.getPreferredSize().height));
				maxField.setHorizontalAlignment(JFormattedTextField.CENTER);
				panel.getComponent(2).setVisible(false);
				panel.remove(2);
				panel.add(maxField, 2);
				
				panel.addNotify();
				
				pack();

			} catch (Exception e) {
				desktop.displayMessage(e.getMessage());
			}
		}

	}

	private Range calculateRange(HistogramDataType dataType) {
		double minimum = Double.MAX_VALUE, maximum = 0;
		ChromatographicPeak[] peaks;
		double[] values = null;
		for (RawDataFile dataFile : rawDataFiles) {
			peaks = peakList.getPeaks(dataFile);
			values = new double[peaks.length];
			for (int i = 0; i < peaks.length; i++) {
				switch (dataType) {
				case AREA:
					values[i] = peaks[i].getArea();
					break;
				case HEIGHT:
					values[i] = peaks[i].getHeight();
					break;
				case MASS:
					values[i] = peaks[i].getMZ();
					break;
				case RT:
					values[i] = peaks[i].getRT();
					break;
				}

				minimum = Math.min(values[i], minimum);
				maximum = Math.max(values[i], maximum);
			}
		}
		return new Range(minimum, maximum);
	}
	
	private NumberFormat getAxisNumberFormat(HistogramDataType dataType){
		
		NumberFormat formatter = null;
		switch (dataType){
		case AREA:
			formatter = MZmineCore.getIntensityFormat();
			break;
		case MASS:
			formatter = MZmineCore.getMZFormat();
			break;
		case HEIGHT:
			formatter = MZmineCore.getIntensityFormat();
			break;
		case RT:
			formatter = MZmineCore.getRTFormat();
			break;
		}
		return formatter;
	}

}
