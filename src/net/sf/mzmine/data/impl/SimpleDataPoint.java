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

package net.sf.mzmine.data.impl;

import java.text.Format;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.main.MZmineCore;

/**
 * This class represents one datapoint of a spectra (m/z and intensity)
 */
public class SimpleDataPoint implements DataPoint {

    private float mz, intensity;

    /**
     * @param mz
     * @param intensity
     */
    public SimpleDataPoint(float mz, float intensity) {
        this.mz = mz;
        this.intensity = intensity;
    }

    public float getIntensity() {
        return intensity;
    }

    public float getMZ() {
        return mz;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public void setMZ(float mz) {
        this.mz = mz;
    }

    public String toString() {
        Format mzFormat = MZmineCore.getDesktop().getMZFormat();
        Format intensityFormat = MZmineCore.getDesktop().getIntensityFormat();
        String str = "m/z: " + mzFormat.format(mz) + ", intensity: "
                + intensityFormat.format(intensity);
        return str;
    }

}
