/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.userinterface.components;

import java.awt.Color;
import java.awt.Dimension;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.io.OpenedRawDataFile;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;


/**
 * Simple lightweight component for plotting peak shape
 */
public class PeakComponent extends ChartPanel {

    private JFreeChart chart;
    private DefaultTableXYDataset dataset;
    private XYSeries series; 
    
    public PeakComponent(Peak peak) {
        
        super(null, true);
        
        // create and fill the dataset
        dataset = new DefaultTableXYDataset();
        series = new XYSeries(peak.toString(), false, false);
        dataset.addSeries(series);
        
        OpenedRawDataFile dataFile = peak.getDataFile();
        int scanNumbers[] = peak.getScanNumbers();
        
        for (int scan : scanNumbers) {
            double dataPoints[][] = peak.getRawDatapoints(scan);
            // find maximum intensity
            double maxIntensity = 0;
            for (int i = 0; i < dataPoints.length; i++) {
                double[] point = dataPoints[i];
                if (point[1] > maxIntensity) maxIntensity = point[1];
            }
            double retentionTime = dataFile.getCurrentFile().getRetentionTime(scan);
            series.add(retentionTime, maxIntensity);
        }
        
        
        // initialize the chart by default time series chart from factory
        chart = ChartFactory.createXYLineChart(null, // title
                null, // x-axis label
                null, // y-axis label
                dataset, // data set
                PlotOrientation.VERTICAL, // orientation
                false, // create legend?
                false, // generate tooltips?
                false // generate URLs?
                );
        
        chart.setBackgroundPaint(Color.white);
        
        XYPlot plot = (XYPlot) chart.getPlot();

        plot.setDomainCrosshairVisible(false);
        plot.setRangeCrosshairVisible(false);
        //plot.clearDomainAxes();
        //plot.clearRangeAxes();
        
        plot.getDomainAxis().setRange(dataFile.getCurrentFile().getDataMinRT(1), dataFile.getCurrentFile().getDataMaxRT(1));
        
        setChart(chart);
        
        // disable maximum size (we don't want scaling)
        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);
      //  setMinimumDrawWidth(1);
      //  setMinimumDrawHeight(1);

        
    }
    
    public Dimension getMinimumSize() {
        return new Dimension(200,100);
    }
    
}
