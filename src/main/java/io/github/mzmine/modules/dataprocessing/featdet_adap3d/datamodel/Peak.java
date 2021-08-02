/*
 * (C) Copyright 2015-2017 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */
package io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel;

import java.io.Serializable;

import java.util.NavigableMap;
import java.util.Map;
import java.util.TreeMap;

import io.github.mzmine.modules.dataprocessing.align_adap3.algorithms.Math;

/**
 * Class Peak contains all information about a peak as well as its chromatogram, and some methods to
 * get the information
 *
 * @author aleksandrsmirnov Modified by Dharak Shah to include in MSDK
 */
public class Peak implements Cloneable, Serializable {
  /**
  * 
  */
  private static final long serialVersionUID = 1L;
  private NavigableMap<Double, Double> chromatogram; // (retTime, intensity) - pairs
  private io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel.PeakInfo info;

  private double retTimeMin;
  private double retTimeMax;
  private double mzMin;
  private double mzMax;

  private double apexIntensity;
  private double apexRetTime;
  private double apexMZ;

  private double norm;
  private double shift;

  // ------------------------------------------------------------------------
  // ----- Contsructors -----------------------------------------------------
  // ------------------------------------------------------------------------

  /**
   * <p>
   * Constructor for Peak.
   * </p>
   *
   * @param peak a {@link Peak} object.
   */
  public Peak(final Peak peak) {
    info = new io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel.PeakInfo(peak.info);
    shift = peak.shift;
    chromatogram = new TreeMap<>(peak.chromatogram);

    retTimeMin = peak.retTimeMin;
    retTimeMax = peak.retTimeMax;

    mzMin = peak.mzMin;
    mzMax = peak.mzMax;

    apexIntensity = peak.apexIntensity;
    apexRetTime = peak.apexRetTime;
    apexMZ = peak.apexMZ;

    norm = peak.norm;
  }

  /**
   * <p>
   * Constructor for Peak.
   * </p>
   *
   * @param chromatogram a {@link NavigableMap} object.
   * @param info a {@link io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel.PeakInfo} object.
   */
  public Peak(final NavigableMap<Double, Double> chromatogram, final io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel.PeakInfo info) {
    this(chromatogram, info.mzValue);

    // this.info = new PeakInfo(info);
    this.info = info;
  }

  /**
   * <p>
   * Constructor for Peak.
   * </p>
   *
   * @param chromatogram a {@link NavigableMap} object.
   * @param mz a double.
   */
  public Peak(final NavigableMap<Double, Double> chromatogram, final double mz) {
    this.info = new io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel.PeakInfo();
    info.mzValue = mz;
    this.shift = 0.0;
    this.chromatogram = new TreeMap<>(chromatogram);

    retTimeMin = Double.MAX_VALUE;
    retTimeMax = 0.0;

    for (Map.Entry<Double, Double> entry : chromatogram.entrySet()) {
      double retTime = entry.getKey();
      double intensity = entry.getValue();

      if (retTime > retTimeMax)
        retTimeMax = retTime;
      if (retTime < retTimeMin)
        retTimeMin = retTime;

      if (intensity > apexIntensity) {
        apexIntensity = intensity;
        apexRetTime = retTime;
      }
    }

    apexMZ = mzMin = mzMax = mz;

    norm = java.lang.Math.sqrt(Math.continuous_dot_product(chromatogram, chromatogram));
  }

  // ------------------------------------------------------------------------
  // ----- Methods ----------------------------------------------------------
  // ------------------------------------------------------------------------

  /**
   * <p>
   * Setter for the field <code>shift</code>.
   * </p>
   *
   * @param shift a double.
   */
  public void setShift(double shift) {
    this.shift = shift;
  };

  /** {@inheritDoc} */
  @Override
  public Peak clone() {
    return new Peak(chromatogram, info);
  }

  // ------------------------------------------------------------------------
  // ----- Properties -------------------------------------------------------
  // ------------------------------------------------------------------------

  /**
   * <p>
   * Getter for the field <code>chromatogram</code>.
   * </p>
   *
   * @return a {@link NavigableMap} object.
   */
  public NavigableMap<Double, Double> getChromatogram() {
    return chromatogram;
  };

  /**
   * <p>
   * Getter for the field <code>info</code>.
   * </p>
   *
   * @return a {@link io.github.mzmine.modules.dataprocessing.featdet_adap3d.datamodel.PeakInfo} object.
   */
  public PeakInfo getInfo() {
    return info;
  };

  /**
   * <p>
   * getRetTime.
   * </p>
   *
   * @return a double.
   */
  public double getRetTime() {
    return apexRetTime;
  };

  /**
   * <p>
   * getMZ.
   * </p>
   *
   * @return a double.
   */
  public double getMZ() {
    return apexMZ;
  };

  /**
   * <p>
   * getIntensity.
   * </p>
   *
   * @return a double.
   */
  public double getIntensity() {
    return apexIntensity;
  };

  /**
   * <p>
   * Getter for the field <code>norm</code>.
   * </p>
   *
   * @return a double.
   */
  public double getNorm() {
    return norm;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "#" + this.info.peakID + ": mz=" + this.apexMZ + " rt=" + this.apexRetTime;
  }
}
