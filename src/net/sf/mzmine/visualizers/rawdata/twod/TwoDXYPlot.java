/**
 * 
 */
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
 * 
 */
class TwoDXYPlot extends XYPlot {

    TwoDDataSet dataset;

    TwoDXYPlot(XYDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis, XYItemRenderer renderer) { 
        super(dataset, domainAxis, rangeAxis, renderer);
        this.dataset = (TwoDDataSet) dataset;
    }
    
    public boolean render(Graphics2D g2, Rectangle2D area, int index, PlotRenderingInfo info, CrosshairState crosshairState)  {

        BufferedImage bi = dataset.getRenderedImage();
        
        
        if (bi != null) {
            AffineTransform tr =AffineTransform.getTranslateInstance(area.getX(), area.getY());
            
            tr.concatenate( AffineTransform.getScaleInstance(area
                    .getWidth()
                    / bi.getWidth(), area.getHeight() / bi.getHeight()));
                
            // tr.concatenate(AffineTransform.getTranslateInstance(area.getX(), area.getY()));
            //System.out.println("rendering to " + area);    
        
            g2.drawRenderedImage(bi, tr);
        }
        return bi != null;

    }
    
}
