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

package net.sf.mzmine.modules.peakpicking.recursivethreshold;

/**
 * This class represent a 1D peak
 */
class OneDimPeak {

    public int scanNum;

    public float mz, intensity;
    public int datapointIndex;

    private boolean connected;

    public OneDimPeak(int scanNum, int datapointIndex, float mz,
            float intensity) {
        this.scanNum = scanNum;
        this.datapointIndex = datapointIndex;
        this.mz = mz;
        this.intensity = intensity;
        connected = false;
    }

    public void setConnected() {
        connected = true;
    }

    public boolean isConnected() {
        return connected;
    }

}