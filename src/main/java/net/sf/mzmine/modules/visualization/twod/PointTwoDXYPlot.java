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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.twod;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;

import com.google.common.collect.Range;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.sf.mzmine.datamodel.DataPoint;


/**
 * This class is responsible for drawing the actual data points.
 * modified by Owen Myers 2017
 */
class PointTwoDXYPlot extends BaseXYPlot {


    PointTwoDXYPlot(TwoDDataSet dataset, Range<Double> rtRange,
	    Range<Double> mzRange, ValueAxis domainAxis, ValueAxis rangeAxis) {

		super(dataset, rtRange, mzRange, domainAxis, rangeAxis);

		this.dataset = dataset;

		totalRTRange = rtRange;
		totalMZRange = mzRange;

    }

    public boolean render(final Graphics2D g2, final Rectangle2D dataArea,
	    int index, PlotRenderingInfo info, CrosshairState crosshairState) {

	    // if this is not TwoDDataSet
	    if (index != 0)
	        return super.render(g2, dataArea, index, info, crosshairState);

	    // prepare some necessary constants
	    final int x = (int) dataArea.getX();
	    final int y = (int) dataArea.getY();
	    final int width = (int) dataArea.getWidth();
	    final int height = (int) dataArea.getHeight();

	    final double imageRTMin = (double) getDomainAxis().getRange()
	    	.getLowerBound();
	    final double imageRTMax = (double) getDomainAxis().getRange()
	    	.getUpperBound();
	    final double imageRTStep = (imageRTMax - imageRTMin) / width;
	    final double imageMZMin = (double) getRangeAxis().getRange()
	    	.getLowerBound();
	    final double imageMZMax = (double) getRangeAxis().getRange()
	    	.getUpperBound();
	    final double imageMZStep = (imageMZMax - imageMZMin) / height;

        // This if statement below keeps the min max values at the original values when the user
        // zooms in. We need some variables that scall as the box size so that the points we show
        // have better resolution the more someone zooms in.

        double dynamicImageRTMin = imageRTMin;
        double dynamicImageRTMax = imageRTMax;
        double dynamicImageMZMin = imageMZMin;
        double dynamicImageMZMax = imageMZMax;
        double dynamicImageRTStep = imageRTStep;
        double dynamicImageMZStep = imageMZStep;

	    if ((zoomOutBitmap != null)
	    	&& (imageRTMin == totalRTRange.lowerEndpoint())
	    	&& (imageRTMax == totalRTRange.upperEndpoint())
	    	&& (imageMZMin == totalMZRange.lowerEndpoint())
	    	&& (imageMZMax == totalMZRange.upperEndpoint())
	    	&& (zoomOutBitmap.getWidth() == width)
	    	&& (zoomOutBitmap.getHeight() == height)) {
	        g2.drawImage(zoomOutBitmap, x, y, null);
	        return true;
	    }


	    // Save current time
	    Date renderStartTime = new Date();


	    // prepare a bitmap of required size
        //BufferedImage image = new BufferedImage(width, height,
        //BufferedImage.TYPE_INT_ARGB);
        
        //ArrayList<DataPoint> listOfDataPoints = new ArrayList<DataPoint>();
        //ArrayList<DataPoint> listOfRTValues = new ArrayList<DataPoint>();
        ArrayList<DataPoint> listOfDataPoints;
        ArrayList<Double> listOfRTValues;

        // These two function must be run in this order
        Range<Double> rtRangeIn = Range.closed(getDomainAxis().getRange().getLowerBound(),getDomainAxis().getRange().getUpperBound());
        Range<Double> mzRangeIn = Range.closed(getRangeAxis().getRange().getLowerBound(),getRangeAxis().getRange().getUpperBound());
        listOfDataPoints = dataset.getCentroidedDataPointsInRTMZRange(rtRangeIn,mzRangeIn);
        listOfRTValues = dataset.getrtValuesInUserRange();

        // points to be plotted
        List plotPoints = new ArrayList();

        int count = 0;

        // Store the current mz,rt,int values so that they can be outputed to a file if we want
        //currentlyPlottedMZ = new ArrayList();

        //for (Iterator dpIt = listOfDataPoints.iterator(); dpIt.hasNext();) {

        // make a list to keep track of the intesities of each
        for (DataPoint curDataPoint : listOfDataPoints) {

            //DataPoint curDataPoint = dpIt.next();
            double currentRT = listOfRTValues.get(count).doubleValue();
            double currentMZ = curDataPoint.getMZ();

            plotPoints.add(new Point2D.Double(currentRT,currentMZ));

            count += 1; 

            //currentlyPlottedMZ.add()
        }


	    // draw image points
	    //for (int i = 0; i < width; i++)
	    //    for (int j = 0; j < height; j++) {
	    //	Color pointColor = paletteType.getColor(values[i][j]);
	    //	//image.setRGB(i, height - j - 1, pointColor.getRGB());
        //    points.add(new Point2D.Float(i,j));
	    //    }
        count = 0;
        for (Iterator i = plotPoints.iterator(); i.hasNext();) {
            Point2D.Double pt = (Point2D.Double) i.next();
            // using the ''dynamic'' min and max will make the resolution imporve as someone zooms
            // in 
            //float xPlace = (pt.x-(float)dynamicImageRTMin)/((float)dynamicImageRTStep) + x;
            //float yPlace = (pt.y-(float)dynamicImageMZMin)/((float)dynamicImageMZStep) + y;
            double xPlace = (pt.x-dynamicImageRTMin)/(dynamicImageRTStep)+(double)x;
            double yPlace = (double)height-(pt.y-dynamicImageMZMin)/(dynamicImageMZStep)+(double)y;

            //get the current intensity
            // use the R, G B for the intensity
            
            double curIntensity = listOfDataPoints.get(count).getIntensity();
            curIntensity = curIntensity / dataset.curMaxIntensity;

            //g2.setColor(Color.BLACK);
            Color pointColor = paletteType.getColor(curIntensity);
            g2.setColor(pointColor );
			Ellipse2D dot = new Ellipse2D.Double(xPlace - 2, yPlace - 2, 5, 5);
            g2.fill(dot);

            count += 1;
        }

            //float xPlace = ((float)42.0-(float)dynamicImageRTMin)/((float)dynamicImageRTStep)+x;
            //float yPlace = (float)height - ((float)201.02-(float)dynamicImageMZMin)/((float)dynamicImageMZStep)+y;
            //Ellipse2D dot = new Ellipse2D.Float(xPlace - 1, yPlace - 1, 2, 2);
            //g2.fill(dot);
        
        //g2.dispose();

	    // if we are zoomed out, save the values
	    if ((imageRTMin == totalRTRange.lowerEndpoint())
	    	&& (imageRTMax == totalRTRange.upperEndpoint())
	    	&& (imageMZMin == totalMZRange.lowerEndpoint())
	    	&& (imageMZMax == totalMZRange.upperEndpoint())) {
	        //zoomOutBitmap = image;
	    }

	    // Paint image
	    //g2.drawImage(image, x, y, null);
        
        //g.setColor(Color.BLACK)

	    Date renderFinishTime = new Date();

	    logger.finest("Finished rendering 2D visualizer, "
	    	+ (renderFinishTime.getTime() - renderStartTime.getTime())
	    	+ " ms");



	    return true;

    }
}
