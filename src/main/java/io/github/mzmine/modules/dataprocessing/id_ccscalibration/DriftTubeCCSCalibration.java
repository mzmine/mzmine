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

import io.github.mzmine.modules.dataprocessing.id_ccscalibration.reference.CCSCalibrant;
import java.util.Objects;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.w3c.dom.Element;

public class DriftTubeCCSCalibration implements CCSCalibration {

  protected static final String XML_TYPE_NAME = "dtims_linear";

  private final double beta;
  private final double tfix;
  private final double rsquare;
  private final int n;

  public DriftTubeCCSCalibration(final double beta, final double tfix, final double rsquare,
      final int n) {
    this.beta = beta;
    this.tfix = tfix;
    this.rsquare = rsquare;
    this.n = n;
  }

  /**
   * @param simpleRegression Regression of drift time (f(x)) vs gamma * ccs (x)
   */
  public DriftTubeCCSCalibration(SimpleRegression simpleRegression) {
    this.tfix = simpleRegression.getIntercept();
    this.beta = simpleRegression.getSlope();
    this.rsquare = simpleRegression.getRSquare();
    this.n = (int) simpleRegression.getN();
  }

  private double getN2Gamma(double mz, int charge) {
    return 1 / (double) charge * Math.sqrt(mz * charge / (mz * charge + CCSCalibrant.N2_MASS));
  }

  @Override
  public float getCCS(double mz, int charge, float mobility) {
    // t_D = ß * gamma * ccs + t_fix
    // y   = m         x     + b        // general linear form
    // t_D = y; beta = m; gamma*css = x; t_fix = b

    // x  = (y-b) / m
    // gamma * ccs =  (t_D - t_fix) / beta
    // ccs = (t_D - t_fix) / (beta * gamma)
    return (float) ((mobility - tfix) / (beta * getN2Gamma(mz, charge)));
  }

  @Override
  public String toString() {
    return String.format("%s\tbeta: %.4f\ttfix: %.4f\trsquare: %.4f\tn: %d",
        this.getClass().getSimpleName(), beta, tfix, rsquare, n);
  }

  @Override
  public void saveToXML(Element element) {
    element.setAttribute(XML_TYPE_ATTR, XML_TYPE_NAME);
    element.setAttribute("tfix", String.valueOf(tfix));
    element.setAttribute("beta", String.valueOf(beta));
    element.setAttribute("rsquare", String.valueOf(rsquare));
    element.setAttribute("n", String.valueOf(n));
  }

  public static CCSCalibration loadFromXML(Element element) {
    double tfix = Double.parseDouble(element.getAttribute("tfix"));
    double beta = Double.parseDouble(element.getAttribute("beta"));
    double rsquare = Double.parseDouble(element.getAttribute("rsquare"));
    int n = Integer.parseInt(element.getAttribute("n"));

    return new DriftTubeCCSCalibration(beta, tfix, rsquare, n);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DriftTubeCCSCalibration that)) {
      return false;
    }
    return Double.compare(that.beta, beta) == 0 && Double.compare(that.tfix, tfix) == 0
        && Double.compare(that.rsquare, rsquare) == 0 && n == that.n;
  }

  @Override
  public int hashCode() {
    return Objects.hash(beta, tfix, rsquare, n);
  }
}
