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

package net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart;

import java.text.DecimalFormat;

import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.data.xy.XYDataset;

public class DiagonalLineLabelGenerator implements XYItemLabelGenerator {

    // We use 3 decimal digits, to show ratios such as "0.125x"
    public static final DecimalFormat labelFormat = new DecimalFormat("0.###");

    /**
     * @see org.jfree.chart.labels.XYItemLabelGenerator#generateLabel(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateLabel(XYDataset dataSet, int series, int item) {

	DiagonalLineDataset diagonalDataSet = (DiagonalLineDataset) dataSet;

	double doubleFold = 0;
	switch (series) {
	case 0:
	    doubleFold = diagonalDataSet.getFold();
	    break;
	case 1:
	    doubleFold = 1d;
	    break;
	case 2:
	    doubleFold = 1d / diagonalDataSet.getFold();
	    break;
	}

	String label = labelFormat.format(doubleFold) + "x";

	return label;

    }
}
