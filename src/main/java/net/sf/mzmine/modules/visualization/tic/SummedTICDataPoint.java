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

package net.sf.mzmine.modules.visualization.tic;

public class SummedTICDataPoint {
  private double rt;
  private double intensity;
  private double mzBasePeak;
  private double intensityBasePeak;

  public SummedTICDataPoint(double rt, double intensity, double mzBasePeak,
      double intensityBasePeak) {
    super();
    this.rt = rt;
    this.intensity = intensity;
    this.mzBasePeak = mzBasePeak;
    this.intensityBasePeak = intensityBasePeak;
  }

  public double getIntensityBasePeak() {
    return intensityBasePeak;
  }

  public void setIntensityBasePeak(double intensityBasePeak) {
    this.intensityBasePeak = intensityBasePeak;
  }

  public double getRetentionTime() {
    return rt;
  }

  public void setRetentionTime(double rt) {
    this.rt = rt;
  }

  public double getIntensity() {
    return intensity;
  }

  public void setIntensity(double intensity) {
    this.intensity = intensity;
  }

  public double getMzBasePeak() {
    return mzBasePeak;
  }

  public void setMzBasePeak(double mzBasePeak) {
    this.mzBasePeak = mzBasePeak;
  }



}
