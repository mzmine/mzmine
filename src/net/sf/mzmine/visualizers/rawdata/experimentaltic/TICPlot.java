/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.experimentaltic;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.ui.RectangleInsets;

import com.sun.org.apache.xerces.internal.impl.dv.dtd.NMTOKENDatatypeValidator;

/**
 * 
 */
class TICPlot extends ChartPanel {

    private JFreeChart chart;

    private XYPlot plot;

    private DateFormat rtFormat; // TODO
    private NumberFormat intensityFormat;

    private TICVisualizer visualizer;

    private int numberOfDataSets = 0;

    private static Color[] plotColors = { Color.blue, Color.red, Color.green,
            Color.magenta, Color.cyan, Color.orange };

    private LegendTitle legend;

    /**
     * @param chart
     */
    TICPlot(final TICVisualizer visualizer) {
        // superconstructor with no chart yet, but enable off-screen buffering
        super(null, true);

        this.visualizer = visualizer;

        chart = ChartFactory.createTimeSeriesChart(null, // title
                "Retention time", // x-axis label
                "Intensity", // y-axis label
                null, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        chart.setBackgroundPaint(Color.white);
        setChart(chart);

        // the legend was constructed by ChartFactory, we can save it for later
        legend = chart.getLegend();
        chart.removeLegend();

        plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));

        rtFormat = new SimpleDateFormat("m:ss");
        intensityFormat = new DecimalFormat("0.00E0");

        DateAxis xAxis = (DateAxis) plot.getDomainAxis();
        xAxis.setDateFormatOverride(rtFormat);
        xAxis.setUpperMargin(0.001);
        xAxis.setLowerMargin(0.001);
        plot.setDomainAxis(xAxis);

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setNumberFormatOverride(intensityFormat);

        // plot.setRangeCrosshairLockedOnData(false);
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setDomainCrosshairPaint(Color.darkGray);
        plot.setRangeCrosshairPaint(Color.darkGray);
        BasicStroke crossHairStroke = new BasicStroke(1, BasicStroke.CAP_BUTT,
                BasicStroke.JOIN_BEVEL, 1.0f, new float[] { 3, 3 }, 0);
        plot.setDomainCrosshairStroke(crossHairStroke);
        plot.setRangeCrosshairStroke(crossHairStroke);

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot
                .getRenderer();
        renderer.setPaint(Color.blue);
        renderer.setBaseShapesFilled(true);
        renderer.setDrawOutlines(false);
        renderer.setUseFillPaint(true);
        renderer.setBaseFillPaint(Color.red);

        XYItemLabelGenerator labelGenerator = new TICItemLabelGenerator(this);
        renderer.setItemLabelGenerator(labelGenerator);
        renderer.setItemLabelsVisible(true);

        // to receive key events
        setFocusable(true);

        getInputMap().put(KeyStroke.getKeyStroke("RIGHT"), "moveCursorRight");
        getActionMap().put("moveCursorRight", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                double selectedRT = plot.getDomainCrosshairValue();
                double selectedIT = plot.getRangeCrosshairValue();
                for (int i = 0; i < numberOfDataSets; i++) {
                    RawDataFileDataSet dataSet = (RawDataFileDataSet) plot
                            .getDataset(i);
                    if (dataSet == null)
                        continue;
                    int index = dataSet.getSeriesIndex(selectedRT, selectedIT);
                    if (index >= 0) {
                        index++;
                        if (index < dataSet.getItemCount(0)) {
                            plot.setDomainCrosshairValue(dataSet.getXValue(0,
                                    index));
                            plot.setRangeCrosshairValue(dataSet.getYValue(0,
                                    index));
                        }
                        break;
                    }
                }

            }
        });
        getInputMap().put(KeyStroke.getKeyStroke("LEFT"), "moveCursorLeft");
        getActionMap().put("moveCursorLeft", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                double selectedRT = plot.getDomainCrosshairValue();
                double selectedIT = plot.getRangeCrosshairValue();
                for (int i = 0; i < numberOfDataSets; i++) {
                    RawDataFileDataSet dataSet = (RawDataFileDataSet) plot
                            .getDataset(i);
                    if (dataSet == null)
                        continue;
                    int index = dataSet.getSeriesIndex(selectedRT, selectedIT);
                    if (index >= 0) {
                        index--;
                        if (index > 0) {
                            plot.setDomainCrosshairValue(dataSet.getXValue(0,
                                    index));
                            plot.setRangeCrosshairValue(dataSet.getYValue(0,
                                    index));
                        }
                        break;
                    }
                }
            }
        });

        setMaximumDrawWidth(Integer.MAX_VALUE);
        setMaximumDrawHeight(Integer.MAX_VALUE);

        getPopupMenu().addSeparator();
        getPopupMenu().add(new AddFilePopupMenu(visualizer));
        getPopupMenu().add(new RemoveFilePopupMenu(visualizer));

    }

    /**
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent event) {
        super.mouseClicked(event);

        requestFocus();

        /*
         * if ((event.getButton() == MouseEvent.BUTTON1) &&
         * (event.getClickCount() == 2)) { for (int j = 0; j <
         * dataset.getSeriesCount(); j++) { XYSeries series =
         * dataset.getSeries(j);
         * 
         * for (int i = 0; i < series.getItemCount(); i++) { double val =
         * dataset.getXValue(0, i); if (val == plot.getDomainCrosshairValue()) {
         * SpectrumVisualizer specVis = new SpectrumVisualizer(
         * visualizer.getRawDataFile(), visualizer .getScanNumbes()[i]);
         * MainWindow.getInstance().addInternalFrame(specVis); return; } } } }
         */
    }

    public void chartProgress(ChartProgressEvent event) {
        super.chartProgress(event);
        if (event.getType() == ChartProgressEvent.DRAWING_FINISHED)
            visualizer.updateTitle();
    }

    void switchItemLabelsVisible() {

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot
                .getRenderer();
        ;
        boolean itemLabelsVisible = renderer.isSeriesItemLabelsVisible(0);
        for (int i = 0; i < numberOfDataSets; i++) {
            renderer = (XYLineAndShapeRenderer) plot.getRenderer(i);
            if (renderer != null) {
                renderer.setItemLabelsVisible(!itemLabelsVisible);
            }
        }
    }

    void switchDataPointsVisible() {

        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot
                .getRenderer();
        boolean dataPointsVisible = renderer.getBaseShapesVisible();
        for (int i = 0; i < numberOfDataSets; i++) {
            renderer = (XYLineAndShapeRenderer) plot.getRenderer(i);
            if (renderer != null) {
                renderer.setBaseShapesVisible(!dataPointsVisible);
            }
        }
    }

    XYPlot getPlot() {
        return plot;
    }

    synchronized void addDataset(RawDataFileDataSet newSet) {

        plot.setDataset(numberOfDataSets, newSet);

        try {
            XYLineAndShapeRenderer newRenderer;
            newRenderer = (XYLineAndShapeRenderer) ((XYLineAndShapeRenderer) plot
                    .getRenderer()).clone();
            newRenderer.setPaint(plotColors[numberOfDataSets
                    % plotColors.length]);
            plot.setRenderer(numberOfDataSets, newRenderer);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        numberOfDataSets++;

    }

    void showLegend(boolean show) {
        chart.removeLegend();
        if (show) {
            chart.addLegend(legend);
        }
    }
}
