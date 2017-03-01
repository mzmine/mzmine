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

package net.sf.mzmine.modules.visualization.intensityplot;

import java.text.Format;

import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.main.MZmineCore;

import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.xy.XYDataset;

/**
 * 
 */
class IntensityPlotTooltipGenerator implements CategoryToolTipGenerator,
	XYToolTipGenerator {

    /**
     * @see org.jfree.chart.labels.CategoryToolTipGenerator#generateToolTip(org.jfree.data.category.CategoryDataset,
     *      int, int)
     */
    public String generateToolTip(CategoryDataset dataset, int row, int column) {
	Format intensityFormat = MZmineCore.getConfiguration()
		.getIntensityFormat();
	Feature peaks[] = ((IntensityPlotDataset) dataset)
		.getPeaks(row, column);
	RawDataFile files[] = ((IntensityPlotDataset) dataset).getFiles(column);

	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < files.length; i++) {
	    sb.append(files[i].getName());
	    sb.append(": ");
	    if (peaks[i] != null) {
		sb.append(peaks[i].toString());
		sb.append(", height: ");
		sb.append(intensityFormat.format(peaks[i].getHeight()));
	    } else {
		sb.append("N/A");
	    }
	    sb.append("\n");
	}

	return sb.toString();
    }

    public String generateToolTip(XYDataset dataset, int series, int item) {
	return generateToolTip((CategoryDataset) dataset, series, item);
    }

}
