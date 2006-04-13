/*
 * Copyright 2005 VTT Biotechnology
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

package net.sf.mzmine.visualizers;

import net.sf.mzmine.io.RawDataFile;

public interface RawDataVisualizer {

    public RawDataFile getRawDataFile();
    
    public void setRawDataFile(RawDataFile newFile);

    public void setMZRange(double mzMin, double mzMax);

    public void setRTRange(double rtMin, double rtMax);

    public void setMZPosition(double mz);

    public void setRTPosition(double rt);

    public void attachVisualizer(RawDataVisualizer visualizer);

    public void detachVisualizer(RawDataVisualizer visualizer);

    public void printMe();

    public void copyMe();

}
