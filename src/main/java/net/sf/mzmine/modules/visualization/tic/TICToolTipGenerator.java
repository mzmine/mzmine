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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.tic;

import java.text.NumberFormat;
import java.util.Map;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakInformation;
import net.sf.mzmine.main.MZmineCore;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Tooltip generator for TIC visualizer
 */
public class TICToolTipGenerator implements XYToolTipGenerator {

    private final NumberFormat rtFormat = MZmineCore.getConfiguration()
	    .getRTFormat();
    private final NumberFormat mzFormat = MZmineCore.getConfiguration()
	    .getMZFormat();
    private final NumberFormat intensityFormat = MZmineCore.getConfiguration()
	    .getIntensityFormat();

    @Override
    public String generateToolTip(final XYDataset dataSet, final int series,
	    final int item) {

	final String toolTip;

	final double rtValue = dataSet.getXValue(series, item);
	final double intValue = dataSet.getYValue(series, item);

	if (dataSet instanceof TICDataSet) {

	    final TICDataSet ticDataSet = (TICDataSet) dataSet;

	    toolTip = "Scan #" + ticDataSet.getScanNumber(item)
		    + "\nRetention time: " + rtFormat.format(rtValue)
		    + "\nBase peak m/z: "
		    + mzFormat.format(ticDataSet.getZValue(series, item))
		    + "\nIntensity: " + intensityFormat.format(intValue);

	} else if (dataSet instanceof PeakDataSet) {

	    final PeakDataSet peakDataSet = (PeakDataSet) dataSet;
	    final Feature feature  = peakDataSet.getFeature();
		PeakInformation peakInfo = null;
		if (feature != null) peakInfo = feature.getPeakInformation();

	    final String label = peakDataSet.getName();
	    String text = label == null || label.length() == 0 ? ""
		    : label + '\n';
	    text += "Retention time: " + rtFormat.format(rtValue)
				+ "\nm/z: " + mzFormat.format(peakDataSet.getMZ(item))
				+ "\nIntensity: " + intensityFormat.format(intValue);

	    NumberFormat numberFormat = NumberFormat.getInstance();

	    if (peakInfo != null)
			for (Map.Entry<String, String> e : peakInfo.getAllProperties().entrySet()) {
	    	    try {
	    	        double value = Double.parseDouble(e.getValue());
                    text += "\n" + e.getKey() + ": " + numberFormat.format(value);
                }
                catch (NullPointerException | NumberFormatException exception) {
	    	        continue;
                }
			}

	    toolTip = text;

	} else {

	    toolTip = "Retention time: " + rtFormat.format(rtValue)
		    + "\nIntensity: " + intensityFormat.format(intValue);
	}

	return toolTip;
    }
}
