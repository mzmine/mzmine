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

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.desktop.impl.WindowsMenu;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.WindowSettingsParameter;

import com.google.common.collect.Range;

public class HistogramWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    private HistogramChart histogram;

    public HistogramWindow(ParameterSet parameters) {

        super("");

        PeakList peakList = parameters
                .getParameter(HistogramParameters.peakList).getValue()
                .getMatchingPeakLists()[0];

        this.setTitle("Histogram of " + peakList.getName());

        RawDataFile rawDataFiles[] = parameters.getParameter(
                HistogramParameters.dataFiles).getValue();

        HistogramDataType dataType = parameters.getParameter(
                HistogramParameters.dataRange).getType();
        int numOfBins = parameters.getParameter(HistogramParameters.numOfBins)
                .getValue();
        Range<Double> range = parameters.getParameter(
                HistogramParameters.dataRange).getValue();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setBackground(Color.white);

        // Creates plot and toolbar
        histogram = new HistogramChart();

        Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
        Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);

        JPanel pnlPlot = new JPanel(new BorderLayout());
        pnlPlot.setBorder(BorderFactory.createCompoundBorder(one, two));
        pnlPlot.setBackground(Color.white);

        pnlPlot.add(histogram, BorderLayout.CENTER);

        add(pnlPlot, BorderLayout.CENTER);

        // Add the Windows menu
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(new WindowsMenu());
        setJMenuBar(menuBar);

        pack();

        // get the window settings parameter
        ParameterSet paramSet = MZmineCore.getConfiguration()
                .getModuleParameters(HistogramVisualizerModule.class);
        WindowSettingsParameter settings = paramSet
                .getParameter(HistogramParameters.windowSettings);

        // update the window and listen for changes
        settings.applySettingsToWindow(this);
        this.addComponentListener(settings);

        if (peakList != null) {
            HistogramPlotDataset dataSet = new HistogramPlotDataset(peakList,
                    rawDataFiles, numOfBins, dataType, range);
            histogram.addDataset(dataSet, dataType);
        }

    }

    HistogramChart getChart() {
        return histogram;
    }

}
