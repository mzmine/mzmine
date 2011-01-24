/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.event.ActionEvent;
import java.text.NumberFormat;

import javax.swing.JComboBox;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.histogram.histogramdatalabel.HistogramDataType;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.components.RangeComponent;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

public class HistogramSetupDialog extends ParameterSetupDialog {

	private JComboBox dataTypeComponent;
	private RangeComponent dataRangeComponent;
	private PeakList peakList;

	public HistogramSetupDialog(String title, SimpleParameterSet parameterSet,
			PeakList peakList, String helpID) {

		super(title, parameterSet, helpID);
		this.peakList = peakList;

		dataTypeComponent = (JComboBox) getComponentForParameter(HistogramParameters.dataType);
		dataTypeComponent.addActionListener(this);

		dataRangeComponent = (RangeComponent) getComponentForParameter(HistogramParameters.rangeData);

		updateNumberFormats();

	}

	/**
     * 
     */
	@Override
	public void actionPerformed(ActionEvent event) {

		super.actionPerformed(event);

		// When dialog is created, dataRangeComponent may be still null when we
		// get here
		if (dataRangeComponent == null)
			return;

		Object source = event.getSource();

		if (source == dataTypeComponent) {

			updateNumberFormats();
			updateRangeValue();
		}

	}

	private void updateNumberFormats() {
		HistogramDataType dataType = (HistogramDataType) dataTypeComponent
				.getSelectedItem();
		NumberFormat format = getAxisNumberFormat(dataType);
		dataRangeComponent.setNumberFormat(format);
	}

	private void updateRangeValue() {

		RawDataFile rawDataFiles[] = peakList.getRawDataFiles();

		HistogramDataType dataType = (HistogramDataType) dataTypeComponent
				.getSelectedItem();

		Range valueRange = calculateRange(dataType, rawDataFiles);
		if (valueRange != null)
			dataRangeComponent.setRangeValue(valueRange);

	}

	private Range calculateRange(HistogramDataType dataType,
			RawDataFile rawDataFiles[]) {

		Range range = null;

		for (RawDataFile dataFile : rawDataFiles) {
			for (ChromatographicPeak peak : peakList.getPeaks(dataFile)) {

				double value = 0;

				switch (dataType) {
				case AREA:
					value = peak.getArea();
					break;
				case HEIGHT:
					value = peak.getHeight();
					break;
				case MASS:
					value = peak.getMZ();
					break;
				case RT:
					value = peak.getRT();
					break;
				}

				if (Double.isNaN(value))
					continue;

				if (range == null)
					range = new Range(value);
				else
					range.extendRange(value);
			}

		}
		return range;
	}

	private NumberFormat getAxisNumberFormat(HistogramDataType dataType) {

		NumberFormat formatter = null;
		switch (dataType) {
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
