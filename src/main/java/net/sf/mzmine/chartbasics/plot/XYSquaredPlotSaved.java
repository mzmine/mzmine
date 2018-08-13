package net.sf.mzmine.chartbasics.plot;

/**
*class SquaredXYPlot.java
*by Erica Liszewski
*December 20, 2005
*
*An extension to the XYPlot provided in JFreeChart, this class draws
*XY plots where the spacing between two given values is the same on
*both the X and Y axis.  This class over-rides draw() in XYPlot to add it's
*functionality.
*/


import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.xy.XYDataset;

/**
 * This plot keeps one axis at static range and scales 
 * the opposite range in a way that both axis have the same value-to-java2d dimensions.
 * A box from 0;0 to 1;1 will be a perfect square.
 * For graphics export: Supply a ChartRenderingInfo to the JFreeChart.draw method
 * @author Robin Schmid
 *
 */
public class XYSquaredPlotSaved extends XYPlot {
    
    protected boolean squareToRange;
    
    /*public SquaredXYPlot()
     *default constructor
     */
    public XYSquaredPlotSaved(){
        super();
        squareToRange = true;
    }
    
    /*public SquaredXYPlot(XYDataset dataset,
            ValueAxis domainAxis,
            ValueAxis rangeAxis,
            XYItemRenderer renderer)
     */
    public XYSquaredPlotSaved(XYDataset dataset,
            ValueAxis domainAxis,
            ValueAxis rangeAxis,
            XYItemRenderer renderer) {
        super(dataset, domainAxis, rangeAxis, renderer);
        squareToRange = true;
    }
    
    /*public void setSquaredToRange(boolean squareToRange)
     *determines which axis is changed so the axis end up squared
     *
     *squareToRange is true if the y-axis is left alone and the
     *     x-axis is adjusted to match
     *squareToRange is false if the x-axis is left alone and the
     *    y-axis is adjusted
     */
    public void setSquaredToRange(boolean squareToRange){
        this.squareToRange = squareToRange;
    }
    
    /*public boolean getSquaredToRange()
     *returns the squareToRange
     *see setSquaredToRange()
     */
    public boolean getSquaredToRange(){
        return this.squareToRange;
    }
    
    // needs a PlotRenderingInfo which is usually provided by the CHartRenderingInfo
    @Override
    public void draw(Graphics2D g2,
            Rectangle2D area,
            Point2D anchor,
            PlotState parentState,
            PlotRenderingInfo info) {
//    	if(info==null || info.getDataArea()==null)
//    		super.draw(g2, area, anchor, parentState, info);
    	
    	 // if the plot area is too small, just return...
    	Rectangle2D savedarea = (Rectangle2D) area.clone();
        boolean b1 = (area.getWidth() <= MINIMUM_WIDTH_TO_DRAW);
        boolean b2 = (area.getHeight() <= MINIMUM_HEIGHT_TO_DRAW);
        if (b1 || b2) {
            return;
        }

        System.out.println("DRAW previous");
        printInfo(info);
        // record the plot area...
        if (info != null) {
            info.setPlotArea(area);
        }

        // adjust the drawing area for the plot insets (if any)...
        RectangleInsets insets = getInsets();
        insets.trim(area);

        AxisSpace space = calculateAxisSpace(g2, area);
        Rectangle2D dataArea = space.shrink(area, null);
        this.getAxisOffset().trim(dataArea);

        dataArea = integerise(dataArea);
        if (dataArea.isEmpty()) {
            return;
        }
        createAndAddEntity((Rectangle2D) dataArea.clone(), info, null, null);
        if (info != null) {
            info.setDataArea(dataArea);
        }
        System.out.println("DRAW data area calc");
        printInfo(info);
        
        // end of calculation
        area = savedarea;

        ValueAxis adjaxis = getRangeAxis();
        ValueAxis stataxis = getDomainAxis();
        double ratio = dataArea.getHeight()/dataArea.getWidth();
        //if the range is being adjusted
        if(this.squareToRange){
        	stataxis = getRangeAxis();
            adjaxis = getDomainAxis();
            
            ratio = 1/ratio;
        }
        
        double statrange = stataxis.getRange().getLength();
        double adjrange = statrange*ratio;
        double lower = adjaxis.getLowerBound();
        double lastupper = adjaxis.getUpperBound();
        
        if(lower+adjrange!=lastupper)
        	adjaxis.setRange(lower, lower+adjrange);
        
        double ratio2 = adjaxis.getRange().getLength()/statrange;
        System.out.println(ratio + "  "+ ratio2);
        //call the original draw method to handle things from here
        super.draw(g2, area, anchor, parentState, info);

        System.out.println("DRAW done");
        printInfo(info);
        System.out.println();
    }
    
    
    private void printInfo(PlotRenderingInfo info) {
    	if(info==null)
    		return;
    	if(info.getOwner()!=null)
    		System.out.println("Chart area "+info.getOwner().getChartArea().toString());
    	if(info.getPlotArea()!=null)
    	System.out.println("Plot area "+info.getPlotArea().toString());
    	if(info.getDataArea()!=null)
    	System.out.println("Data area "+info.getDataArea().toString());
	}

	/**
     * Trims a rectangle to integer coordinates.
     *
     * @param rect  the incoming rectangle.
     *
     * @return A rectangle with integer coordinates.
     */
    private Rectangle integerise(Rectangle2D rect) {
        int x0 = (int) Math.ceil(rect.getMinX());
        int y0 = (int) Math.ceil(rect.getMinY());
        int x1 = (int) Math.floor(rect.getMaxX());
        int y1 = (int) Math.floor(rect.getMaxY());
        return new Rectangle(x0, y0, (x1 - x0), (y1 - y0));
    }
}