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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.modules.visualization.histogram.histogramdatalabel.HistogramDataType;
import net.sf.mzmine.modules.visualization.histogram.histogramdatalabel.HistogramPlotDataset;
import net.sf.mzmine.util.Range;

public class HistogramWindow extends JInternalFrame {

    private HistogramChart histogram;

    public HistogramWindow(PeakList peakList, HistogramParameters parameters) {

        super(null, true, true, true, true);
        this.setTitle("Histogram of " + peakList.toString());

        Object dataFileObjects[] = (Object[]) parameters.getParameterValue(HistogramParameters.dataFiles);
        RawDataFile rawDataFiles[] = new RawDataFile[dataFileObjects.length];
        for (int i = 0; i < dataFileObjects.length; i++)
            rawDataFiles[i] = (RawDataFile) dataFileObjects[i];

        HistogramDataType dataType = (HistogramDataType) parameters.getParameterValue(HistogramParameters.dataType);
        int numOfBins = (Integer) parameters.getParameterValue(HistogramParameters.numOfBins);
        Range range = (Range) parameters.getParameterValue(HistogramParameters.rangeData);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        // Creates plot and toolbar
        histogram = new HistogramChart();
        HistogramToolBar toolbar = new HistogramToolBar(
                ((ActionListener) histogram));

        Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        JPanel pnlPlot = new JPanel(new BorderLayout());
        pnlPlot.setBorder(BorderFactory.createCompoundBorder(one, two));
        pnlPlot.setBackground(Color.white);

        pnlPlot.add(toolbar, BorderLayout.EAST);
        pnlPlot.add(histogram, BorderLayout.CENTER);

        add(pnlPlot, BorderLayout.CENTER);
        pack();

        if (peakList != null) {
            HistogramPlotDataset dataSet = new HistogramPlotDataset(peakList,
                    rawDataFiles, numOfBins, dataType, range);
            histogram.addDataset(dataSet, dataType);
        }

    }

}
