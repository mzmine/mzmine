/**
 * 
 */
package net.sf.mzmine.visualizers.rawdata.twod;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;

import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.PlotState;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleInsets;

/**
 * 
 */
class TwoDXYPlot extends XYPlot {

    TwoDDataSet dataset;

    TwoDXYPlot(XYDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis,
            XYItemRenderer renderer) {
        super(dataset, domainAxis, rangeAxis, renderer);
        this.dataset = (TwoDDataSet) dataset;
    }

    /**
     * Draws the plot within the specified area on a graphics device.
     * 
     * @param g2
     *            the graphics device.
     * @param area
     *            the plot area (in Java2D space).
     * @param anchor
     *            an anchor point in Java2D space (<code>null</code>
     *            permitted).
     * @param parentState
     *            the state from the parent plot, if there is one (<code>null</code>
     *            permitted).
     * @param info
     *            collects chart drawing information (<code>null</code>
     *            permitted).
     */
/*    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
            PlotState parentState, PlotRenderingInfo info) {

        // if the plot area is too small, just return...
        boolean b1 = (area.getWidth() <= MINIMUM_WIDTH_TO_DRAW);
        boolean b2 = (area.getHeight() <= MINIMUM_HEIGHT_TO_DRAW);
        if (b1 || b2) {
            return;
        }

        // record the plot area...
        if (info != null) {
            info.setPlotArea(area);
        }

        BufferedImage bi = dataset.getRenderedImage();

        if (bi != null) {

            AffineTransform tr = AffineTransform.getTranslateInstance(area
                    .getX(), area.getY());

            tr.concatenate(AffineTransform.getScaleInstance(area.getWidth()
                    / bi.getWidth(), area.getHeight() / bi.getHeight()));


            g2.drawRenderedImage(bi, tr);

        }

        // adjust the drawing area for the plot insets (if any)...
        RectangleInsets insets = getInsets();
        insets.trim(area);

        AxisSpace space = calculateAxisSpace(g2, area);
        Rectangle2D dataArea = space.shrink(area, null);
        super.axisOffset.trim(dataArea);

        if (info != null) {
            info.setDataArea(dataArea);
        }

        // draw the plot background and axes...
        
        Map axisStateMap = drawAxes(g2, area, dataArea, info);

        if (anchor != null && !dataArea.contains(anchor)) {
            anchor = null;
        }
        CrosshairState crosshairState = new CrosshairState();
        crosshairState.setCrosshairDistance(Double.POSITIVE_INFINITY);
        crosshairState.setAnchor(anchor);
        crosshairState.setCrosshairX(getDomainCrosshairValue());
        crosshairState.setCrosshairY(getRangeCrosshairValue());
        Shape originalClip = g2.getClip();
        Composite originalComposite = g2.getComposite();

        g2.clip(dataArea);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                getForegroundAlpha()));

        AxisState domainAxisState = (AxisState) axisStateMap
                .get(getDomainAxis());
        if (domainAxisState == null) {
            if (parentState != null) {
                domainAxisState = (AxisState) parentState.getSharedAxisStates()
                        .get(getDomainAxis());
            }
        }

        AxisState rangeAxisState = (AxisState) axisStateMap.get(getRangeAxis());
        if (rangeAxisState == null) {
            if (parentState != null) {
                rangeAxisState = (AxisState) parentState.getSharedAxisStates()
                        .get(getRangeAxis());
            }
        }
        if (domainAxisState != null) {
            drawDomainTickBands(g2, dataArea, domainAxisState.getTicks());
        }
        if (rangeAxisState != null) {
            drawRangeTickBands(g2, dataArea, rangeAxisState.getTicks());
        }
        if (domainAxisState != null) {
            drawDomainGridlines(g2, dataArea, domainAxisState.getTicks());
        }
        if (rangeAxisState != null) {
            drawRangeGridlines(g2, dataArea, rangeAxisState.getTicks());
            drawZeroRangeBaseline(g2, dataArea);
        }

        // draw the markers that are associated with a specific renderer...
        for (int i = 0; i < this.renderers.size(); i++) {
            drawDomainMarkers(g2, dataArea, i, Layer.BACKGROUND);
        }
        for (int i = 0; i < this.renderers.size(); i++) {
            drawRangeMarkers(g2, dataArea, i, Layer.BACKGROUND);
        }

        // now draw annotations and render data items...
        boolean foundData = false;
        DatasetRenderingOrder order = getDatasetRenderingOrder();
        if (order == DatasetRenderingOrder.FORWARD) {

            // draw background annotations
            int rendererCount = this.renderers.size();
            for (int i = 0; i < rendererCount; i++) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.BACKGROUND, info);
                }
            }

            // render data items...
            for (int i = 0; i < getDatasetCount(); i++) {
                foundData = render(g2, dataArea, i, info, crosshairState)
                        || foundData;
            }

            // draw foreground annotations
            for (int i = 0; i < rendererCount; i++) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.FOREGROUND, info);
                }
            }

        } else if (order == DatasetRenderingOrder.REVERSE) {

            // draw background annotations
            int rendererCount = this.renderers.size();
            for (int i = rendererCount - 1; i >= 0; i--) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.BACKGROUND, info);
                }
            }

            for (int i = getDatasetCount() - 1; i >= 0; i--) {
                foundData = render(g2, dataArea, i, info, crosshairState)
                        || foundData;
            }

            // draw foreground annotations
            for (int i = rendererCount - 1; i >= 0; i--) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                            Layer.FOREGROUND, info);
                }
            }

        }

        PlotOrientation orient = getOrientation();

        // draw domain crosshair if required...
        if (!this.domainCrosshairLockedOnData && anchor != null) {
            double xx = getDomainAxis().java2DToValue(anchor.getX(), dataArea,
                    getDomainAxisEdge());
            crosshairState.setCrosshairX(xx);
        }
        setDomainCrosshairValue(crosshairState.getCrosshairX(), false);
        if (isDomainCrosshairVisible()) {
            double x = getDomainCrosshairValue();
            Paint paint = getDomainCrosshairPaint();
            Stroke stroke = getDomainCrosshairStroke();
            if (orient == PlotOrientation.HORIZONTAL) {
                drawHorizontalLine(g2, dataArea, x, stroke, paint);
            } else if (orient == PlotOrientation.VERTICAL) {
                drawVerticalLine(g2, dataArea, x, stroke, paint);
            }
        }

        // draw range crosshair if required...
        if (!this.rangeCrosshairLockedOnData && anchor != null) {
            double yy = getRangeAxis().java2DToValue(anchor.getY(), dataArea,
                    getRangeAxisEdge());
            crosshairState.setCrosshairY(yy);
        }
        setRangeCrosshairValue(crosshairState.getCrosshairY(), false);
        if (isRangeCrosshairVisible()
                && getRangeAxis().getRange().contains(getRangeCrosshairValue())) {
            double y = getRangeCrosshairValue();
            Paint paint = getRangeCrosshairPaint();
            Stroke stroke = getRangeCrosshairStroke();
            if (orient == PlotOrientation.HORIZONTAL) {
                drawVerticalLine(g2, dataArea, y, stroke, paint);
            } else if (orient == PlotOrientation.VERTICAL) {
                drawHorizontalLine(g2, dataArea, y, stroke, paint);
            }
        }

        if (!foundData) {
            drawNoDataMessage(g2, dataArea);
        }

        for (int i = 0; i < this.renderers.size(); i++) {
            drawDomainMarkers(g2, dataArea, i, Layer.FOREGROUND);
        }
        for (int i = 0; i < this.renderers.size(); i++) {
            drawRangeMarkers(g2, dataArea, i, Layer.FOREGROUND);
        }

        drawAnnotations(g2, dataArea, info);
        g2.setClip(originalClip);
        g2.setComposite(originalComposite);

        drawOutline(g2, dataArea);

    }
*/
}
