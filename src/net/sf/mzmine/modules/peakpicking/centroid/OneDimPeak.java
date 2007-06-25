/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.peakpicking.centroid;


/**
 * This class represent a 1D peak
 */
class OneDimPeak {

    public int scanNum;

    public float mz;
    public float intensity;
    public int datapointIndex;

    private boolean connected;

    public OneDimPeak(int _scanNum, int _datapointIndex, float _mz,
            float _intensity) {
        scanNum = _scanNum;
        datapointIndex = _datapointIndex;
        mz = _mz;
        intensity = _intensity;

        connected = false;
    }

    public void setConnected() {
        connected = true;
    }

    public boolean isConnected() {
        return connected;
    }

}