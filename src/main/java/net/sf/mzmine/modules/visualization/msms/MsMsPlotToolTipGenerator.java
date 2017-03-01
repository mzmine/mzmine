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

package net.sf.mzmine.modules.visualization.msms;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

public class MsMsPlotToolTipGenerator implements XYToolTipGenerator {

    private NumberFormat rtFormat = MZmineCore.getConfiguration().getRTFormat();
    private NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();
    private NumberFormat intensityFormat = MZmineCore.getConfiguration()
	    .getIntensityFormat();

    @Override
    public String generateToolTip(XYDataset dataset, int series, int item) {

	MsMsDataSet IDADataSet = (MsMsDataSet) dataset;

	return String.valueOf("RT: "
		+ rtFormat.format(IDADataSet.getX(series, item)) + "\nm/z: "
		+ mzFormat.format(IDADataSet.getY(series, item))
		+ "\nIntensity: "
		+ intensityFormat.format(IDADataSet.getZ(series, item)));
    }

}
