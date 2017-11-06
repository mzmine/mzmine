/* 
 * Copyright (C) 2017 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV1_5;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Paint;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author Du-Lab Team <dulab.binf@gmail.com>
 */


public class EICPlot extends ChartPanel 
{
    private final XYSeriesCollection xyDataset;
    private final List <Double> colorDataset;
    private final List <String> toolTips;
    
    public EICPlot()
    {
        this(new ArrayList <List <NavigableMap <Double, Double>>> (), 
                new ArrayList <Double> (), 
                new ArrayList <List <String>> (),
                null);
    }
    
    public EICPlot(List <List <NavigableMap <Double, Double>>> clusters,
            List <Double> colors,
            List <List <String>> info,
            List <NavigableMap <Double, Double>> modelPeaks)
    {
        super(null, true);
        
        setBackground(Color.white);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        
        NumberAxis xAxis = new NumberAxis("Retention Time");
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setUpperMargin(0);
        xAxis.setLowerMargin(0);
        
        NumberAxis yAxis = new NumberAxis("Intensity");
        yAxis.setAutoRangeIncludesZero(false);
        yAxis.setUpperMargin(0);
        yAxis.setLowerMargin(0);
        
        xyDataset = new XYSeriesCollection();
        colorDataset = new ArrayList <> ();
        toolTips = new ArrayList <> ();
        
        int seriesID = 0;
        
        for (int i = 0; i < clusters.size(); ++i)
        {
            List <NavigableMap <Double, Double>> cluster = clusters.get(i);
            double color = colors.get(i);
            
            for (int j = 0; j < cluster.size(); ++j) 
            {   
                XYSeries series = new XYSeries(seriesID++);
                
                for (Entry <Double, Double> e : cluster.get(j).entrySet())
                    series.add(e.getKey(), e.getValue());
                
                xyDataset.addSeries(series);
                colorDataset.add(color);
                toolTips.add(info.get(i).get(j));
            }
        }
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer() {
            @Override
            public Paint getItemPaint(int row, int col) {
                double c = colorDataset.get(row);
                return Color.getHSBColor((float) c, 1.0f, 1.0f);
            }
        };
        
        renderer.setDefaultShapesVisible(false);
        renderer.setDefaultToolTipGenerator(new XYToolTipGenerator() {
            @Override
            public String generateToolTip(XYDataset dataset, int series, int item)
            {
                try {
                    return toolTips.get(series);
                } catch (NullPointerException | IndexOutOfBoundsException e) {
                    return "";
                }
            }
        });
        
        XYPlot plot = new XYPlot(xyDataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinesVisible(true);
        
        JFreeChart chart = new JFreeChart("", 
                new Font("SansSerif", Font.BOLD, 12),
                plot, 
                false);
        chart.setBackgroundPaint(Color.white);
        
        super.setChart(chart);
    }
    
    
    
    public void updateData(List <List <NavigableMap <Double, Double>>> clusters,
            List <Double> colors,
            List <List <String>> info,
            List <NavigableMap <Double, Double>> modelPeaks)
    {
//        for (int i = 0; i < xyDataset.getSeriesCount(); ++i)
//            xyDataset.removeSeries(i);
        xyDataset.removeAllSeries();
        colorDataset.clear();
        toolTips.clear();
        
        int seriesID = 0;
        
        for (int i = 0; i < clusters.size(); ++i)
        {
            List <NavigableMap <Double, Double>> cluster = clusters.get(i);
            double color = colors.get(i);
            
            for (int j = 0; j < cluster.size(); ++j) 
            {                
                XYSeries series = new XYSeries(seriesID++);
                
                for (Entry <Double, Double> e : cluster.get(j).entrySet())
                    series.add(e.getKey(), e.getValue());
                
                xyDataset.addSeries(series);
                colorDataset.add(color);
                toolTips.add(info.get(i).get(j));
            }
        }
    }
}
