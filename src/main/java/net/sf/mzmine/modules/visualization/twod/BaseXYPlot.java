/*
 * Copyright (c) 2017 The du-lab Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.twod;

import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetChangeEvent;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by owen myers (oweenm@gmail.com) on 4/5/17.
 */

public class BaseXYPlot extends XYPlot{
    private static final long serialVersionUID = 1L;

    public Logger logger = Logger.getLogger(this.getClass().getName());

    public Range<Double> totalRTRange, totalMZRange;
    public BufferedImage zoomOutBitmap;

    public TwoDDataSet dataset;

    public TwoDPaletteType paletteType = TwoDPaletteType.PALETTE_RAINBOW;

    public PlotMode plotMode = PlotMode.UNDEFINED;

    public boolean logScale;
    public double maxValue = 0;


    BaseXYPlot(TwoDDataSet dataset, Range<Double> rtRange,
                    Range<Double> mzRange, ValueAxis domainAxis, ValueAxis rangeAxis) {

        super(dataset, domainAxis, rangeAxis, null);

        this.dataset = dataset;

        totalRTRange = rtRange;
        totalMZRange = mzRange;

    }

    public boolean render(final Graphics2D g2, final Rectangle2D dataArea,
                          int index, PlotRenderingInfo info, CrosshairState crosshairState) {
        return true;

    }

    Range<Double> getDomainRange() {
        return Range.closed(getDomainAxis().getRange().getLowerBound(),
                getDomainAxis().getRange().getUpperBound());
    }

    Range<Double> getAxisRange() {
        return Range.closed(getRangeAxis().getRange().getLowerBound(),
                getRangeAxis().getRange().getUpperBound());
    }

    void switchPalette() {
        TwoDPaletteType types[] = TwoDPaletteType.values();
        int newIndex = paletteType.ordinal() + 1;
        if (newIndex >= types.length)
            newIndex = 0;
        paletteType = types[newIndex];
        zoomOutBitmap = null;
        datasetChanged(new DatasetChangeEvent(dataset, dataset));
    }

    PlotMode getPlotMode() {
        return plotMode;
    }

    void setPlotMode(PlotMode plotMode) {
        this.plotMode = plotMode;

        // clear the zoom out image cache
        zoomOutBitmap = null;

        datasetChanged(new DatasetChangeEvent(dataset, dataset));
    }

    void setLogScale(boolean logscale) {
        logScale = logscale;

        // clear the zoom out image cache
        zoomOutBitmap = null;

        datasetChanged(new DatasetChangeEvent(dataset, dataset));
    }

}
