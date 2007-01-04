/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.neutralloss;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * 
 */
class NeutralLossDataPointRenderer extends XYLineAndShapeRenderer {

    // data points shape
    private static final Shape dataPointsShape = new Ellipse2D.Float(-1, -1, 2,
            2);

    private NeutralLossPlot nlPlot;
    
    NeutralLossDataPointRenderer(NeutralLossPlot nlPlot) {
        super(false, true);
        this.nlPlot = nlPlot;

        setShape(dataPointsShape);

    }

    public void drawItem(java.awt.Graphics2D g2, XYItemRendererState state,
            Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
            ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
            int series, int item, CrosshairState crosshairState, int pass) {

        NeutralLossDataSet nlDataset = (NeutralLossDataSet) dataset;


        NeutralLossDataPoint point = nlDataset.getDataPoint(item);

        if ((point.getPrecursorMZ() < nlPlot.getHighlightedMin())
                || (point.getPrecursorMZ() > nlPlot.getHighlightedMax()))
            setPaint(Color.blue);
        else
            setPaint(Color.red);

        super.drawItem(g2, state, dataArea, info, plot, domainAxis, rangeAxis,
                dataset, series, item, crosshairState, pass);

    }

}
