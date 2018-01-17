/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
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

package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import dulab.adap.datamodel.BetterComponent;
import dulab.adap.datamodel.BetterPeak;
import dulab.adap.datamodel.Component;
import dulab.adap.workflow.decomposition.ComponentSelector;
import dulab.adap.workflow.decomposition.RetTimeClusterer;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.util.GUIUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Du-Lab Team dulab.binf@gmail.com
 */
public class ComponentConstructionSupplier extends AlgorithmSupplier
{
    private static final String NAME = "Component Construction";

    private static final DoubleParameter RT_TOLERANCE = new DoubleParameter(NAME + ": Ret Time Tolerance",
            "Retention-time tolerance specifies when two peaks are considered to belong to one component",
            NumberFormat.getNumberInstance(), 0.5, 0.0, 1.0);

    private static final Parameter[] PARAMETERS = new Parameter[] {RT_TOLERANCE};

    private final JComboBox<RetTimeClusterer.Cluster> cboClusters;
    private final JPanel pnlPlotXY;
    private final Plot plot;

    ComponentConstructionSupplier()
    {
        cboClusters = new JComboBox<>();
        cboClusters.setVisible(true);
        cboClusters.addActionListener(this);

        JPanel pnlLabelFields = new JPanel(new BorderLayout());
        pnlLabelFields.add(new JLabel("Window"), BorderLayout.WEST);
        pnlLabelFields.add(cboClusters, BorderLayout.CENTER);
        pnlLabelFields.setOpaque(false);

        plot = new Plot();
        pnlPlotXY = new JPanel(new BorderLayout());
        pnlPlotXY.setBackground(Color.white);
        pnlPlotXY.add(pnlLabelFields, BorderLayout.NORTH);
        pnlPlotXY.add(plot, BorderLayout.CENTER);
        GUIUtils.addMarginAndBorder(pnlPlotXY, 10);
    }

    @Override
    public String getName() {return NAME;}

    @Override
    public Parameter[] getParameters() {return PARAMETERS;}

    @Override
    public JPanel getPanel() {return pnlPlotXY;}

    @Override
    public void actionPerformed(ActionEvent ae) {}

    @Override
    public void updateData(@Nonnull DataProvider dataProvider)
    {
        super.updateData(dataProvider);

        List<RetTimeClusterer.Cluster> clusters = dataProvider.getWindows(true);

        cboClusters.removeActionListener(this);
        cboClusters.removeAllItems();
        for (RetTimeClusterer.Cluster c : clusters)
            cboClusters.addItem(c);
        cboClusters.addActionListener(this);

        if (cboClusters.getItemCount() > 0)
            cboClusters.setSelectedIndex(0);
    }

    private void constructComponents(@Nonnull RetTimeClusterer.Cluster cluster, @Nonnull DataProvider dataProvider)
    {
        if (parameters == null) return;

        final Double retTimeTolerance = parameters.getParameter(RT_TOLERANCE).getValue();

        if (retTimeTolerance == null) return;

        List<BetterComponent> components =
                new ComponentSelector(cluster, dataProvider.getChromatograms(true), retTimeTolerance).run();


    }


    private static class Plot extends ChartPanel
    {
        private static final Color[] COLORS = new Color[] {
                Color.BLUE, Color.PINK, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.GREEN, Color.RED};

        private enum PeakType {SIMPLE, MODEL};

        private final XYSeriesCollection xyDataset;
        private final List<Integer> colorDataset;
        private final List<String> toolTips;
        private final List<Double> widths;

        Plot()
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
            colorDataset = new ArrayList<>();
            toolTips = new ArrayList <> ();
            widths = new ArrayList<>();

//            int seriesID = 0;
//
//            for (int i = 0; i < clusters.size(); ++i)
//            {
//                List <NavigableMap <Double, Double>> cluster = clusters.get(i);
//                double color = colors.get(i);
//
//                for (int j = 0; j < cluster.size(); ++j)
//                {
//                    XYSeries series = new XYSeries(seriesID++);
//
//                    for (Map.Entry<Double, Double> e : cluster.get(j).entrySet())
//                        series.add(e.getKey(), e.getValue());
//
//                    xyDataset.addSeries(series);
//                    colorDataset.add(color);
//                    toolTips.add(info.get(i).get(j));
//                }
//            }

            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer() {
                @Override
                public Paint getItemPaint(int row, int col)
                {
                    String type = xyDataset.getSeries(row).getDescription();

                    Paint color;

                    if (type.equals(Plot.PeakType.MODEL.name()))
                        color = COLORS[row % COLORS.length];
                    else
                        color = new Color(0,0,0,50);

                    return color;
                }

                @Override
                public Stroke getSeriesStroke(int series)
                {
                    XYSeries s = xyDataset.getSeries(series);
                    String type = s.getDescription();

                    float width;
                    if (type.equals((Plot.PeakType.MODEL.name())))
                        width = 2.0f;
                    else
                        width = 1.0f;

                    return new BasicStroke(width);
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

        void updateData(@Nonnull List<BetterPeak> peaks,
                        @Nonnull List<BetterComponent> components)
        {
            xyDataset.removeAllSeries();
            xyDataset.setNotify(false);
            toolTips.clear();

            double startRetTime = components.stream().mapToDouble(BetterPeak::getFirstRetTime).min().orElse(0.0);
            double endRetTime = components.stream().mapToDouble(BetterPeak::getLastRetTime).max().orElse(0.0);
            if (endRetTime <= startRetTime) return;

            int seriesID = 0;
            for (BetterPeak peak : peaks)
            {
                XYSeries series = new XYSeries(seriesID++);
                series.setDescription(Plot.PeakType.SIMPLE.name());

                for (int i = 0; i < peak.chromatogram.length; ++i) {
                    double retTime = peak.chromatogram.getRetTime(i);
                    if (startRetTime <= retTime && retTime <= endRetTime)
                        series.add(peak.chromatogram.getRetTime(i), peak.chromatogram.getIntensity(i));
                }

                xyDataset.addSeries(series);
                toolTips.add(String.format("M/z: %.2f\nIntensity: %.0f",
                        peak.getMZ(), peak.getIntensity()));
            }

            for (BetterPeak peak : components)
            {
                XYSeries series = new XYSeries((seriesID++));
                series.setDescription(Plot.PeakType.MODEL.name());

                for (int i = 0; i < peak.chromatogram.length; ++i)
                    series.add(peak.chromatogram.getRetTime(i), peak.chromatogram.getIntensity(i));

                xyDataset.addSeries(series);
                toolTips.add(String.format("Model peak\nM/z: %.2f\nIntensity: %.0f",
                        peak.getMZ(), peak.getIntensity()));
            }

            xyDataset.setNotify(true);
        }
    }
}
