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

package net.sf.mzmine.modules.visualization.ida;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;

import com.google.common.collect.Range;

/**
 * This class is responsible for drawing the actual data points.
 */
class IDAXYPlot extends XYPlot {

    private static final long serialVersionUID = 1L;

    IDAXYPlot(IDADataSet dataset, Range<Double> rtRange,
	    Range<Double> mzRange, ValueAxis domainAxis, ValueAxis rangeAxis) {

	super(dataset, domainAxis, rangeAxis, null);
    }
    
    Range<Double> getDomainRange() {
	return Range.closed(getDomainAxis().getRange().getLowerBound(),
		getDomainAxis().getRange().getUpperBound());
    }

    Range<Double> getAxisRange() {
	return Range.closed(getRangeAxis().getRange().getLowerBound(),
		getRangeAxis().getRange().getUpperBound());
    }

}