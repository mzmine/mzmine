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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.text.DecimalFormat;

import org.jfree.data.xy.AbstractXYDataset;

public class DiagonalPlotDataset extends AbstractXYDataset {

	private double[][] values;
	private String toolTipText;
	private double times;
	private static DecimalFormat formatter1 = new DecimalFormat("###");
	private static DecimalFormat formatter2 = new DecimalFormat("###.###");

	public DiagonalPlotDataset(double[] maxMinValue, double times) {

		if (times == 0)
			times = 1;

		this.times = times;

		double minimum = maxMinValue[0] - (maxMinValue[0] / 2);
		double maximum = maxMinValue[1] + (maxMinValue[1] / 2);

		if (times > 0)
			toolTipText = formatter1.format(times) + "x";
		else
			toolTipText = formatter2.format(times) + "x";
			

		values = new double[2][2];
		values[0][0] = minimum;
		values[0][1] = minimum;
		values[1][0] = maximum;
		values[1][1] = maximum;
	}

	@Override
	public int getSeriesCount() {
		return 1;
	}

	@Override
	public Comparable getSeriesKey(int series) {
		return 1;
	}

	public int getItemCount(int series) {
		return 2;
	}

	public Number getX(int series, int item) {
		return values[item][0];
	}

	public Number getY(int series, int item) {
		return values[item][1] * times;
	}

	public void setTimes(float times) {
		if (times == 0)
			times = 1;
		this.times = times;
	}

	public String getToolTipText() {
		return toolTipText;
	}

}
