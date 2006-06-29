package net.sf.mzmine.visualizers.rawdata.twod;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

/**
 * This class is responsible for drawing the actual data points.
 */
class TwoDXYPlot extends XYPlot {

    TwoDDataSet dataset;

    TwoDXYPlot(XYDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis,
            XYItemRenderer renderer) {
        super(dataset, domainAxis, rangeAxis, renderer);
        this.dataset = (TwoDDataSet) dataset;
    }

    public boolean render(Graphics2D g2, Rectangle2D area, int index,
            PlotRenderingInfo info, CrosshairState crosshairState) {

        BufferedImage image = dataset.getRenderedImage();

        if (image != null) {

            AffineTransform transform = AffineTransform.getTranslateInstance(
                    area.getX(), area.getY());

            AffineTransform scaleTransform = AffineTransform.getScaleInstance(
                    area.getWidth() / image.getWidth(), 
                    area.getHeight() / image.getHeight());

            transform.concatenate(scaleTransform);

            g2.drawRenderedImage(image, transform);
        }
        
        return image != null;

    }

}