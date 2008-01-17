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

package net.sf.mzmine.modules.visualization.spectra;

import java.text.NumberFormat;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.main.MZmineCore;

import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.data.xy.XYDataset;

/**
 * Tooltip generator for picked peaks
 */
class PeakToolTipGenerator implements XYToolTipGenerator {

    private NumberFormat mzFormat = MZmineCore.getDesktop().getMZFormat();
    private NumberFormat intensityFormat = MZmineCore.getDesktop().getIntensityFormat();

    /**
     * @see org.jfree.chart.labels.XYToolTipGenerator#generateToolTip(org.jfree.data.xy.XYDataset,
     *      int, int)
     */
    public String generateToolTip(XYDataset dataset, int series, int item) {

        PeakListDataSet peakListDataSet = (PeakListDataSet) dataset;

        PeakList peakList = peakListDataSet.getPeakList();
        Peak peak = peakListDataSet.getPeak(series, item);
        PeakListRow row = peakList.getPeakRow(peak);
        double intValue = dataset.getYValue(series, item);
        double mzValue = dataset.getXValue(series, item);

        String tooltip = "<html>" + "Peak: " + peak + "<br>Status: "
                + peak.getPeakStatus() + "<br>Peak list row: " + row
                + "<br>Data point m/z: " + mzFormat.format(mzValue)
                + "<br>Data point intensity: "
                + intensityFormat.format(intValue) + "</html>";

        return tooltip;

    }

}
