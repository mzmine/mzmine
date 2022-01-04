/*
 *  Copyright 2006-2020 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.modules.dataprocessing.id_ccscalibration;

import org.w3c.dom.Element;

/**
 *
 */
public class TwCCSCalibration implements CCSCalibration {

  private final double coeff;
  private final double exponent;
  private final double t0;

  public TwCCSCalibration(double coeff, double exponent, double t0) {
    this.coeff = coeff;
    this.exponent = exponent;
    this.t0 = t0;
  }

  /**
   * Todo this is not correct yet, the ccs needs to be corrected for the ion charge. Maybe the drift time needs to be adjusted as well?
   *
   * @param mz
   * @param charge
   * @param mobility
   * @return
   */
  @Override
  public float getCCS(double mz, int charge, float mobility) {
    return (float) (coeff * Math.pow(mobility + t0, exponent));
  }

  @Override
  public void saveToXML(Element element) {

  }

  public static CCSCalibration loadFromXML(Element xmlElement) {
    return null;
  }
}
