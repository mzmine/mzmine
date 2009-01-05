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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.tic;

import java.text.NumberFormat;

import net.sf.mzmine.main.MZmineCore;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Tooltip generator for TIC visualizer
 */
class TICToolTipGenerator implements XYToolTipGenerator {

    private NumberFormat rtFormat = MZmineCore.getRTFormat();
    private NumberFormat mzFormat = MZmineCore.getMZFormat();
    private NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

    /**
     * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateToolTip(XYDataset dataset, int series, int item) {

        String toolTip;

        double rtValue = dataset.getXValue(series, item);
        double intValue = dataset.getYValue(series, item);

        if (dataset instanceof TICDataSet) {

            TICDataSet ticDataSet = (TICDataSet) dataset;

            double mzValue = ticDataSet.getZValue(series, item);
            int scanNumber = ticDataSet.getScanNumber(series, item);

            toolTip = "<html>Scan #" + scanNumber + "<br>Retention time: "
                    + rtFormat.format(rtValue) + "<br>Base peak m/z: "
                    + mzFormat.format(mzValue) + "<br>Intensity: "
                    + intensityFormat.format(intValue) + "</html>";
        } else {
            toolTip = "<html>Retention time: " + rtFormat.format(rtValue)
                    + "<br>Intensity: " + intensityFormat.format(intValue)
                    + "</html>";
        }

        return toolTip;
    }

}
