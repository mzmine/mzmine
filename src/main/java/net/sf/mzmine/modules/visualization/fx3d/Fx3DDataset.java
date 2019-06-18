/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package net.sf.mzmine.modules.visualization.fx3d;

import com.google.common.collect.Range;

import javafx.beans.property.SimpleStringProperty;

public class Fx3DDataset {

    private float[][] intensityValues;
    private int rtResolution;
    private int mzResolution;
    private double maxBinnedIntensity;
    private Range<Double> rtRange, mzRange;
    private SimpleStringProperty fileName = new SimpleStringProperty("");
    private SimpleStringProperty color = new SimpleStringProperty("");

    public Fx3DDataset(float[][] intensityValues, int rtResolution,
            int mzResolution, double maxBinnedIntensity, Range<Double> rtRange,
            Range<Double> mzRange, String fileName) {
        this.intensityValues = intensityValues;
        this.rtResolution = rtResolution;
        this.mzResolution = mzResolution;
        this.maxBinnedIntensity = maxBinnedIntensity;
        this.rtRange = rtRange;
        this.mzRange = mzRange;
        this.fileName.set(fileName);
    }

    public float[][] getIntensityValues() {
        return intensityValues;
    }

    public int getRtResolution() {
        return rtResolution;
    }

    public int getMzResolution() {
        return mzResolution;
    }

    public double getMaxBinnedIntensity() {
        return maxBinnedIntensity;
    }

    public Range<Double> getRtRange() {
        return rtRange;
    }

    public Range<Double> getMzRange() {
        return mzRange;
    }

    public String getFileName() {
        return fileName.get();
    }

    public void setColor(String color) {
        this.color.set(color);
    }

    public String getColor() {
        return color.get();
    }
}
