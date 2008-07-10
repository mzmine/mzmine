/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.data;

/**
 * This interface represents a single m/z peak within a spectrum. The getMZ()
 * and getIntensity() methods of MzPeak return the best m/z and intensity
 * values, which do not necessarily match any raw data points within the scan.
 * Instead, MzPeak provides the getRawDataPoints() method which returns those
 * data points that were considered to form this MzPeak.
 */
public interface MzPeak extends DataPoint {

    /**
     * This method returns an array of raw data points that form this peak,
     * sorted in m/z order.
     */
    public DataPoint[] getRawDataPoints();

}
