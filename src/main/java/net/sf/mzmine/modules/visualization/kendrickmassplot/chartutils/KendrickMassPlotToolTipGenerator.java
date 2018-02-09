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

package net.sf.mzmine.modules.visualization.kendrickmassplot.chartutils;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;

import net.sf.mzmine.datamodel.PeakListRow;

/**
 * Tooltip generator for Kendrick mass plots and Van Krevelen diagrams
 * 
 * @author Ansgar Korf (ansgar.korf@uni-muenster.de)
 */
public class KendrickMassPlotToolTipGenerator implements XYZToolTipGenerator {

	private String xAxisLabel, yAxisLabel, zAxisLabel;
	private NumberFormat numberFormatX = new DecimalFormat("####0.0000");
	private NumberFormat numberFormatY = new DecimalFormat("0.000");
	private PeakListRow rows[];
	private String featureIdentity;

	public KendrickMassPlotToolTipGenerator(String xAxisLabel, String yAxisLabel, String zAxisLabel,
			PeakListRow rows[]) {
		super();
		this.xAxisLabel = xAxisLabel;
		this.yAxisLabel = yAxisLabel;
		this.zAxisLabel = zAxisLabel;
		this.rows = rows;

	}

	@Override
	public String generateToolTip(XYZDataset dataset, int series, int item) {
		if (rows[item].getPreferredPeakIdentity() != null) {
			featureIdentity = rows[item].getPreferredPeakIdentity().getDescription();
			return String.valueOf(
					featureIdentity + "\n" + xAxisLabel + ": " + numberFormatX.format(dataset.getXValue(series, item))
							+ " " + yAxisLabel + ": " + numberFormatY.format(dataset.getYValue(series, item)) + " "
							+ zAxisLabel + ": " + numberFormatY.format(dataset.getZValue(series, item)));
		} else {
			return String.valueOf(xAxisLabel + ": " + numberFormatX.format(dataset.getXValue(series, item)) + " "
					+ yAxisLabel + ": " + numberFormatY.format(dataset.getYValue(series, item)) + " " + zAxisLabel
					+ ": " + numberFormatY.format(dataset.getZValue(series, item)));
		}
	}

	@Override
	public String generateToolTip(XYDataset dataset, int series, int item) {
		if (rows[item].getPreferredPeakIdentity() != null) {
			featureIdentity = rows[item].getPreferredPeakIdentity().getDescription();
			return String.valueOf(
					featureIdentity + "\n" + xAxisLabel + ": " + numberFormatX.format(dataset.getXValue(series, item))
							+ " " + yAxisLabel + ": " + numberFormatY.format(dataset.getYValue(series, item)));
		} else {
			return String.valueOf(xAxisLabel + ": " + numberFormatX.format(dataset.getXValue(series, item)) + " "
					+ yAxisLabel + ": " + numberFormatY.format(dataset.getYValue(series, item)));
		}
	}

}
